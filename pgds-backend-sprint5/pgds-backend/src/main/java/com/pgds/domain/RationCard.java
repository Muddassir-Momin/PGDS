package com.pgds.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "ration_cards")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RationCard {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true) private String cardNumber;
    @Column(nullable = false) private String headOfFamily;
    private String address;
    @Column(nullable = false) private String district;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CardCategory category;
    @Column(nullable = false) private int familyMembers;
    @Builder.Default private boolean eligible = true;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "fps_id") private FairPriceShop fps;
}
