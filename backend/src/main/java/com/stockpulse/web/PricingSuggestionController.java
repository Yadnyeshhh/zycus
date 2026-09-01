package com.stockpulse.web;

import com.stockpulse.domain.SuggestionStatus;
import com.stockpulse.dto.PricingSuggestionResponse;
import com.stockpulse.dto.SuggestionDecisionRequest;
import com.stockpulse.exception.BadRequestException;
import com.stockpulse.service.SuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pricing-suggestions")
@RequiredArgsConstructor
public class PricingSuggestionController {

    private final SuggestionService suggestionService;

    @GetMapping
    public ResponseEntity<List<PricingSuggestionResponse>> getPricingSuggestions(
            @RequestParam(name = "status", required = false) SuggestionStatus status,
            @RequestParam(name = "productId", required = false) Long productId) {
        List<PricingSuggestionResponse> list = suggestionService.getPricingSuggestions(status, productId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PricingSuggestionResponse> getPricingSuggestionById(@PathVariable("id") Long id) {
        PricingSuggestionResponse response = suggestionService.getPricingSuggestionById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PricingSuggestionResponse> decidePricingSuggestion(
            @PathVariable("id") Long id,
            @RequestBody SuggestionDecisionRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        SuggestionStatus decision = request.parseStatus();
        if (decision == null) {
            throw new BadRequestException("Invalid decision status. Must be ACCEPTED or REJECTED (or action: ACCEPT / REJECT)");
        }
        PricingSuggestionResponse response = suggestionService.decidePricingSuggestion(id, decision);
        return ResponseEntity.ok(response);
    }
}
