package com.stockpulse.service;

import com.stockpulse.domain.InventorySnapshot;
import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ProductStatus;
import com.stockpulse.domain.ReorderSuggestion;
import com.stockpulse.domain.SuggestionStatus;
import com.stockpulse.domain.TriggerReason;
import com.stockpulse.dto.PricingSuggestionResponse;
import com.stockpulse.dto.ReorderSuggestionResponse;
import com.stockpulse.engine.CommerceStrategyManager;
import com.stockpulse.engine.PricingRecommendation;
import com.stockpulse.engine.ReorderRecommendation;
import com.stockpulse.exception.BadRequestException;
import com.stockpulse.exception.ResourceNotFoundException;
import com.stockpulse.repository.InventorySnapshotRepository;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuggestionService {

    private final ProductRepository productRepository;
    private final PricingSuggestionRepository pricingRepo;
    private final ReorderSuggestionRepository reorderRepo;
    private final InventorySnapshotRepository snapshotRepo;
    private final CommerceStrategyManager strategyManager;

    @Transactional
    public PricingSuggestionResponse generatePricingSuggestion(Long productId, TriggerReason triggerReason) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        Double avgVel = productRepository.averageDemandVelocity(product.getCategory());
        double categoryAvg = (avgVel != null) ? avgVel : 1.0;

        PricingRecommendation rec = strategyManager.getActivePricingStrategy()
                .recommendPricing(product, triggerReason, categoryAvg);

        PricingSuggestion suggestion = PricingSuggestion.builder()
                .product(product)
                .currentPrice(product.getCurrentPrice())
                .recommendedPrice(rec.getRecommendedPrice())
                .direction(rec.getDirection())
                .confidence(rec.getConfidence())
                .reasoning(rec.getReasoning())
                .status(SuggestionStatus.PENDING)
                .triggerReason(triggerReason)
                .strategy(rec.getStrategyName())
                .build();

        PricingSuggestion saved = pricingRepo.save(suggestion);

        if (product.getStatus() == ProductStatus.ACTIVE) {
            product.setStatus(ProductStatus.PRICE_REVIEW_PENDING);
            productRepository.save(product);
        }

        log.info("Generated PricingSuggestion {} for product {} (ID: {}) with status PENDING, price {} -> {}",
                saved.getId(), product.getSku(), product.getId(), suggestion.getCurrentPrice(), suggestion.getRecommendedPrice());

        return PricingSuggestionResponse.fromEntity(saved);
    }

    @Transactional
    public ReorderSuggestionResponse generateReorderSuggestion(Long productId, TriggerReason triggerReason) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        Double avgVel = productRepository.averageDemandVelocity(product.getCategory());
        double categoryAvg = (avgVel != null) ? avgVel : 1.0;

        ReorderRecommendation rec = strategyManager.getActiveReorderStrategy()
                .recommendReorder(product, triggerReason, categoryAvg);

        ReorderSuggestion suggestion = ReorderSuggestion.builder()
                .product(product)
                .currentStock(product.getStockLevel())
                .recommendedQuantity(rec.getRecommendedQuantity())
                .suggestedLeadTimeDays(rec.getSuggestedLeadTimeDays())
                .confidence(rec.getConfidence())
                .reasoning(rec.getReasoning())
                .status(SuggestionStatus.PENDING)
                .triggerReason(triggerReason)
                .strategy(rec.getStrategyName())
                .build();

        ReorderSuggestion saved = reorderRepo.save(suggestion);

        log.info("Generated ReorderSuggestion {} for product {} (ID: {}) with status PENDING, qty={}",
                saved.getId(), product.getSku(), product.getId(), suggestion.getRecommendedQuantity());

        return ReorderSuggestionResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<PricingSuggestionResponse> getPricingSuggestions(SuggestionStatus status, Long productId) {
        List<PricingSuggestion> list;
        if (productId != null) {
            list = pricingRepo.findByProductIdOrderByCreatedAtDesc(productId);
            if (status != null) {
                list = list.stream().filter(s -> s.getStatus() == status).toList();
            }
        } else if (status != null) {
            list = pricingRepo.findByStatusOrderByCreatedAtDesc(status);
        } else {
            list = pricingRepo.findAll();
        }
        return list.stream().map(PricingSuggestionResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public PricingSuggestionResponse getPricingSuggestionById(Long id) {
        PricingSuggestion s = pricingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing suggestion not found with id: " + id));
        return PricingSuggestionResponse.fromEntity(s);
    }

    @Transactional
    public PricingSuggestionResponse decidePricingSuggestion(Long id, SuggestionStatus decision) {
        if (decision == null || decision == SuggestionStatus.PENDING) {
            throw new BadRequestException("Decision must be ACCEPTED or REJECTED");
        }

        PricingSuggestion suggestion = pricingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing suggestion not found with id: " + id));

        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            if (suggestion.getStatus() == decision) {
                return PricingSuggestionResponse.fromEntity(suggestion);
            }
            throw new BadRequestException("Suggestion already decided with status: " + suggestion.getStatus());
        }

        suggestion.setStatus(decision);
        suggestion.setDecidedAt(Instant.now());

        Product product = suggestion.getProduct();

        if (decision == SuggestionStatus.ACCEPTED) {
            product.setCurrentPrice(suggestion.getRecommendedPrice());
            log.info("Pricing suggestion {} ACCEPTED: Product {} (ID: {}) price updated to {}",
                    id, product.getSku(), product.getId(), product.getCurrentPrice());
        } else {
            log.info("Pricing suggestion {} REJECTED: Product {} (ID: {}) price remains {}",
                    id, product.getSku(), product.getId(), product.getCurrentPrice());
        }

        // Return product status to ACTIVE / OUT_OF_STOCK
        if (product.getStatus() == ProductStatus.PRICE_REVIEW_PENDING) {
            product.setStatus(product.getStockLevel() == 0 ? ProductStatus.OUT_OF_STOCK : ProductStatus.ACTIVE);
        }

        productRepository.save(product);
        PricingSuggestion saved = pricingRepo.save(suggestion);

        return PricingSuggestionResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ReorderSuggestionResponse> getReorderSuggestions(SuggestionStatus status, Long productId) {
        List<ReorderSuggestion> list;
        if (productId != null) {
            list = reorderRepo.findByProductIdOrderByCreatedAtDesc(productId);
            if (status != null) {
                list = list.stream().filter(s -> s.getStatus() == status).toList();
            }
        } else if (status != null) {
            list = reorderRepo.findByStatusOrderByCreatedAtDesc(status);
        } else {
            list = reorderRepo.findAll();
        }
        return list.stream().map(ReorderSuggestionResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public ReorderSuggestionResponse getReorderSuggestionById(Long id) {
        ReorderSuggestion s = reorderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reorder suggestion not found with id: " + id));
        return ReorderSuggestionResponse.fromEntity(s);
    }

    @Transactional
    public ReorderSuggestionResponse decideReorderSuggestion(Long id, SuggestionStatus decision) {
        if (decision == null || decision == SuggestionStatus.PENDING) {
            throw new BadRequestException("Decision must be ACCEPTED or REJECTED");
        }

        ReorderSuggestion suggestion = reorderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reorder suggestion not found with id: " + id));

        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            if (suggestion.getStatus() == decision) {
                return ReorderSuggestionResponse.fromEntity(suggestion);
            }
            throw new BadRequestException("Suggestion already decided with status: " + suggestion.getStatus());
        }

        suggestion.setStatus(decision);
        suggestion.setDecidedAt(Instant.now());

        Product product = suggestion.getProduct();

        if (decision == SuggestionStatus.ACCEPTED) {
            int oldStock = product.getStockLevel();
            int newStock = oldStock + suggestion.getRecommendedQuantity();
            product.setStockLevel(newStock);

            if (product.getStatus() == ProductStatus.OUT_OF_STOCK && newStock > 0) {
                product.setStatus(ProductStatus.ACTIVE);
            }

            productRepository.save(product);

            snapshotRepo.save(InventorySnapshot.builder()
                    .product(product)
                    .stockLevel(newStock)
                    .demandVelocity(product.getDemandVelocity())
                    .triggerReason(TriggerReason.MANUAL)
                    .build());

            log.info("Reorder suggestion {} ACCEPTED: Product {} (ID: {}) stock incremented from {} to {}",
                    id, product.getSku(), product.getId(), oldStock, newStock);
        } else {
            log.info("Reorder suggestion {} REJECTED: Product {} (ID: {}) stock remains {}",
                    id, product.getSku(), product.getId(), product.getStockLevel());
        }

        ReorderSuggestion saved = reorderRepo.save(suggestion);
        return ReorderSuggestionResponse.fromEntity(saved);
    }
}
