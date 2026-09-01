package com.stockpulse.loop;

import com.stockpulse.domain.SuggestionStatus;
import com.stockpulse.domain.TriggerReason;
import com.stockpulse.event.ProductInventoryChangedEvent;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import com.stockpulse.service.SuggestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgenticRecommendationListener {

    private final SuggestionService suggestionService;
    private final PricingSuggestionRepository pricingRepo;
    private final ReorderSuggestionRepository reorderRepo;

    @Async
    @EventListener
    public void onInventoryChanged(ProductInventoryChangedEvent event) {
        TriggerReason trigger = event.getTriggerReason();
        if (trigger == null || trigger == TriggerReason.INITIAL || trigger == TriggerReason.MANUAL) {
            log.debug("Ignoring routine inventory event: {}", event);
            return;
        }

        Long productId = event.getProductId();
        log.info("Agentic Loop received trigger {} for product ID {}", trigger, productId);

        try {
            processTrigger(productId, trigger);
        } catch (Exception e) {
            log.error("Error in async agentic recommendation loop for product ID {}: {}", productId, e.getMessage(), e);
        }
    }

    public void processTrigger(Long productId, TriggerReason trigger) {
        // Idempotency: Skip duplicate PENDING suggestion for same product + trigger + type
        boolean hasPendingPricing = pricingRepo.existsByProductIdAndStatusAndTriggerReason(productId, SuggestionStatus.PENDING, trigger);
        if (!hasPendingPricing) {
            log.info("Agentic Loop generating {} pricing suggestion for product ID {}", trigger, productId);
            suggestionService.generatePricingSuggestion(productId, trigger);
        } else {
            log.info("Idempotency: PENDING {} pricing suggestion already exists for product ID {}, skipping", trigger, productId);
        }

        boolean hasPendingReorder = reorderRepo.existsByProductIdAndStatusAndTriggerReason(productId, SuggestionStatus.PENDING, trigger);
        if (!hasPendingReorder) {
            log.info("Agentic Loop generating {} reorder suggestion for product ID {}", trigger, productId);
            suggestionService.generateReorderSuggestion(productId, trigger);
        } else {
            log.info("Idempotency: PENDING {} reorder suggestion already exists for product ID {}, skipping", trigger, productId);
        }
    }
}
