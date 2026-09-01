package com.stockpulse.web;

import com.stockpulse.domain.Category;
import com.stockpulse.domain.ProductStatus;
import com.stockpulse.dto.CreateOrderRequest;
import com.stockpulse.dto.CreateProductRequest;
import com.stockpulse.dto.ProductResponse;
import com.stockpulse.dto.UpdateStockRequest;
import com.stockpulse.exception.BadRequestException;
import com.stockpulse.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final com.stockpulse.service.SuggestionService suggestionService;

    @PostMapping("/{id}/suggest-pricing")
    public ResponseEntity<com.stockpulse.dto.PricingSuggestionResponse> suggestPricing(@PathVariable("id") Long id) {
        com.stockpulse.dto.PricingSuggestionResponse response = suggestionService.generatePricingSuggestion(id, com.stockpulse.domain.TriggerReason.MANUAL);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{id}/suggest-pricing/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter suggestPricingStream(@PathVariable("id") Long id) {
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(30000L);
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                com.stockpulse.dto.PricingSuggestionResponse response = suggestionService.generatePricingSuggestion(id, com.stockpulse.domain.TriggerReason.MANUAL);
                String reasoning = response.getReasoning();
                String[] words = reasoning.split(" ");
                for (String word : words) {
                    emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().name("token").data(word + " "));
                    Thread.sleep(30);
                }
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().name("complete").data(response));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    @PostMapping("/{id}/suggest-reorder")
    public ResponseEntity<com.stockpulse.dto.ReorderSuggestionResponse> suggestReorder(@PathVariable("id") Long id) {
        com.stockpulse.dto.ReorderSuggestionResponse response = suggestionService.generateReorderSuggestion(id, com.stockpulse.domain.TriggerReason.MANUAL);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts(
            @RequestParam(name = "status", required = false) ProductStatus status,
            @RequestParam(name = "category", required = false) Category category) {
        List<ProductResponse> products = productService.getProducts(status, category);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable("id") Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductResponse> updateStock(
            @PathVariable("id") Long id,
            @RequestBody(required = false) UpdateStockRequest request) {
        if (request == null || request.getStockLevel() == null) {
            throw new BadRequestException("Stock level is required");
        }
        ProductResponse updated = productService.updateStock(id, request.getStockLevel());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/orders")
    public ResponseEntity<ProductResponse> simulateOrder(
            @PathVariable("id") Long id,
            @RequestBody(required = false) CreateOrderRequest request) {
        int qty = (request != null && request.getQuantity() != null) ? request.getQuantity() : 1;
        ProductResponse updated = productService.recordOrder(id, qty);
        return ResponseEntity.ok(updated);
    }
}
