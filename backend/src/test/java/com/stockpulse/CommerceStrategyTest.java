package com.stockpulse;

import com.stockpulse.domain.Category;
import com.stockpulse.domain.PriceDirection;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ProductStatus;
import com.stockpulse.domain.TriggerReason;
import com.stockpulse.engine.CommerceStrategyManager;
import com.stockpulse.engine.PricingRecommendation;
import com.stockpulse.engine.ReorderRecommendation;
import com.stockpulse.engine.RuleBasedPricingStrategy;
import com.stockpulse.engine.RuleBasedReorderStrategy;
import com.stockpulse.engine.StrategyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CommerceStrategyTest {

    @Autowired
    private RuleBasedPricingStrategy pricingStrategy;

    @Autowired
    private RuleBasedReorderStrategy reorderStrategy;

    @Autowired
    private CommerceStrategyManager strategyManager;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        strategyManager.setActiveStrategyType(StrategyType.RULE_BASED);
    }

    @Test
    void testLowStockTriggers10PercentPriceIncrease() {
        Product product = Product.builder()
                .sku("TEST-1")
                .name("Low Stock Item")
                .category(Category.ELECTRONICS)
                .currentPrice(new BigDecimal("100.00"))
                .stockLevel(5)
                .reorderThreshold(15) // stock (5) < threshold (15)
                .demandVelocity(2)
                .status(ProductStatus.ACTIVE)
                .build();

        PricingRecommendation rec = pricingStrategy.recommendPricing(product, TriggerReason.INVENTORY_LOW, 2.0);

        assertThat(rec.getDirection()).isEqualTo(PriceDirection.INCREASE);
        assertThat(rec.getRecommendedPrice()).isEqualByComparingTo("110.00");
        assertThat(rec.getConfidence()).isEqualByComparingTo("0.900");
        assertThat(rec.getReasoning()).contains("10% price increase");
    }

    @Test
    void testDemandSpikeTriggers5PercentPriceIncrease() {
        Product product = Product.builder()
                .sku("TEST-2")
                .name("Spike Item")
                .category(Category.ELECTRONICS)
                .currentPrice(new BigDecimal("100.00"))
                .stockLevel(30)
                .reorderThreshold(15) // stock ok
                .demandVelocity(10) // velocity (10) > 2 * avg (3.0)
                .status(ProductStatus.ACTIVE)
                .build();

        PricingRecommendation rec = pricingStrategy.recommendPricing(product, TriggerReason.DEMAND_SPIKE, 3.0);

        assertThat(rec.getDirection()).isEqualTo(PriceDirection.INCREASE);
        assertThat(rec.getRecommendedPrice()).isEqualByComparingTo("105.00");
        assertThat(rec.getConfidence()).isEqualByComparingTo("0.850");
        assertThat(rec.getReasoning()).contains("5% price increase");
    }

    @Test
    void testNormalConditionsResultInHold() {
        Product product = Product.builder()
                .sku("TEST-3")
                .name("Normal Item")
                .category(Category.ELECTRONICS)
                .currentPrice(new BigDecimal("50.00"))
                .stockLevel(50)
                .reorderThreshold(20)
                .demandVelocity(2)
                .status(ProductStatus.ACTIVE)
                .build();

        PricingRecommendation rec = pricingStrategy.recommendPricing(product, TriggerReason.MANUAL, 2.0);

        assertThat(rec.getDirection()).isEqualTo(PriceDirection.HOLD);
        assertThat(rec.getRecommendedPrice()).isEqualByComparingTo("50.00");
        assertThat(rec.getConfidence()).isEqualByComparingTo("1.000");
    }

    @Test
    void testReorderQuantityCalculation() {
        Product product = Product.builder()
                .sku("TEST-4")
                .name("Reorder Test Item")
                .category(Category.HOME)
                .currentPrice(new BigDecimal("20.00"))
                .stockLevel(8)
                .reorderThreshold(15) // Target = 15 * 3 = 45; 45 - 8 = 37
                .demandVelocity(2)
                .status(ProductStatus.ACTIVE)
                .build();

        ReorderRecommendation rec = reorderStrategy.recommendReorder(product, TriggerReason.INVENTORY_LOW, 2.0);

        assertThat(rec.getRecommendedQuantity()).isEqualTo(37);
        assertThat(rec.getConfidence()).isEqualByComparingTo("0.850");
    }

    @Test
    void testRuntimeStrategySwitchingViaEndpoint() throws Exception {
        mockMvc.perform(get("/commerce/strategy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeStrategy").value("RULE_BASED"));

        mockMvc.perform(post("/commerce/strategy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"strategy\":\"RULE_BASED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeStrategy").value("RULE_BASED"));
    }
}
