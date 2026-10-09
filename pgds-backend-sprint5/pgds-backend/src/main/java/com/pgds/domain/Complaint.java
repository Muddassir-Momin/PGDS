package com.pgds.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "complaints")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Complaint {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private AppUser submittedBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "fps_id") private FairPriceShop fps;
    @Column(nullable = false, length = 2000) private String description;
    /** Filled by the AI classifier in Sprint 4 */
    private String category;
    @Enumerated(EnumType.STRING) private Priority priority;
    private String department;
    @Column(length = 1000) private String resolutionNote;
    @Enumerated(EnumType.STRING) @Builder.Default private ComplaintStatus status = ComplaintStatus.OPEN;
    @Column(nullable = false, updatable = false) @Builder.Default private Instant createdAt = Instant.now();
}
