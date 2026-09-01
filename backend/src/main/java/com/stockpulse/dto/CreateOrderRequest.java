package com.stockpulse.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @JsonAlias({"qty", "count"})
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity = 1;
}
