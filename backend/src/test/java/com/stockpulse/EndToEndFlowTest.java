package com.stockpulse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpulse.domain.PriceDirection;
import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ReorderSuggestion;
import com.stockpulse.domain.SuggestionStatus;
import com.stockpulse.domain.TriggerReason;
import com.stockpulse.dto.CreateOrderRequest;
import com.stockpulse.dto.SuggestionDecisionRequest;
import com.stockpulse.engine.CommerceStrategyManager;
import com.stockpulse.engine.StrategyType;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EndToEndFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PricingSuggestionRepository pricingRepo;

    @Autowired
    private ReorderSuggestionRepository reorderRepo;

    @Autowired
    private CommerceStrategyManager strategyManager;

    @Test
    void testCompleteLowInventoryEndToEndWalkthrough() throws Exception {
        strategyManager.setActiveStrategyType(StrategyType.RULE_BASED);

        // PRD-003 is Organic Cotton T-Shirt, initial stock 8, threshold 15
        Product tShirt = productRepository.findBySku("SKU-APP-001").orElseThrow();
        BigDecimal originalPrice = tShirt.getCurrentPrice();

        // 1. Simulate sale via POST /products/{id}/orders
        mockMvc.perform(post("/products/" + tShirt.getId() + "/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockLevel").value(6));

        // Allow async agentic loop to process event
        Thread.sleep(400);

        // 2. Verify pending suggestions created with INVENTORY_LOW
        List<PricingSuggestion> pricingList = pricingRepo.findByProductIdOrderByCreatedAtDesc(tShirt.getId());
        assertThat(pricingList).isNotEmpty();
        PricingSuggestion pendingPrice = pricingList.stream()
                .filter(s -> s.getStatus() == SuggestionStatus.PENDING && s.getTriggerReason() == TriggerReason.INVENTORY_LOW)
                .findFirst().orElseThrow();

        assertThat(pendingPrice.getDirection()).isEqualTo(PriceDirection.INCREASE);
        assertThat(pendingPrice.getRecommendedPrice()).isGreaterThan(originalPrice);

        List<ReorderSuggestion> reorderList = reorderRepo.findByProductIdOrderByCreatedAtDesc(tShirt.getId());
        assertThat(reorderList).isNotEmpty();
        ReorderSuggestion pendingReorder = reorderList.stream()
                .filter(s -> s.getStatus() == SuggestionStatus.PENDING && s.getTriggerReason() == TriggerReason.INVENTORY_LOW)
                .findFirst().orElseThrow();

        assertThat(pendingReorder.getRecommendedQuantity()).isGreaterThanOrEqualTo(1);

        // 3. Merchandiser accepts pricing suggestion
        mockMvc.perform(patch("/pricing-suggestions/" + pendingPrice.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SuggestionDecisionRequest("ACCEPTED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        Product updatedPricePrd = productRepository.findById(tShirt.getId()).orElseThrow();
        assertThat(updatedPricePrd.getCurrentPrice()).isEqualByComparingTo(pendingPrice.getRecommendedPrice());

        // 4. Merchandiser accepts reorder suggestion
        int stockBeforeReorder = updatedPricePrd.getStockLevel();
        mockMvc.perform(patch("/reorder-suggestions/" + pendingReorder.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SuggestionDecisionRequest("ACCEPTED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        Product finalPrd = productRepository.findById(tShirt.getId()).orElseThrow();
        assertThat(finalPrd.getStockLevel()).isEqualTo(stockBeforeReorder + pendingReorder.getRecommendedQuantity());
    }

    @Test
    void testDemandSpikeAndRejectionWalkthrough() throws Exception {
        strategyManager.setActiveStrategyType(StrategyType.RULE_BASED);

        // PRD-008 Hoodie (SKU-APP-003), initial velocity 15, stock 11, threshold 12
        Product hoodie = productRepository.findBySku("SKU-APP-003").orElseThrow();
        BigDecimal initialPrice = hoodie.getCurrentPrice();

        // Simulate a viral surge order (+25 units) -> velocity increases significantly
        mockMvc.perform(post("/products/" + hoodie.getId() + "/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(25))))
                .andExpect(status().isOk());

        Thread.sleep(400);

        List<PricingSuggestion> pricingList = pricingRepo.findByProductIdOrderByCreatedAtDesc(hoodie.getId());
        assertThat(pricingList).isNotEmpty();

        PricingSuggestion pending = pricingList.stream()
                .filter(s -> s.getStatus() == SuggestionStatus.PENDING)
                .findFirst().orElseThrow();

        // Merchandiser REJECTS the suggestion
        mockMvc.perform(patch("/pricing-suggestions/" + pending.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SuggestionDecisionRequest("REJECTED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        // Live product price must NOT change!
        Product unchangedPrd = productRepository.findById(hoodie.getId()).orElseThrow();
        assertThat(unchangedPrd.getCurrentPrice()).isEqualByComparingTo(initialPrice);
    }
}
