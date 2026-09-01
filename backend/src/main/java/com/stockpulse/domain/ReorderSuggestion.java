package com.stockpulse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "reorder_suggestions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "current_stock", nullable = false)
    private int currentStock;

    @Column(name = "recommended_quantity", nullable = false)
    private int recommendedQuantity;

    /** Sprint 2 extension: supplier catalog will name preferred supplier + real lead time. */
    @Column(name = "suggested_lead_time_days", nullable = false)
    private int suggestedLeadTimeDays;

    /** 0.0 - 1.0 */
    @Column(name = "confidence", nullable = false, precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "reasoning", nullable = false, length = 2000)
    private String reasoning;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SuggestionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_reason", nullable = false)
    private TriggerReason triggerReason;

    @Column(name = "strategy", length = 50)
    private String strategy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
