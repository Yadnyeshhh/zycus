package com.stockpulse.engine;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.TriggerReason;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RuleBasedReorderStrategy implements ReorderStrategy {

    @Override
    public StrategyType getType() {
        return StrategyType.RULE_BASED;
    }

    @Override
    public ReorderRecommendation recommendReorder(Product product, TriggerReason triggerReason, double categoryAverageVelocity) {
        int threshold = product.getReorderThreshold();
        int stock = product.getStockLevel();
        int targetSafetyStock = threshold * 3;
        int qty = Math.max(1, targetSafetyStock - stock);

        return ReorderRecommendation.builder()
                .recommendedQuantity(qty)
                .suggestedLeadTimeDays(7)
                .confidence(new BigDecimal("0.850"))
                .reasoning(String.format("Rule-based: Replenishment target (3x threshold of %d = %d) minus current stock (%d) yields recommended reorder of %d units.",
                        threshold, targetSafetyStock, stock, qty))
                .strategyName("RULE_BASED")
                .build();
    }
}
