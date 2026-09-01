package com.stockpulse.bootstrap;

import com.stockpulse.domain.Category;
import com.stockpulse.domain.InventorySnapshot;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ProductStatus;
import com.stockpulse.domain.TriggerReason;
import com.stockpulse.repository.InventorySnapshotRepository;
import com.stockpulse.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds the Addendum A catalog (8 products). PRD-003 (T-Shirt) is already below its
 * reorder threshold for the immediate inventory-low demo path; PRD-008 (Hoodie) has
 * velocity 15 for the demand-spike demo path.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final InventorySnapshotRepository snapshotRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (productRepository.count() > 0) {
            log.info("Seed data already present ({} products), skipping", productRepository.count());
            return;
        }

        List<Product> seeded = productRepository.saveAll(List.of(
                product("SKU-ELEC-001", "Wireless Earbuds Pro", Category.ELECTRONICS, "79.99", 45, 20, 3, ProductStatus.ACTIVE),
                product("SKU-ELEC-002", "USB-C Hub 7-Port", Category.ELECTRONICS, "34.99", 120, 30, 1, ProductStatus.ACTIVE),
                product("SKU-APP-001", "Organic Cotton T-Shirt", Category.APPAREL, "24.99", 8, 15, 12, ProductStatus.PRICE_REVIEW_PENDING),
                product("SKU-APP-002", "Running Shorts — Navy", Category.APPAREL, "39.99", 55, 20, 2, ProductStatus.ACTIVE),
                product("SKU-HOME-001", "Ceramic Pour-Over Set", Category.HOME, "49.99", 22, 10, 4, ProductStatus.ACTIVE),
                product("SKU-HOME-002", "LED Desk Lamp — Dimmable", Category.HOME, "59.99", 0, 15, 0, ProductStatus.OUT_OF_STOCK),
                product("SKU-ELEC-003", "Portable Charger 20K", Category.ELECTRONICS, "44.99", 18, 25, 8, ProductStatus.ACTIVE),
                product("SKU-APP-003", "Hoodie — Heather Grey", Category.APPAREL, "54.99", 11, 12, 15, ProductStatus.ACTIVE)
        ));

        seeded.forEach(p -> snapshotRepository.save(InventorySnapshot.builder()
                .product(p)
                .stockLevel(p.getStockLevel())
                .demandVelocity(p.getDemandVelocity())
                .triggerReason(TriggerReason.INITIAL)
                .build()));

        log.info("Seeded {} products with initial inventory snapshots", seeded.size());
    }

    private Product product(String sku, String name, Category category, String price,
                            int stock, int threshold, int velocity, ProductStatus status) {
        return Product.builder()
                .sku(sku)
                .name(name)
                .category(category)
                .currentPrice(new BigDecimal(price))
                .stockLevel(stock)
                .reorderThreshold(threshold)
                .demandVelocity(velocity)
                .status(status)
                .build();
    }
}
