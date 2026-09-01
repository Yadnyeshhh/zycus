package com.stockpulse.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpulse.domain.PriceDirection;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.TriggerReason;
import com.stockpulse.engine.PricingRecommendation;
import com.stockpulse.engine.ReorderRecommendation;
import com.stockpulse.engine.RuleBasedPricingStrategy;
import com.stockpulse.engine.RuleBasedReorderStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiCommerceAdvisor {

    private final LLMGateway llmGateway;
    private final RuleBasedPricingStrategy rulePricingStrategy;
    private final RuleBasedReorderStrategy ruleReorderStrategy;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PricingRecommendation recommendPricing(Product product, TriggerReason triggerReason, double categoryAvgVelocity) {
        try {
            String prompt = buildPricingPrompt(product, triggerReason, categoryAvgVelocity);
            String rawJson = llmGateway.callLLM(prompt);
            return parseAndValidatePricing(rawJson, product, triggerReason, categoryAvgVelocity);
        } catch (Exception e) {
            log.warn("AI Pricing Advisor failed for product {} (ID: {}): {}. Falling back to Rule-Based Strategy.",
                    product.getSku(), product.getId(), e.getMessage());
            PricingRecommendation fallback = rulePricingStrategy.recommendPricing(product, triggerReason, categoryAvgVelocity);
            fallback.setReasoning("[AI Fallback to Rules - " + e.getMessage() + "] " + fallback.getReasoning());
            fallback.setStrategyName("AI_FALLBACK_RULE");
            return fallback;
        }
    }

    public ReorderRecommendation recommendReorder(Product product, TriggerReason triggerReason, double categoryAvgVelocity) {
        try {
            String prompt = buildReorderPrompt(product, triggerReason, categoryAvgVelocity);
            String rawJson = llmGateway.callLLM(prompt);
            return parseAndValidateReorder(rawJson, product, triggerReason, categoryAvgVelocity);
        } catch (Exception e) {
            log.warn("AI Reorder Advisor failed for product {} (ID: {}): {}. Falling back to Rule-Based Strategy.",
                    product.getSku(), product.getId(), e.getMessage());
            ReorderRecommendation fallback = ruleReorderStrategy.recommendReorder(product, triggerReason, categoryAvgVelocity);
            fallback.setReasoning("[AI Fallback to Rules - " + e.getMessage() + "] " + fallback.getReasoning());
            fallback.setStrategyName("AI_FALLBACK_RULE");
            return fallback;
        }
    }

    private String buildPricingPrompt(Product product, TriggerReason triggerReason, double categoryAvgVelocity) {
        if (triggerReason == TriggerReason.INVENTORY_LOW) {
            return String.format("""
                    You are an expert commerce merchandising AI advisor at ShopStream.
                    
                    TRIGGER CONTEXT: CRITICAL INVENTORY DEPLETION
                    The stock for this item has dropped below its reorder safety threshold.
                    Merchandising Dilemma:
                    - Option A: Increase price moderately (5%% - 20%%) to slow down demand velocity, protect remaining units, and capture higher margin per unit while replenishment is underway.
                    - Option B: If the item has poor conversion or is discontinued, apply a clearance discount (DECREASE).
                    - Option C: HOLD if demand is inelastic.
                    
                    PRODUCT CONTEXT:
                    - SKU: %s
                    - Product Name: %s
                    - Category: %s
                    - Current Price: $%s
                    - Current Stock Level: %d units
                    - Reorder Threshold: %d units (Stock is below threshold!)
                    - Demand Velocity (orders in 24h): %d
                    - Category Average Velocity: %.2f
                    
                    Return a JSON object with:
                    {
                      "recommendedPrice": <number, e.g. 29.99>,
                      "direction": "<INCREASE|DECREASE|HOLD>",
                      "confidence": <number between 0.0 and 1.0>,
                      "reasoning": "<clear 1-2 sentence merchandising rationale for human approver>"
                    }
                    """,
                    product.getSku(), product.getName(), product.getCategory(), product.getCurrentPrice(),
                    product.getStockLevel(), product.getReorderThreshold(), product.getDemandVelocity(), categoryAvgVelocity);
        } else if (triggerReason == TriggerReason.DEMAND_SPIKE) {
            return String.format("""
                    You are an expert commerce merchandising AI advisor at ShopStream.
                    
                    TRIGGER CONTEXT: DEMAND VELOCITY SPIKE (VIRAL SURGE)
                    This item is experiencing a rapid surge in sales orders significantly exceeding historical and category baselines.
                    Merchandising Directive:
                    - Capitalize on heightened consumer intent and willingness to pay by recommending a modest, justifiable price increase (3%% - 15%%).
                    - Avoid excessive price gouging which could degrade brand reputation.
                    
                    PRODUCT CONTEXT:
                    - SKU: %s
                    - Product Name: %s
                    - Category: %s
                    - Current Price: $%s
                    - Current Stock Level: %d units
                    - Reorder Threshold: %d units
                    - Demand Velocity (orders in 24h): %d (Spike vs Category Avg of %.2f!)
                    - Category Average Velocity: %.2f
                    
                    Return a JSON object with:
                    {
                      "recommendedPrice": <number, e.g. 59.99>,
                      "direction": "<INCREASE|DECREASE|HOLD>",
                      "confidence": <number between 0.0 and 1.0>,
                      "reasoning": "<clear 1-2 sentence merchandising rationale highlighting the velocity spike>"
                    }
                    """,
                    product.getSku(), product.getName(), product.getCategory(), product.getCurrentPrice(),
                    product.getStockLevel(), product.getReorderThreshold(), product.getDemandVelocity(), categoryAvgVelocity, categoryAvgVelocity);
        } else {
            return String.format("""
                    You are an expert commerce merchandising AI advisor at ShopStream.
                    
                    TRIGGER CONTEXT: ROUTINE / ON-DEMAND MERCHANDISING EVALUATION
                    Evaluate whether the product price is optimal given current velocity and stock buffers.
                    
                    PRODUCT CONTEXT:
                    - SKU: %s
                    - Product Name: %s
                    - Category: %s
                    - Current Price: $%s
                    - Current Stock Level: %d units
                    - Reorder Threshold: %d units
                    - Demand Velocity: %d
                    - Category Average Velocity: %.2f
                    
                    Return a JSON object with:
                    {
                      "recommendedPrice": <number>,
                      "direction": "<INCREASE|DECREASE|HOLD>",
                      "confidence": <number between 0.0 and 1.0>,
                      "reasoning": "<clear 1-2 sentence merchandising explanation>"
                    }
                    """,
                    product.getSku(), product.getName(), product.getCategory(), product.getCurrentPrice(),
                    product.getStockLevel(), product.getReorderThreshold(), product.getDemandVelocity(), categoryAvgVelocity);
        }
    }

    private String buildReorderPrompt(Product product, TriggerReason triggerReason, double categoryAvgVelocity) {
        return String.format("""
                You are an inventory replenishment specialist at ShopStream.
                
                TRIGGER: %s
                Calculate optimal reorder quantity and lead time to maintain stock without over-committing capital.
                
                PRODUCT DETAILS:
                - SKU: %s (%s, Category: %s)
                - Current Stock: %d units
                - Reorder Safety Threshold: %d units
                - Recent 24h Sales Velocity: %d orders
                - Category Average Velocity: %.2f
                
                Return a JSON object with:
                {
                  "recommendedQuantity": <positive integer>,
                  "suggestedLeadTimeDays": <positive integer, typically 3 to 14 days>,
                  "confidence": <number between 0.0 and 1.0>,
                  "reasoning": "<concise justification for the batch size>"
                }
                """,
                triggerReason, product.getSku(), product.getName(), product.getCategory(),
                product.getStockLevel(), product.getReorderThreshold(), product.getDemandVelocity(), categoryAvgVelocity);
    }

    private PricingRecommendation parseAndValidatePricing(String json, Product product, TriggerReason trigger, double categoryAvg) {
        try {
            JsonNode root = objectMapper.readTree(cleanJsonString(json));
            double recPriceVal = root.path("recommendedPrice").asDouble(product.getCurrentPrice().doubleValue());
            BigDecimal recPrice = BigDecimal.valueOf(recPriceVal).setScale(2, RoundingMode.HALF_UP);

            // Sane bounds check
            BigDecimal minAllowed = product.getCurrentPrice().multiply(new BigDecimal("0.20"));
            BigDecimal maxAllowed = product.getCurrentPrice().multiply(new BigDecimal("3.00"));
            if (recPrice.compareTo(minAllowed) < 0 || recPrice.compareTo(maxAllowed) > 0 || recPrice.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("AI recommended price ${} outside sane bounds [${}, ${}]. Falling back to rules.", recPrice, minAllowed, maxAllowed);
                return rulePricingStrategy.recommendPricing(product, trigger, categoryAvg);
            }

            String dirStr = root.path("direction").asText("HOLD").toUpperCase();
            PriceDirection direction;
            try {
                direction = PriceDirection.valueOf(dirStr);
            } catch (Exception e) {
                int cmp = recPrice.compareTo(product.getCurrentPrice());
                direction = (cmp > 0) ? PriceDirection.INCREASE : ((cmp < 0) ? PriceDirection.DECREASE : PriceDirection.HOLD);
            }

            double confVal = root.path("confidence").asDouble(0.85);
            confVal = Math.max(0.1, Math.min(1.0, confVal));
            BigDecimal confidence = BigDecimal.valueOf(confVal).setScale(3, RoundingMode.HALF_UP);

            String reasoning = root.path("reasoning").asText("AI-generated dynamic pricing recommendation.");

            return PricingRecommendation.builder()
                    .recommendedPrice(recPrice)
                    .direction(direction)
                    .confidence(confidence)
                    .reasoning(reasoning)
                    .strategyName("AI")
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse AI pricing JSON: {}", json, e);
            throw new RuntimeException("Malformed JSON from LLM", e);
        }
    }

    private ReorderRecommendation parseAndValidateReorder(String json, Product product, TriggerReason trigger, double categoryAvg) {
        try {
            JsonNode root = objectMapper.readTree(cleanJsonString(json));
            int recQty = root.path("recommendedQuantity").asInt(Math.max(1, (product.getReorderThreshold() * 3) - product.getStockLevel()));
            if (recQty < 1) {
                recQty = 1;
            }

            int leadTime = root.path("suggestedLeadTimeDays").asInt(7);
            if (leadTime < 1) leadTime = 7;

            double confVal = root.path("confidence").asDouble(0.85);
            confVal = Math.max(0.1, Math.min(1.0, confVal));
            BigDecimal confidence = BigDecimal.valueOf(confVal).setScale(3, RoundingMode.HALF_UP);

            String reasoning = root.path("reasoning").asText("AI-generated replenishment recommendation.");

            return ReorderRecommendation.builder()
                    .recommendedQuantity(recQty)
                    .suggestedLeadTimeDays(leadTime)
                    .confidence(confidence)
                    .reasoning(reasoning)
                    .strategyName("AI")
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse AI reorder JSON: {}", json, e);
            throw new RuntimeException("Malformed JSON from LLM", e);
        }
    }

    private String cleanJsonString(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
