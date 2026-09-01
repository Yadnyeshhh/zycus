package com.stockpulse.dto;

import com.stockpulse.domain.PriceDirection;
import com.stockpulse.domain.PricingSuggestion;
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
public class PricingSuggestionResponse {
    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private BigDecimal currentPrice;
    private BigDecimal recommendedPrice;
    private PriceDirection direction;
    private BigDecimal confidence;
    private String reasoning;
    private SuggestionStatus status;
    private TriggerReason triggerReason;
    private String strategy;
    private Instant createdAt;
    private Instant decidedAt;

    public static PricingSuggestionResponse fromEntity(PricingSuggestion s) {
        if (s == null) return null;
        return PricingSuggestionResponse.builder()
                .id(s.getId())
                .productId(s.getProduct().getId())
                .productSku(s.getProduct().getSku())
                .productName(s.getProduct().getName())
                .currentPrice(s.getCurrentPrice())
                .recommendedPrice(s.getRecommendedPrice())
                .direction(s.getDirection())
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
