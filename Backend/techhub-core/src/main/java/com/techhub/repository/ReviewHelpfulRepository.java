package com.techhub.repository;

import com.techhub.model.entity.ReviewHelpful;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewHelpfulRepository extends JpaRepository<ReviewHelpful, UUID> {

    long countByReviewId(UUID reviewId);

    boolean existsByReviewIdAndUserId(UUID reviewId, UUID userId);

    Optional<ReviewHelpful> findByReviewIdAndUserId(UUID reviewId, UUID userId);

    @Query("SELECT r.review.id, count(r) FROM ReviewHelpful r WHERE r.review.id IN :reviewIds GROUP BY r.review.id")
    List<Object[]> countByReviewIds(@Param("reviewIds") List<UUID> reviewIds);

    @Query("SELECT r.review.id FROM ReviewHelpful r WHERE r.user.id = :userId AND r.review.id IN :reviewIds")
    List<UUID> findVotedReviewIdsByUserIdAndReviewIds(@Param("userId") UUID userId, @Param("reviewIds") List<UUID> reviewIds);
}
