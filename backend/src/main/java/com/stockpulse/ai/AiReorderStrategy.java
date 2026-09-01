package com.stockpulse.ai;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.TriggerReason;
import com.stockpulse.engine.ReorderRecommendation;
import com.stockpulse.engine.ReorderStrategy;
import com.stockpulse.engine.StrategyType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiReorderStrategy implements ReorderStrategy {

    private final AiCommerceAdvisor advisor;

    @Override
    public StrategyType getType() {
        return StrategyType.AI;
    }

    @Override
    public ReorderRecommendation recommendReorder(Product product, TriggerReason triggerReason, double categoryAverageVelocity) {
        return advisor.recommendReorder(product, triggerReason, categoryAverageVelocity);
    }
}
