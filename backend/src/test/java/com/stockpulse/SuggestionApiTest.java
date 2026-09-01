package com.stockpulse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.SuggestionStatus;
import com.stockpulse.domain.TriggerReason;
import com.stockpulse.dto.PricingSuggestionResponse;
import com.stockpulse.dto.ReorderSuggestionResponse;
import com.stockpulse.dto.SuggestionDecisionRequest;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import com.stockpulse.service.SuggestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SuggestionApiTest {

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
    private SuggestionService suggestionService;

    @Test
    void testOnDemandPricingAndReorderSuggestionEndpoints() throws Exception {
        Product prd1 = productRepository.findBySku("SKU-ELEC-001").orElseThrow();

        mockMvc.perform(post("/products/" + prd1.getId() + "/suggest-pricing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is(prd1.getId().intValue())))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.recommendedPrice", notNullValue()))
                .andExpect(jsonPath("$.confidence", notNullValue()));

        mockMvc.perform(post("/products/" + prd1.getId() + "/suggest-reorder"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is(prd1.getId().intValue())))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.recommendedQuantity", notNullValue()));
    }

    @Test
    void testAcceptPricingSuggestionUpdatesProductPrice() throws Exception {
        Product prd = productRepository.findBySku("SKU-ELEC-002").orElseThrow();
        BigDecimal initialPrice = prd.getCurrentPrice();

        PricingSuggestionResponse suggestion = suggestionService.generatePricingSuggestion(prd.getId(), TriggerReason.MANUAL);

        SuggestionDecisionRequest acceptReq = new SuggestionDecisionRequest("ACCEPTED");

        mockMvc.perform(patch("/pricing-suggestions/" + suggestion.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCEPTED")));

        Product updatedPrd = productRepository.findById(prd.getId()).orElseThrow();
        assertThat(updatedPrd.getCurrentPrice()).isEqualByComparingTo(suggestion.getRecommendedPrice());
    }

    @Test
    void testRejectPricingSuggestionLeavesPriceUnchanged() throws Exception {
        Product prd = productRepository.findBySku("SKU-HOME-001").orElseThrow();
        BigDecimal initialPrice = prd.getCurrentPrice();

        PricingSuggestionResponse suggestion = suggestionService.generatePricingSuggestion(prd.getId(), TriggerReason.MANUAL);

        SuggestionDecisionRequest rejectReq = new SuggestionDecisionRequest("REJECTED");

        mockMvc.perform(patch("/pricing-suggestions/" + suggestion.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")));

        Product updatedPrd = productRepository.findById(prd.getId()).orElseThrow();
        assertThat(updatedPrd.getCurrentPrice()).isEqualByComparingTo(initialPrice);
    }

    @Test
    void testAcceptReorderSuggestionIncrementsStock() throws Exception {
        Product prd = productRepository.findBySku("SKU-APP-002").orElseThrow();
        int initialStock = prd.getStockLevel();

        ReorderSuggestionResponse suggestion = suggestionService.generateReorderSuggestion(prd.getId(), TriggerReason.INVENTORY_LOW);

        SuggestionDecisionRequest acceptReq = new SuggestionDecisionRequest("ACCEPTED");

        mockMvc.perform(patch("/reorder-suggestions/" + suggestion.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCEPTED")));

        Product updatedPrd = productRepository.findById(prd.getId()).orElseThrow();
        assertThat(updatedPrd.getStockLevel()).isEqualTo(initialStock + suggestion.getRecommendedQuantity());
    }

    @Test
    void testListSuggestions() throws Exception {
        mockMvc.perform(get("/pricing-suggestions"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/reorder-suggestions"))
                .andExpect(status().isOk());
    }
}
