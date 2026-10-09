package com.pgds.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "fair_price_shops")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FairPriceShop {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true) private String shopCode;   // e.g. FPS101
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String district;
    private String address;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;
}
