package com.stockpulse;

import com.stockpulse.ai.AiCommerceAdvisor;
import com.stockpulse.ai.LLMGateway;
import com.stockpulse.domain.Category;
import com.stockpulse.domain.PriceDirection;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ProductStatus;
import com.stockpulse.domain.TriggerReason;
import com.stockpulse.engine.PricingRecommendation;
import com.stockpulse.engine.ReorderRecommendation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class AiCommerceAdvisorTest {

    @Autowired
    private AiCommerceAdvisor advisor;

    @MockBean
    private LLMGateway llmGateway;

    @Test
    void testAiPricingLowInventory() {
        when(llmGateway.callLLM(anyString())).thenReturn("""
                {
                  "recommendedPrice": 32.99,
                  "direction": "INCREASE",
                  "confidence": 0.89,
                  "reasoning": "Raise price by $3.00 to preserve scarce inventory during replenishment lead time."
                }
                """);

        Product product = Product.builder()
                .sku("TEST-AI-1")
                .name("AI Test Item")
                .category(Category.APPAREL)
                .currentPrice(new BigDecimal("29.99"))
                .stockLevel(4)
                .reorderThreshold(15)
                .demandVelocity(10)
                .status(ProductStatus.ACTIVE)
                .build();

        PricingRecommendation rec = advisor.recommendPricing(product, TriggerReason.INVENTORY_LOW, 3.0);

        assertThat(rec.getDirection()).isEqualTo(PriceDirection.INCREASE);
        assertThat(rec.getRecommendedPrice()).isEqualByComparingTo("32.99");
        assertThat(rec.getConfidence()).isEqualByComparingTo("0.890");
        assertThat(rec.getReasoning()).contains("Raise price by $3.00");
        assertThat(rec.getStrategyName()).isEqualTo("AI");
    }

    @Test
    void testAiReorderRecommendation() {
        when(llmGateway.callLLM(anyString())).thenReturn("""
                {
                  "recommendedQuantity": 45,
                  "suggestedLeadTimeDays": 5,
                  "confidence": 0.85,
                  "reasoning": "Batch size 45 units covers 30-day forecasted demand buffer."
                }
                """);

        Product product = Product.builder()
                .sku("TEST-AI-2")
                .name("AI Reorder Item")
                .category(Category.ELECTRONICS)
                .currentPrice(new BigDecimal("99.99"))
                .stockLevel(5)
                .reorderThreshold(20)
                .demandVelocity(8)
                .status(ProductStatus.ACTIVE)
                .build();

        ReorderRecommendation rec = advisor.recommendReorder(product, TriggerReason.INVENTORY_LOW, 2.5);

        assertThat(rec.getRecommendedQuantity()).isEqualTo(45);
        assertThat(rec.getSuggestedLeadTimeDays()).isEqualTo(5);
        assertThat(rec.getConfidence()).isEqualByComparingTo("0.850");
        assertThat(rec.getStrategyName()).isEqualTo("AI");
    }

    @Test
    void testMalformedJsonFallsBackToRules() {
        when(llmGateway.callLLM(anyString())).thenReturn("Not valid JSON at all");

        Product product = Product.builder()
                .sku("TEST-AI-3")
                .name("Fallback Item")
                .category(Category.ELECTRONICS)
                .currentPrice(new BigDecimal("100.00"))
                .stockLevel(5)
                .reorderThreshold(20)
                .demandVelocity(2)
                .status(ProductStatus.ACTIVE)
                .build();

        PricingRecommendation rec = advisor.recommendPricing(product, TriggerReason.INVENTORY_LOW, 2.0);

        assertThat(rec.getDirection()).isEqualTo(PriceDirection.INCREASE);
        assertThat(rec.getRecommendedPrice()).isEqualByComparingTo("110.00"); // 10% rule increase
        assertThat(rec.getReasoning()).contains("AI Fallback to Rules");
        assertThat(rec.getStrategyName()).isEqualTo("AI_FALLBACK_RULE");
    }

    @Test
    void testAbsurdPriceFallsBackToRules() {
        when(llmGateway.callLLM(anyString())).thenReturn("""
                {
                  "recommendedPrice": 999999.00,
                  "direction": "INCREASE",
                  "confidence": 0.99,
                  "reasoning": "Extreme price gouging"
                }
                """);

        Product product = Product.builder()
                .sku("TEST-AI-4")
                .name("Absurd Price Item")
                .category(Category.ELECTRONICS)
                .currentPrice(new BigDecimal("50.00"))
                .stockLevel(5)
                .reorderThreshold(20)
                .demandVelocity(2)
                .status(ProductStatus.ACTIVE)
                .build();

        PricingRecommendation rec = advisor.recommendPricing(product, TriggerReason.INVENTORY_LOW, 2.0);

        assertThat(rec.getRecommendedPrice()).isEqualByComparingTo("55.00"); // 10% rule increase
    }
}
