package com.stockpulse.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStockRequest {

    @JsonAlias("stock")
    @Min(value = 0, message = "Stock level must be non-negative")
    private Integer stockLevel;
}
