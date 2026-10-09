package com.pgds.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity @Table(name = "ai_alerts", indexes = @Index(name = "idx_alert_status", columnList = "status"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiAlert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AlertType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Priority severity;
    @Enumerated(EnumType.STRING) @Column(nullable = false) @Builder.Default private AlertStatus status = AlertStatus.OPEN;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "fps_id") private FairPriceShop fps;
    @Column(nullable = false, length = 200) private String title;
    @Column(length = 1000) private String details;
    /** Unique per finding, so re-running the analysis never duplicates an alert. */
    @Column(nullable = false, unique = true, length = 120) private String referenceKey;
    private Double score;
    @Column(nullable = false, updatable = false) @Builder.Default private Instant detectedAt = Instant.now();
    private String reviewedBy;
    @Column(length = 500) private String reviewNote;
}
