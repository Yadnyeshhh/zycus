package com.stockpulse.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.stockpulse.domain.SuggestionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionDecisionRequest {

    @JsonAlias({"action", "decision"})
    private String status;

    public SuggestionStatus parseStatus() {
        if (status == null || status.isBlank()) {
            return null;
        }
        String s = status.trim().toUpperCase();
        if ("ACCEPT".equals(s) || "ACCEPTED".equals(s)) {
            return SuggestionStatus.ACCEPTED;
        }
        if ("REJECT".equals(s) || "REJECTED".equals(s)) {
            return SuggestionStatus.REJECTED;
        }
        return SuggestionStatus.valueOf(s);
    }
}
