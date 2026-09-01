package com.stockpulse.engine;

import com.stockpulse.domain.PriceDirection;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.TriggerReason;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class RuleBasedPricingStrategy implements PricingStrategy {

    @Override
    public StrategyType getType() {
        return StrategyType.RULE_BASED;
    }

    @Override
    public PricingRecommendation recommendPricing(Product product, TriggerReason triggerReason, double categoryAverageVelocity) {
        BigDecimal currentPrice = product.getCurrentPrice();
        int stock = product.getStockLevel();
        int threshold = product.getReorderThreshold();
        int velocity = product.getDemandVelocity();

        if (stock < threshold) {
            BigDecimal recommendedPrice = currentPrice.multiply(new BigDecimal("1.10"))
                    .setScale(2, RoundingMode.HALF_UP);
            return PricingRecommendation.builder()
                    .recommendedPrice(recommendedPrice)
                    .direction(PriceDirection.INCREASE)
                    .confidence(new BigDecimal("0.900"))
                    .reasoning(String.format("Rule-based: Stock level (%d) is below reorder threshold (%d). Recommended 10%% price increase from $%s to $%s to protect remaining inventory.",
                            stock, threshold, currentPrice, recommendedPrice))
                    .strategyName("RULE_BASED")
                    .build();
        } else if (velocity > (2.0 * categoryAverageVelocity)) {
            BigDecimal recommendedPrice = currentPrice.multiply(new BigDecimal("1.05"))
                    .setScale(2, RoundingMode.HALF_UP);
            return PricingRecommendation.builder()
                    .recommendedPrice(recommendedPrice)
                    .direction(PriceDirection.INCREASE)
                    .confidence(new BigDecimal("0.850"))
                    .reasoning(String.format("Rule-based: Demand velocity (%d) exceeds 2x category average (%.1f). Recommended 5%% price increase from $%s to $%s to capture high demand.",
                            velocity, categoryAverageVelocity, currentPrice, recommendedPrice))
                    .strategyName("RULE_BASED")
                    .build();
        } else {
            return PricingRecommendation.builder()
                    .recommendedPrice(currentPrice)
                    .direction(PriceDirection.HOLD)
                    .confidence(new BigDecimal("1.000"))
                    .reasoning(String.format("Rule-based: Stock (%d) and velocity (%d) within normal parameters. HOLD current price at $%s.",
                            stock, velocity, currentPrice))
                    .strategyName("RULE_BASED")
                    .build();
        }
    }
}
