package com.pgds.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true) private String username;
    @Column(nullable = false) private String passwordHash;   // BCrypt
    @Column(nullable = false) private String fullName;
    private String email;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role role;
    @Builder.Default private boolean enabled = true;
    /** Set for WAREHOUSE_MANAGER */
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "warehouse_id") private Warehouse warehouse;
    /** Set for FPS_DEALER */
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "fps_id") private FairPriceShop fps;
}
