package com.farmconnect.repository;

import com.farmconnect.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
    boolean existsByOrderIdAndSellerId(Long orderId, Long sellerId);
}
