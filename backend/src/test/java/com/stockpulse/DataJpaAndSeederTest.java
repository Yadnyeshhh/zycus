package com.stockpulse;

import com.stockpulse.domain.Category;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ProductStatus;
import com.stockpulse.repository.InventorySnapshotRepository;
import com.stockpulse.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DataJpaAndSeederTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventorySnapshotRepository snapshotRepository;

    @Test
    void testContextLoadsAndDataSeeded() {
        List<Product> products = productRepository.findAll();
        assertThat(products).hasSize(8);

        Product prd003 = productRepository.findBySku("SKU-APP-001").orElseThrow();
        assertThat(prd003.getName()).isEqualTo("Organic Cotton T-Shirt");
        assertThat(prd003.getStockLevel()).isEqualTo(8);
        assertThat(prd003.getReorderThreshold()).isEqualTo(15);
        assertThat(prd003.isStockBelowReorderThreshold()).isTrue();

        Double elecAvg = productRepository.averageDemandVelocity(Category.ELECTRONICS);
        assertThat(elecAvg).isNotNull();

        assertThat(snapshotRepository.count()).isEqualTo(8);
    }
}
