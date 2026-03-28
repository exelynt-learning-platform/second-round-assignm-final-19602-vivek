package com.ecommerce.repository;

import com.ecommerce.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    List<Order> findByUser_IdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByIdAndUser_Id(Long id, Long userId);

    @EntityGraph(attributePaths = {"items", "items.product"})
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);
}
