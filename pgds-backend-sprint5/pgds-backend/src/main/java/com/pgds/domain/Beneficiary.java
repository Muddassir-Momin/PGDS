package com.pgds.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "beneficiaries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Beneficiary {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    private int age;
    /** Last 4 digits only - never store the full Aadhaar number. Used for duplicate checks together with name/DOB. */
    @Column(length = 4) private String aadhaarLast4;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "ration_card_id")
    private RationCard rationCard;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id")
    private AppUser user;
}
