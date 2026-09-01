package com.stockpulse.engine;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.TriggerReason;

public interface ReorderStrategy {

    StrategyType getType();

    ReorderRecommendation recommendReorder(Product product, TriggerReason triggerReason, double categoryAverageVelocity);
}
