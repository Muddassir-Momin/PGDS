package com.pgds.repo;

import com.pgds.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {

    /** Current balance of a grain at a warehouse, derived from the ledger. */
    @Query("""
        select coalesce(sum(case when t.txnType in (com.pgds.domain.StockTxnType.OPENING,
                                                    com.pgds.domain.StockTxnType.RECEIPT,
                                                    com.pgds.domain.StockTxnType.TRANSFER_IN)
                                 then t.quantityKg else -t.quantityKg end), 0)
        from StockTransaction t
        where t.warehouse.id = :warehouseId and t.grainType = :grain""")
    BigDecimal warehouseBalance(@Param("warehouseId") Long warehouseId, @Param("grain") GrainType grain);

    /** Current balance of a grain at a Fair Price Shop, derived from the ledger. */
    @Query("""
        select coalesce(sum(case when t.txnType in (com.pgds.domain.StockTxnType.OPENING,
                                                    com.pgds.domain.StockTxnType.RECEIPT,
                                                    com.pgds.domain.StockTxnType.TRANSFER_IN)
                                 then t.quantityKg else -t.quantityKg end), 0)
        from StockTransaction t
        where t.fps.id = :fpsId and t.grainType = :grain""")
    BigDecimal fpsBalance(@Param("fpsId") Long fpsId, @Param("grain") GrainType grain);

    Page<StockTransaction> findByWarehouseIdOrderByCreatedAtDesc(Long warehouseId, Pageable pageable);
    Page<StockTransaction> findByFpsIdOrderByCreatedAtDesc(Long fpsId, Pageable pageable);

    /** Per-shop, per-grain balances (dashboards). */
    @Query("""
        select new com.pgds.repo.LocationGrainBalance(t.fps.id, t.grainType,
            sum(case when t.txnType in (com.pgds.domain.StockTxnType.OPENING,
                                        com.pgds.domain.StockTxnType.RECEIPT,
                                        com.pgds.domain.StockTxnType.TRANSFER_IN)
                     then t.quantityKg else -t.quantityKg end))
        from StockTransaction t where t.fps is not null group by t.fps.id, t.grainType""")
    java.util.List<LocationGrainBalance> fpsBalances();

    /** Per-warehouse, per-grain balances (dashboards). */
    @Query("""
        select new com.pgds.repo.LocationGrainBalance(t.warehouse.id, t.grainType,
            sum(case when t.txnType in (com.pgds.domain.StockTxnType.OPENING,
                                        com.pgds.domain.StockTxnType.RECEIPT,
                                        com.pgds.domain.StockTxnType.TRANSFER_IN)
                     then t.quantityKg else -t.quantityKg end))
        from StockTransaction t where t.warehouse is not null group by t.warehouse.id, t.grainType""")
    java.util.List<LocationGrainBalance> warehouseBalances();

    /** Stock leaving a shop in the window (distributions, damage, transfers out). */
    @Query("""
        select new com.pgds.repo.LocationGrainBalance(t.fps.id, t.grainType, sum(t.quantityKg))
        from StockTransaction t
        where t.fps is not null and t.createdAt >= :since
          and t.txnType in (com.pgds.domain.StockTxnType.DISTRIBUTION, com.pgds.domain.StockTxnType.DAMAGE,
                            com.pgds.domain.StockTxnType.TRANSFER_OUT)
        group by t.fps.id, t.grainType""")
    java.util.List<LocationGrainBalance> fpsOutflowSince(@Param("since") java.time.Instant since);

    /** Stock leaving a warehouse in the window (transfers out, damage). */
    @Query("""
        select new com.pgds.repo.LocationGrainBalance(t.warehouse.id, t.grainType, sum(t.quantityKg))
        from StockTransaction t
        where t.warehouse is not null and t.createdAt >= :since
          and t.txnType in (com.pgds.domain.StockTxnType.DAMAGE, com.pgds.domain.StockTxnType.TRANSFER_OUT)
        group by t.warehouse.id, t.grainType""")
    java.util.List<LocationGrainBalance> warehouseOutflowSince(@Param("since") java.time.Instant since);
}
