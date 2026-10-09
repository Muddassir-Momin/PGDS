package com.pgds.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "distributions", indexes = {
        @Index(name = "idx_dist_card_month", columnList = "ration_card_id,period"),
        @Index(name = "idx_dist_receipt", columnList = "receiptNo"),
        @Index(name = "idx_dist_period", columnList = "period")})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Distribution {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** One receipt can cover several grains, so receiptNo is shared by its rows. */
    @Column(nullable = false, length = 40) private String receiptNo;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "ration_card_id") private RationCard rationCard;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "fps_id") private FairPriceShop fps;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private GrainType grainType;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal quantityKg;
    /** Format yyyy-MM, e.g. 2026-10 */
    @Column(nullable = false, length = 7) private String period;
    private String distributedBy;
    @Column(nullable = false, updatable = false) @Builder.Default private Instant distributedAt = Instant.now();
}
