package com.stockpulse.ai;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.TriggerReason;
import com.stockpulse.engine.PricingRecommendation;
import com.stockpulse.engine.PricingStrategy;
import com.stockpulse.engine.StrategyType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiPricingStrategy implements PricingStrategy {

    private final AiCommerceAdvisor advisor;

    @Override
    public StrategyType getType() {
        return StrategyType.AI;
    }

    @Override
    public PricingRecommendation recommendPricing(Product product, TriggerReason triggerReason, double categoryAverageVelocity) {
        return advisor.recommendPricing(product, triggerReason, categoryAverageVelocity);
    }
}
