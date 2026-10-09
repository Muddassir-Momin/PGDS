package com.pgds.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

/** Append-only ledger. Stock is never overwritten; balance = SUM(type.sign * quantityKg). */
@Entity @Table(name = "stock_transactions", indexes = {
        @Index(name = "idx_stock_loc", columnList = "locationType,warehouse_id,fps_id,grainType")})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private LocationType locationType;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "warehouse_id") private Warehouse warehouse;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "fps_id") private FairPriceShop fps;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private GrainType grainType;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private StockTxnType txnType;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal quantityKg;
    private String remarks;
    private String createdBy;
    @Column(nullable = false, updatable = false) @Builder.Default private Instant createdAt = Instant.now();
}
