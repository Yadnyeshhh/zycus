package com.stockpulse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpulse.domain.Category;
import com.stockpulse.domain.Product;
import com.stockpulse.dto.CreateOrderRequest;
import com.stockpulse.dto.CreateProductRequest;
import com.stockpulse.dto.UpdateStockRequest;
import com.stockpulse.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void testGetProductsListAndFiltering() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)));

        mockMvc.perform(get("/products?category=ELECTRONICS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void testGetProductById() throws Exception {
        Product prd1 = productRepository.findBySku("SKU-ELEC-001").orElseThrow();

        mockMvc.perform(get("/products/" + prd1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku", is("SKU-ELEC-001")))
                .andExpect(jsonPath("$.name", is("Wireless Earbuds Pro")))
                .andExpect(jsonPath("$.currentPrice", is(79.99)));
    }

    @Test
    void testCreateProduct() throws Exception {
        CreateProductRequest req = CreateProductRequest.builder()
                .sku("SKU-TEST-001")
                .name("Test Gaming Mouse")
                .category(Category.ELECTRONICS)
                .currentPrice(new BigDecimal("49.99"))
                .stockLevel(100)
                .reorderThreshold(20)
                .demandVelocity(5)
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku", is("SKU-TEST-001")))
                .andExpect(jsonPath("$.name", is("Test Gaming Mouse")))
                .andExpect(jsonPath("$.stockLevel", is(100)));
    }

    @Test
    void testUpdateStock() throws Exception {
        Product prd = productRepository.findBySku("SKU-ELEC-002").orElseThrow();
        UpdateStockRequest req = new UpdateStockRequest(50);

        mockMvc.perform(patch("/products/" + prd.getId() + "/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockLevel", is(50)));
    }

    @Test
    void testSimulateOrder() throws Exception {
        Product prd = productRepository.findBySku("SKU-APP-002").orElseThrow();
        int initialStock = prd.getStockLevel();
        int initialVelocity = prd.getDemandVelocity();

        CreateOrderRequest req = new CreateOrderRequest(3);

        mockMvc.perform(post("/products/" + prd.getId() + "/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockLevel", is(initialStock - 3)))
                .andExpect(jsonPath("$.demandVelocity", is(initialVelocity + 3)));
    }

    @Test
    void testSuggestPricingStream() throws Exception {
        Product prd1 = productRepository.findBySku("SKU-ELEC-001").orElseThrow();

        mockMvc.perform(post("/products/" + prd1.getId() + "/suggest-pricing/stream"))
                .andExpect(status().isOk());
    }

    @Test
    void testNotFoundAndValidationErrors() throws Exception {
        mockMvc.perform(get("/products/99999"))
                .andExpect(status().isNotFound());

        CreateProductRequest invalidReq = CreateProductRequest.builder()
                .sku("")
                .name("")
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest());
    }
}
