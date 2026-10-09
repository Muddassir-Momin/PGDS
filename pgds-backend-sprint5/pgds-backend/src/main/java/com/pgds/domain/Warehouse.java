package com.pgds.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "warehouses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Warehouse {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String district;
    private String address;
}
