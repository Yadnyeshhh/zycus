package com.stockpulse.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderRecommendation {
    private int recommendedQuantity;
    private int suggestedLeadTimeDays;
    private BigDecimal confidence;
    private String reasoning;
    private String strategyName;
}
