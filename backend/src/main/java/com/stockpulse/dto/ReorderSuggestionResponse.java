package com.stockpulse.dto;

import com.stockpulse.domain.ReorderSuggestion;
import com.stockpulse.domain.SuggestionStatus;
import com.stockpulse.domain.TriggerReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderSuggestionResponse {
    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private int currentStock;
    private int recommendedQuantity;
    private int suggestedLeadTimeDays;
    private BigDecimal confidence;
    private String reasoning;
    private SuggestionStatus status;
    private TriggerReason triggerReason;
    private String strategy;
    private Instant createdAt;
    private Instant decidedAt;

    public static ReorderSuggestionResponse fromEntity(ReorderSuggestion s) {
        if (s == null) return null;
        return ReorderSuggestionResponse.builder()
                .id(s.getId())
                .productId(s.getProduct().getId())
                .productSku(s.getProduct().getSku())
                .productName(s.getProduct().getName())
                .currentStock(s.getCurrentStock())
                .recommendedQuantity(s.getRecommendedQuantity())
                .suggestedLeadTimeDays(s.getSuggestedLeadTimeDays())
                .confidence(s.getConfidence())
                .reasoning(s.getReasoning())
                .status(s.getStatus())
                .triggerReason(s.getTriggerReason())
                .strategy(s.getStrategy())
                .createdAt(s.getCreatedAt())
                .decidedAt(s.getDecidedAt())
                .build();
    }
}
