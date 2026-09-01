package com.stockpulse.repository;

import com.stockpulse.domain.Category;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    List<Product> findByStatus(ProductStatus status);

    List<Product> findByCategory(Category category);

    List<Product> findByStatusAndCategory(ProductStatus status, Category category);

    /** Category average demand velocity, used by spike detection and rule-based pricing. */
    @Query("select avg(p.demandVelocity) from Product p where p.category = :category")
    Double averageDemandVelocity(@Param("category") Category category);

    @Query("select avg(p.demandVelocity) from Product p where p.category = :category and p.id <> :productId")
    Double averageDemandVelocityExcluding(@Param("category") Category category, @Param("productId") Long productId);
}
