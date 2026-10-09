package com.pgds.stock;

import com.pgds.common.PageResponse;
import com.pgds.stock.StockDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class StockController {

    private static final String WH_READ = "hasAnyRole('SUPER_ADMIN','GOVT_OFFICER','AUDITOR','WAREHOUSE_MANAGER')";
    private static final String WH_WRITE = "hasAnyRole('SUPER_ADMIN','WAREHOUSE_MANAGER')";
    private static final String FPS_READ = "hasAnyRole('SUPER_ADMIN','GOVT_OFFICER','AUDITOR','FPS_DEALER')";
    private static final String FPS_WRITE = "hasAnyRole('SUPER_ADMIN','FPS_DEALER')";

    private final StockService stock;

    // ----- warehouse -----
    @GetMapping("/api/warehouse/{id}/stock") @PreAuthorize(WH_READ)
    public StockSummary warehouseStock(Authentication a, @PathVariable Long id) { return stock.warehouseStock(a.getName(), id); }

    @GetMapping("/api/warehouse/{id}/transactions") @PreAuthorize(WH_READ)
    public PageResponse<TxnView> warehouseTxns(Authentication a, @PathVariable Long id,
                                               @PageableDefault(size = 20) Pageable p) {
        return stock.warehouseTransactions(a.getName(), id, p);
    }

    @PostMapping("/api/warehouse/{id}/receipts") @PreAuthorize(WH_WRITE) @ResponseStatus(HttpStatus.CREATED)
    public TxnView receive(Authentication a, @PathVariable Long id, @Valid @RequestBody StockEntry e) {
        return stock.receive(a.getName(), id, e);
    }

    @PostMapping("/api/warehouse/{id}/damage") @PreAuthorize(WH_WRITE) @ResponseStatus(HttpStatus.CREATED)
    public TxnView warehouseDamage(Authentication a, @PathVariable Long id, @Valid @RequestBody StockEntry e) {
        return stock.damageAtWarehouse(a.getName(), id, e);
    }

    @PostMapping("/api/warehouse/{id}/transfers") @PreAuthorize(WH_WRITE) @ResponseStatus(HttpStatus.CREATED)
    public TxnView transfer(Authentication a, @PathVariable Long id, @Valid @RequestBody TransferRequest r) {
        return stock.transfer(a.getName(), id, r);
    }

    @GetMapping("/api/warehouse/{id}/shops") @PreAuthorize(WH_WRITE)
    public java.util.List<ShopRef> shopsServed(Authentication a, @PathVariable Long id) {
        return stock.shopsServed(a.getName(), id);
    }

    // ----- fair price shop -----
    @GetMapping("/api/fps/{id}/stock") @PreAuthorize(FPS_READ)
    public StockSummary fpsStock(Authentication a, @PathVariable Long id) { return stock.fpsStock(a.getName(), id); }

    @GetMapping("/api/fps/{id}/transactions") @PreAuthorize(FPS_READ)
    public PageResponse<TxnView> fpsTxns(Authentication a, @PathVariable Long id, @PageableDefault(size = 20) Pageable p) {
        return stock.fpsTransactions(a.getName(), id, p);
    }

    @PostMapping("/api/fps/{id}/damage") @PreAuthorize(FPS_WRITE) @ResponseStatus(HttpStatus.CREATED)
    public TxnView fpsDamage(Authentication a, @PathVariable Long id, @Valid @RequestBody StockEntry e) {
        return stock.damageAtFps(a.getName(), id, e);
    }
}
