package com.stockpulse.dto;

import com.stockpulse.domain.Category;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private String sku;
    private String name;
    private Category category;
    private BigDecimal currentPrice;
    private BigDecimal costPrice;
    private int stockLevel;
    private int reorderThreshold;
    private int demandVelocity;
    private ProductStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static ProductResponse fromEntity(Product p) {
        if (p == null) return null;
        return ProductResponse.builder()
                .id(p.getId())
                .sku(p.getSku())
                .name(p.getName())
                .category(p.getCategory())
                .currentPrice(p.getCurrentPrice())
                .costPrice(p.getCostPrice())
                .stockLevel(p.getStockLevel())
                .reorderThreshold(p.getReorderThreshold())
                .demandVelocity(p.getDemandVelocity())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
