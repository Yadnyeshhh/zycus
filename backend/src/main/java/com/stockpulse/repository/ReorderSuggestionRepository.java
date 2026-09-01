package com.stockpulse.repository;

import com.stockpulse.domain.ReorderSuggestion;
import com.stockpulse.domain.SuggestionStatus;
import com.stockpulse.domain.TriggerReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReorderSuggestionRepository extends JpaRepository<ReorderSuggestion, Long> {

    List<ReorderSuggestion> findByStatusOrderByCreatedAtDesc(SuggestionStatus status);

    List<ReorderSuggestion> findByProductIdOrderByCreatedAtDesc(Long productId);

    /** Idempotency check: one PENDING suggestion per product + trigger + type. */
    boolean existsByProductIdAndStatusAndTriggerReason(Long productId, SuggestionStatus status, TriggerReason triggerReason);
}
