package com.stockpulse.repository;

import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.SuggestionStatus;
import com.stockpulse.domain.TriggerReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PricingSuggestionRepository extends JpaRepository<PricingSuggestion, Long> {

    List<PricingSuggestion> findByStatusOrderByCreatedAtDesc(SuggestionStatus status);

    List<PricingSuggestion> findByProductIdOrderByCreatedAtDesc(Long productId);

    /** Idempotency check: one PENDING suggestion per product + trigger + type. */
    boolean existsByProductIdAndStatusAndTriggerReason(Long productId, SuggestionStatus status, TriggerReason triggerReason);

    Optional<PricingSuggestion> findByProductIdAndStatus(Long productId, SuggestionStatus status);
}
