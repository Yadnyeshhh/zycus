package com.stockpulse.web;

import com.stockpulse.domain.SuggestionStatus;
import com.stockpulse.dto.ReorderSuggestionResponse;
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
@RequestMapping("/reorder-suggestions")
@RequiredArgsConstructor
public class ReorderSuggestionController {

    private final SuggestionService suggestionService;

    @GetMapping
    public ResponseEntity<List<ReorderSuggestionResponse>> getReorderSuggestions(
            @RequestParam(name = "status", required = false) SuggestionStatus status,
            @RequestParam(name = "productId", required = false) Long productId) {
        List<ReorderSuggestionResponse> list = suggestionService.getReorderSuggestions(status, productId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReorderSuggestionResponse> getReorderSuggestionById(@PathVariable("id") Long id) {
        ReorderSuggestionResponse response = suggestionService.getReorderSuggestionById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ReorderSuggestionResponse> decideReorderSuggestion(
            @PathVariable("id") Long id,
            @RequestBody SuggestionDecisionRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        SuggestionStatus decision = request.parseStatus();
        if (decision == null) {
            throw new BadRequestException("Invalid decision status. Must be ACCEPTED or REJECTED (or action: ACCEPT / REJECT)");
        }
        ReorderSuggestionResponse response = suggestionService.decideReorderSuggestion(id, decision);
        return ResponseEntity.ok(response);
    }
}
