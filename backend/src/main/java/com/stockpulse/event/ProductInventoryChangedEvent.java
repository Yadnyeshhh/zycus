package com.stockpulse.event;

import com.stockpulse.domain.Category;
import com.stockpulse.domain.TriggerReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ProductInventoryChangedEvent {
    private Long productId;
    private int stockLevel;
    private int reorderThreshold;
    private int demandVelocity;
    private Category category;
    private TriggerReason triggerReason;
    private int changeAmount;
    private String changeType; // "STOCK_UPDATE" or "ORDER"
}
