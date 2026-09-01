package com.stockpulse.engine;

import com.stockpulse.domain.PriceDirection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingRecommendation {
    private BigDecimal recommendedPrice;
    private PriceDirection direction;
    private BigDecimal confidence;
    private String reasoning;
    private String strategyName;
}
