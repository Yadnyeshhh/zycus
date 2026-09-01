package com.stockpulse.dto;

import com.stockpulse.domain.Category;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProductRequest {

    @NotBlank(message = "SKU is required")
    private String sku;

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Category is required")
    private Category category;

    @NotNull(message = "Current price is required")
    @DecimalMin(value = "0.01", message = "Price must be positive")
    private BigDecimal currentPrice;

    private BigDecimal costPrice;

    @Min(value = 0, message = "Stock level must be non-negative")
    private int stockLevel;

    @Min(value = 1, message = "Reorder threshold must be at least 1")
    private int reorderThreshold;

    @Min(value = 0, message = "Demand velocity must be non-negative")
    private int demandVelocity;
}
