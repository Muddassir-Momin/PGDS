package com.pgds.stock;

import com.pgds.common.ApiException;
import com.pgds.common.PageResponse;
import com.pgds.domain.*;
import com.pgds.repo.*;
import com.pgds.security.AccessGuard;
import com.pgds.stock.StockDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Ledger-based stock. Nothing is ever updated in place: every movement is a new StockTransaction row
 * and the balance is the signed sum. Outflows lock the source row so two requests cannot overdraw stock.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StockService {

    private final WarehouseRepository warehouses;
    private final FairPriceShopRepository shops;
    private final StockTransactionRepository txns;
    private final AccessGuard access;

    // ---------- warehouse ----------

    @Transactional(readOnly = true)
    public StockSummary warehouseStock(String username, Long warehouseId) {
        access.checkWarehouse(username, warehouseId);
        Warehouse w = warehouses.findById(warehouseId).orElseThrow(() -> notFound("Warehouse"));
        return new StockSummary(LocationType.WAREHOUSE, w.getId(), w.getName(),
                balances(g -> txns.warehouseBalance(w.getId(), g)));
    }

    @Transactional(readOnly = true)
    public PageResponse<TxnView> warehouseTransactions(String username, Long warehouseId, Pageable p) {
        access.checkWarehouse(username, warehouseId);
        return PageResponse.of(txns.findByWarehouseIdOrderByCreatedAtDesc(warehouseId, p).map(TxnView::of));
    }

    public TxnView receive(String username, Long warehouseId, StockEntry e) {
        access.checkWarehouse(username, warehouseId);
        Warehouse w = warehouses.findById(warehouseId).orElseThrow(() -> notFound("Warehouse"));
        return TxnView.of(txns.save(warehouseTxn(w, e.grainType(), StockTxnType.RECEIPT, e.quantityKg(), e.remarks(), username)));
    }

    public TxnView damageAtWarehouse(String username, Long warehouseId, StockEntry e) {
        access.checkWarehouse(username, warehouseId);
        Warehouse w = warehouses.findByIdForUpdate(warehouseId).orElseThrow(() -> notFound("Warehouse"));
        requireBalance(txns.warehouseBalance(w.getId(), e.grainType()), e.quantityKg());
        return TxnView.of(txns.save(warehouseTxn(w, e.grainType(), StockTxnType.DAMAGE, e.quantityKg(), e.remarks(), username)));
    }

    /** Warehouse -> FPS. Writes TRANSFER_OUT and TRANSFER_IN in one transaction. Returns the outgoing entry. */
    public TxnView transfer(String username, Long warehouseId, TransferRequest r) {
        access.checkWarehouse(username, warehouseId);
        Warehouse w = warehouses.findByIdForUpdate(warehouseId).orElseThrow(() -> notFound("Warehouse"));
        FairPriceShop fps = shops.findById(r.fpsId()).orElseThrow(() -> notFound("Fair Price Shop"));
        if (fps.getWarehouse() == null || !fps.getWarehouse().getId().equals(w.getId()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "This shop is not served by the selected warehouse");
        requireBalance(txns.warehouseBalance(w.getId(), r.grainType()), r.quantityKg());

        String note = r.remarks() == null ? "" : r.remarks();
        StockTransaction out = warehouseTxn(w, r.grainType(), StockTxnType.TRANSFER_OUT, r.quantityKg(),
                "To " + fps.getShopCode() + (note.isEmpty() ? "" : " - " + note), username);
        StockTransaction in = StockTransaction.builder().locationType(LocationType.FPS).fps(fps)
                .grainType(r.grainType()).txnType(StockTxnType.TRANSFER_IN).quantityKg(r.quantityKg())
                .remarks("From warehouse " + w.getId() + (note.isEmpty() ? "" : " - " + note)).createdBy(username).build();
        txns.saveAll(List.of(out, in));
        return TxnView.of(out);
    }

    /** Shops supplied by this warehouse (for the transfer form). */
    @Transactional(readOnly = true)
    public List<ShopRef> shopsServed(String username, Long warehouseId) {
        access.checkWarehouse(username, warehouseId);
        return shops.findByWarehouseId(warehouseId).stream()
                .map(s -> new ShopRef(s.getId(), s.getShopCode(), s.getName())).toList();
    }

    // ---------- fair price shop ----------

    @Transactional(readOnly = true)
    public StockSummary fpsStock(String username, Long fpsId) {
        access.checkFps(username, fpsId);
        FairPriceShop s = shops.findById(fpsId).orElseThrow(() -> notFound("Fair Price Shop"));
        return new StockSummary(LocationType.FPS, s.getId(), s.getName(), balances(g -> txns.fpsBalance(s.getId(), g)));
    }

    @Transactional(readOnly = true)
    public PageResponse<TxnView> fpsTransactions(String username, Long fpsId, Pageable p) {
        access.checkFps(username, fpsId);
        return PageResponse.of(txns.findByFpsIdOrderByCreatedAtDesc(fpsId, p).map(TxnView::of));
    }

    public TxnView damageAtFps(String username, Long fpsId, StockEntry e) {
        access.checkFps(username, fpsId);
        FairPriceShop s = shops.findByIdForUpdate(fpsId).orElseThrow(() -> notFound("Fair Price Shop"));
        requireBalance(txns.fpsBalance(s.getId(), e.grainType()), e.quantityKg());
        return TxnView.of(txns.save(StockTransaction.builder().locationType(LocationType.FPS).fps(s)
                .grainType(e.grainType()).txnType(StockTxnType.DAMAGE).quantityKg(e.quantityKg())
                .remarks(e.remarks()).createdBy(username).build()));
    }

    // ---------- helpers ----------

    private StockTransaction warehouseTxn(Warehouse w, GrainType g, StockTxnType type, BigDecimal qty, String remarks, String user) {
        return StockTransaction.builder().locationType(LocationType.WAREHOUSE).warehouse(w)
                .grainType(g).txnType(type).quantityKg(qty).remarks(remarks).createdBy(user).build();
    }

    private List<StockBalance> balances(java.util.function.Function<GrainType, BigDecimal> fn) {
        return Arrays.stream(GrainType.values()).map(g -> new StockBalance(g, fn.apply(g))).toList();
    }

    private void requireBalance(BigDecimal available, BigDecimal requested) {
        if (available == null || available.compareTo(requested) < 0)
            throw new ApiException(HttpStatus.CONFLICT,
                    "Insufficient stock: available " + (available == null ? "0" : available.toPlainString()) + " kg");
    }

    private ApiException notFound(String what) { return new ApiException(HttpStatus.NOT_FOUND, what + " not found"); }
}
