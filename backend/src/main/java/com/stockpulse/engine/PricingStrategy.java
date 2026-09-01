package com.stockpulse.engine;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.TriggerReason;

public interface PricingStrategy {

    StrategyType getType();

    PricingRecommendation recommendPricing(Product product, TriggerReason triggerReason, double categoryAverageVelocity);
}
