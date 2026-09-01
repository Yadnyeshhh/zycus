package com.stockpulse.web;

import com.stockpulse.engine.CommerceStrategyManager;
import com.stockpulse.engine.StrategyType;
import com.stockpulse.exception.BadRequestException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/commerce/strategy")
@RequiredArgsConstructor
public class CommerceController {

    private final CommerceStrategyManager strategyManager;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrategySwitchRequest {
        private String strategy;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getActiveStrategy() {
        List<String> available = Arrays.stream(StrategyType.values())
                .filter(s -> s != StrategyType.COMPETITOR_AWARE)
                .map(Enum::name)
                .toList();

        return ResponseEntity.ok(Map.of(
                "activeStrategy", strategyManager.getActiveStrategyType().name(),
                "availableStrategies", available
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> switchStrategy(@RequestBody StrategySwitchRequest request) {
        if (request == null || request.getStrategy() == null || request.getStrategy().isBlank()) {
            throw new BadRequestException("Strategy name is required (e.g. RULE_BASED or AI)");
        }
        try {
            StrategyType type = StrategyType.valueOf(request.getStrategy().trim().toUpperCase());
            strategyManager.setActiveStrategyType(type);
            return getActiveStrategy();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid strategy: " + request.getStrategy() + ". Valid options: RULE_BASED, AI");
        }
    }
}
