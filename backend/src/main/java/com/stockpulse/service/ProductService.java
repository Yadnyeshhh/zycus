package com.stockpulse.service;

import com.stockpulse.domain.Category;
import com.stockpulse.domain.InventorySnapshot;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ProductStatus;
import com.stockpulse.domain.TriggerReason;
import com.stockpulse.dto.CreateProductRequest;
import com.stockpulse.dto.ProductResponse;
import com.stockpulse.event.ProductInventoryChangedEvent;
import com.stockpulse.exception.BadRequestException;
import com.stockpulse.exception.ResourceNotFoundException;
import com.stockpulse.repository.InventorySnapshotRepository;
import com.stockpulse.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final InventorySnapshotRepository snapshotRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        if (productRepository.findBySku(request.getSku()).isPresent()) {
            throw new BadRequestException("Product with SKU '" + request.getSku() + "' already exists");
        }

        ProductStatus initialStatus = (request.getStockLevel() == 0)
                ? ProductStatus.OUT_OF_STOCK
                : ProductStatus.ACTIVE;

        Product product = Product.builder()
                .sku(request.getSku().trim())
                .name(request.getName().trim())
                .category(request.getCategory())
                .currentPrice(request.getCurrentPrice())
                .costPrice(request.getCostPrice())
                .stockLevel(request.getStockLevel())
                .reorderThreshold(request.getReorderThreshold())
                .demandVelocity(request.getDemandVelocity())
                .status(initialStatus)
                .build();

        Product saved = productRepository.save(product);

        snapshotRepository.save(InventorySnapshot.builder()
                .product(saved)
                .stockLevel(saved.getStockLevel())
                .demandVelocity(saved.getDemandVelocity())
                .triggerReason(TriggerReason.INITIAL)
                .build());

        log.info("Created product {} (ID: {})", saved.getSku(), saved.getId());
        return ProductResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts(ProductStatus status, Category category) {
        List<Product> products;
        if (status != null && category != null) {
            products = productRepository.findByStatusAndCategory(status, category);
        } else if (status != null) {
            products = productRepository.findByStatus(status);
        } else if (category != null) {
            products = productRepository.findByCategory(category);
        } else {
            products = productRepository.findAll();
        }
        return products.stream().map(ProductResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = findEntityById(id);
        return ProductResponse.fromEntity(product);
    }

    @Transactional(readOnly = true)
    public Product findEntityById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Transactional
    public ProductResponse updateStock(Long id, int newStockLevel) {
        if (newStockLevel < 0) {
            throw new BadRequestException("Stock level cannot be negative: " + newStockLevel);
        }

        Product product = findEntityById(id);
        int oldStock = product.getStockLevel();
        product.setStockLevel(newStockLevel);

        if (newStockLevel == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        } else if (product.getStatus() == ProductStatus.OUT_OF_STOCK) {
            product.setStatus(ProductStatus.ACTIVE);
        }

        Product saved = productRepository.save(product);

        TriggerReason trigger = (saved.isStockBelowReorderThreshold())
                ? TriggerReason.INVENTORY_LOW
                : TriggerReason.MANUAL;

        snapshotRepository.save(InventorySnapshot.builder()
                .product(saved)
                .stockLevel(saved.getStockLevel())
                .demandVelocity(saved.getDemandVelocity())
                .triggerReason(trigger)
                .build());

        log.info("Updated stock for product {} (ID: {}) from {} to {}", saved.getSku(), saved.getId(), oldStock, newStockLevel);

        eventPublisher.publishEvent(ProductInventoryChangedEvent.builder()
                .productId(saved.getId())
                .stockLevel(saved.getStockLevel())
                .reorderThreshold(saved.getReorderThreshold())
                .demandVelocity(saved.getDemandVelocity())
                .category(saved.getCategory())
                .triggerReason(trigger)
                .changeAmount(newStockLevel - oldStock)
                .changeType("STOCK_UPDATE")
                .build());

        return ProductResponse.fromEntity(saved);
    }

    @Transactional
    public ProductResponse recordOrder(Long id, int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("Order quantity must be at least 1");
        }

        Product product = findEntityById(id);
        int oldStock = product.getStockLevel();
        int newStock = Math.max(0, oldStock - quantity);
        product.setStockLevel(newStock);

        int oldVelocity = product.getDemandVelocity();
        int newVelocity = oldVelocity + quantity;
        product.setDemandVelocity(newVelocity);

        if (newStock == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        }

        Product saved = productRepository.save(product);

        Double peerAvg = productRepository.averageDemandVelocityExcluding(saved.getCategory(), saved.getId());
        double categoryAvg = (peerAvg != null && peerAvg > 0) ? peerAvg : 2.0;
        boolean isDemandSpike = newVelocity > (2.0 * categoryAvg) && newVelocity >= 5;
        boolean isInventoryLow = saved.isStockBelowReorderThreshold();

        TriggerReason primaryTrigger = isInventoryLow ? TriggerReason.INVENTORY_LOW
                : (isDemandSpike ? TriggerReason.DEMAND_SPIKE : TriggerReason.MANUAL);

        snapshotRepository.save(InventorySnapshot.builder()
                .product(saved)
                .stockLevel(saved.getStockLevel())
                .demandVelocity(saved.getDemandVelocity())
                .triggerReason(primaryTrigger)
                .build());

        log.info("Recorded order for product {} (ID: {}): qty={}, newStock={}, newVelocity={}, low={}, spike={}",
                saved.getSku(), saved.getId(), quantity, newStock, newVelocity, isInventoryLow, isDemandSpike);

        // Publish events for low inventory and/or demand spike
        if (isInventoryLow) {
            eventPublisher.publishEvent(ProductInventoryChangedEvent.builder()
                    .productId(saved.getId())
                    .stockLevel(saved.getStockLevel())
                    .reorderThreshold(saved.getReorderThreshold())
                    .demandVelocity(saved.getDemandVelocity())
                    .category(saved.getCategory())
                    .triggerReason(TriggerReason.INVENTORY_LOW)
                    .changeAmount(-quantity)
                    .changeType("ORDER")
                    .build());
        }

        if (isDemandSpike) {
            eventPublisher.publishEvent(ProductInventoryChangedEvent.builder()
                    .productId(saved.getId())
                    .stockLevel(saved.getStockLevel())
                    .reorderThreshold(saved.getReorderThreshold())
                    .demandVelocity(saved.getDemandVelocity())
                    .category(saved.getCategory())
                    .triggerReason(TriggerReason.DEMAND_SPIKE)
                    .changeAmount(quantity)
                    .changeType("ORDER")
                    .build());
        }

        return ProductResponse.fromEntity(saved);
    }
}
