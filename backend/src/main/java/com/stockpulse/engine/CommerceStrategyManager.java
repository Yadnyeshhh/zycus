package com.stockpulse.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CommerceStrategyManager {

    private volatile StrategyType activeStrategyType = StrategyType.RULE_BASED;
    private final Map<StrategyType, PricingStrategy> pricingStrategies = new EnumMap<>(StrategyType.class);
    private final Map<StrategyType, ReorderStrategy> reorderStrategies = new EnumMap<>(StrategyType.class);

    public CommerceStrategyManager(List<PricingStrategy> pricingList, List<ReorderStrategy> reorderList) {
        for (PricingStrategy s : pricingList) {
            pricingStrategies.put(s.getType(), s);
        }
        for (ReorderStrategy s : reorderList) {
            reorderStrategies.put(s.getType(), s);
        }
        log.info("Initialized CommerceStrategyManager with active strategy: {}, pricing strategies: {}, reorder strategies: {}",
                activeStrategyType, pricingStrategies.keySet(), reorderStrategies.keySet());
    }

    public StrategyType getActiveStrategyType() {
        return activeStrategyType;
    }

    public synchronized void setActiveStrategyType(StrategyType type) {
        if (!pricingStrategies.containsKey(type) && !reorderStrategies.containsKey(type)) {
            throw new IllegalArgumentException("Strategy type not registered: " + type);
        }
        StrategyType old = this.activeStrategyType;
        this.activeStrategyType = type;
        log.info("Switched commerce active strategy from {} to {} at runtime", old, type);
    }

    public PricingStrategy getActivePricingStrategy() {
        PricingStrategy strategy = pricingStrategies.get(activeStrategyType);
        if (strategy == null) {
            log.warn("No pricing strategy for {}, falling back to RULE_BASED", activeStrategyType);
            return pricingStrategies.get(StrategyType.RULE_BASED);
        }
        return strategy;
    }

    public ReorderStrategy getActiveReorderStrategy() {
        ReorderStrategy strategy = reorderStrategies.get(activeStrategyType);
        if (strategy == null) {
            log.warn("No reorder strategy for {}, falling back to RULE_BASED", activeStrategyType);
            return reorderStrategies.get(StrategyType.RULE_BASED);
        }
        return strategy;
    }

    public PricingStrategy getRuleBasedPricingStrategy() {
        return pricingStrategies.get(StrategyType.RULE_BASED);
    }

    public ReorderStrategy getRuleBasedReorderStrategy() {
        return reorderStrategies.get(StrategyType.RULE_BASED);
    }
}
