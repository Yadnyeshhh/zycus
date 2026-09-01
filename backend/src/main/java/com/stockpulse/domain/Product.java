package com.stockpulse.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @jakarta.persistence.Column(name = "sku", nullable = false, unique = true)
    private String sku;

    @jakarta.persistence.Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @jakarta.persistence.Column(name = "category", nullable = false)
    private Category category;

    @jakarta.persistence.Column(name = "current_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal currentPrice;

    /** Sprint 2 extension placeholder: margin rules need cost. Nullable by design. */
    @jakarta.persistence.Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice;

    @jakarta.persistence.Column(name = "stock_level", nullable = false)
    private int stockLevel;

    @jakarta.persistence.Column(name = "reorder_threshold", nullable = false)
    private int reorderThreshold;

    /** Orders in the last 24h. */
    @jakarta.persistence.Column(name = "demand_velocity", nullable = false)
    private int demandVelocity;

    @Enumerated(EnumType.STRING)
    @jakarta.persistence.Column(name = "status", nullable = false)
    private ProductStatus status;

    @Version
    private Long version;

    @jakarta.persistence.Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @jakarta.persistence.Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isStockBelowReorderThreshold() {
        return stockLevel < reorderThreshold;
    }
}
