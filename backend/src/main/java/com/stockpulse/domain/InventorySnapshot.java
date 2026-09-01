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

import java.time.Instant;

/** Point-in-time inventory/demand state for a product. */
@Entity
@Table(name = "inventory_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventorySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "stock_level", nullable = false)
    private int stockLevel;

    @Column(name = "demand_velocity", nullable = false)
    private int demandVelocity;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_reason", nullable = false)
    private TriggerReason triggerReason;

    @Column(name = "captured_at", nullable = false, updatable = false)
    private Instant capturedAt;

    @PrePersist
    void onCreate() {
        capturedAt = Instant.now();
    }
}
