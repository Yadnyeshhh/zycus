package com.stockpulse;

import com.stockpulse.domain.Category;
import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ProductStatus;
import com.stockpulse.domain.ReorderSuggestion;
import com.stockpulse.domain.SuggestionStatus;
import com.stockpulse.domain.TriggerReason;
import com.stockpulse.dto.CreateProductRequest;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import com.stockpulse.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AgenticLoopTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PricingSuggestionRepository pricingRepo;

    @Autowired
    private ReorderSuggestionRepository reorderRepo;

    @Test
    void testLowStockAutomaticallyQueuesSuggestions() throws InterruptedException {
        var created = productService.createProduct(CreateProductRequest.builder()
                .sku("SKU-LOOP-001")
                .name("Loop Low Stock Item")
                .category(Category.ELECTRONICS)
                .currentPrice(new BigDecimal("50.00"))
                .stockLevel(12)
                .reorderThreshold(10)
                .demandVelocity(0)
                .build());

        // Order 3 units -> stock becomes 9 (9 < 10 threshold) -> INVENTORY_LOW trigger
        // velocity becomes 3 (within category average)
        productService.recordOrder(created.getId(), 3);

        // Allow async listener to complete
        Thread.sleep(300);

        List<PricingSuggestion> pricingList = pricingRepo.findByProductIdOrderByCreatedAtDesc(created.getId());
        assertThat(pricingList).isNotEmpty();
        PricingSuggestion pSug = pricingList.stream()
                .filter(s -> s.getTriggerReason() == TriggerReason.INVENTORY_LOW)
                .findFirst().orElseThrow();
        assertThat(pSug.getStatus()).isEqualTo(SuggestionStatus.PENDING);

        List<ReorderSuggestion> reorderList = reorderRepo.findByProductIdOrderByCreatedAtDesc(created.getId());
        assertThat(reorderList).isNotEmpty();
        ReorderSuggestion rSug = reorderList.stream()
                .filter(s -> s.getTriggerReason() == TriggerReason.INVENTORY_LOW)
                .findFirst().orElseThrow();
        assertThat(rSug.getStatus()).isEqualTo(SuggestionStatus.PENDING);
    }

    @Test
    void testIdempotencyPreventsDuplicatePendingSuggestions() throws InterruptedException {
        var created = productService.createProduct(CreateProductRequest.builder()
                .sku("SKU-LOOP-002")
                .name("Loop Idempotency Item")
                .category(Category.APPAREL)
                .currentPrice(new BigDecimal("30.00"))
                .stockLevel(12)
                .reorderThreshold(10)
                .demandVelocity(1)
                .build());

        // First order triggers low inventory (stock 9 < 10)
        productService.recordOrder(created.getId(), 3);
        Thread.sleep(300);

        int pricingCountAfterFirst = pricingRepo.findByProductIdOrderByCreatedAtDesc(created.getId()).size();
        int reorderCountAfterFirst = reorderRepo.findByProductIdOrderByCreatedAtDesc(created.getId()).size();
        assertThat(pricingCountAfterFirst).isEqualTo(1);
        assertThat(reorderCountAfterFirst).isEqualTo(1);

        // Second order drops stock further (stock 7 < 10) but PENDING already exists
        productService.recordOrder(created.getId(), 2);
        Thread.sleep(300);

        int pricingCountAfterSecond = pricingRepo.findByProductIdOrderByCreatedAtDesc(created.getId()).size();
        int reorderCountAfterSecond = reorderRepo.findByProductIdOrderByCreatedAtDesc(created.getId()).size();
        assertThat(pricingCountAfterSecond).isEqualTo(1); // Idempotent, did not duplicate!
        assertThat(reorderCountAfterSecond).isEqualTo(1);
    }

    @Test
    void testDemandSpikeTriggerQueuesSuggestions() throws InterruptedException {
        var created = productService.createProduct(CreateProductRequest.builder()
                .sku("SKU-LOOP-003")
                .name("Loop Viral Item")
                .category(Category.HOME)
                .currentPrice(new BigDecimal("80.00"))
                .stockLevel(100)
                .reorderThreshold(10)
                .demandVelocity(0)
                .build());

        // Order 25 units -> velocity becomes 25 (way above category avg ~3.0) -> DEMAND_SPIKE trigger
        productService.recordOrder(created.getId(), 25);
        Thread.sleep(300);

        List<PricingSuggestion> pricingList = pricingRepo.findByProductIdOrderByCreatedAtDesc(created.getId());
        assertThat(pricingList).anyMatch(s -> s.getTriggerReason() == TriggerReason.DEMAND_SPIKE && s.getStatus() == SuggestionStatus.PENDING);
    }
}
