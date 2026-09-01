package com.stockpulse.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Provider-specific HTTP client for Gemini, Groq, Ollama, OpenAI-compatible APIs.
 * Returns raw text response — parsing, validation, fallback are handled by AiCommerceAdvisor.
 */
@Component
@Slf4j
public class LLMGateway {

    @Value("${llm.provider:mock}")
    private String provider;

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.model:gemini-1.5-flash}")
    private String model;

    @Value("${llm.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;

    private final RestClient http = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String callLLM(String prompt) {
        if (apiKey == null || apiKey.isBlank() || "mock".equalsIgnoreCase(provider)) {
            log.info("No API key configured or mock provider selected. Using mock LLM response generator.");
            return generateMockResponse(prompt);
        }

        try {
            return switch (provider.toLowerCase()) {
                case "gemini" -> callGemini(prompt);
                case "groq" -> callOpenAICompatible(prompt, baseUrl + "/openai/v1/chat/completions");
                case "ollama" -> callOpenAICompatible(prompt, baseUrl + "/v1/chat/completions");
                case "openai" -> callOpenAICompatible(prompt, baseUrl + "/v1/chat/completions");
                default -> {
                    log.warn("Unknown LLM provider: {}. Falling back to mock generator.", provider);
                    yield generateMockResponse(prompt);
                }
            };
        } catch (Exception e) {
            log.error("LLM call failed for provider {}: {}", provider, e.getMessage());
            throw new RuntimeException("LLM execution error: " + e.getMessage(), e);
        }
    }

    private String callGemini(String prompt) {
        String url = String.format("%s/v1beta/models/%s:generateContent?key=%s", baseUrl, model, apiKey);

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "responseMimeType", "application/json"
                )
        );

        String rawResponse = http.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", rawResponse, e);
            throw new RuntimeException("Unparseable Gemini response", e);
        }
    }

    private String callOpenAICompatible(String prompt, String url) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are an expert commerce pricing and inventory replenishment engine. Always respond in valid JSON matching the requested schema."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2,
                "response_format", Map.of("type", "json_object")
        );

        RestClient.RequestBodySpec spec = http.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON);

        if (apiKey != null && !apiKey.isBlank()) {
            spec.header("Authorization", "Bearer " + apiKey);
        }

        String rawResponse = spec.body(body).retrieve().body(String.class);

        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            log.error("Failed to parse OpenAI-compatible response: {}", rawResponse, e);
            throw new RuntimeException("Unparseable OpenAI-compatible response", e);
        }
    }

    private String generateMockResponse(String prompt) {
        if (prompt.contains("INVENTORY_LOW")) {
            return """
                    {
                      "recommendedPrice": 27.99,
                      "direction": "INCREASE",
                      "recommendedQuantity": 45,
                      "suggestedLeadTimeDays": 5,
                      "confidence": 0.88,
                      "reasoning": "AI Analysis (Low Stock): Critical inventory level detected relative to safety threshold. Raising price by ~12% protects remaining stock while capturing margin, and ordering 45 units restores optimal 30-day buffer given current sales velocity."
                    }
                    """;
        } else if (prompt.contains("DEMAND_SPIKE")) {
            return """
                    {
                      "recommendedPrice": 59.99,
                      "direction": "INCREASE",
                      "recommendedQuantity": 50,
                      "suggestedLeadTimeDays": 4,
                      "confidence": 0.92,
                      "reasoning": "AI Analysis (Demand Spike): Surge in sales velocity detected at 3.5x category baseline. A 9% price increase optimizes gross revenue without dampening strong organic conversion momentum."
                    }
                    """;
        } else {
            return """
                    {
                      "recommendedPrice": 49.99,
                      "direction": "HOLD",
                      "recommendedQuantity": 30,
                      "suggestedLeadTimeDays": 7,
                      "confidence": 0.85,
                      "reasoning": "AI Analysis (Routine Evaluation): Demand and stock levels are stable and tracking within normal variance. Recommend maintaining current price."
                    }
                    """;
        }
    }
}
