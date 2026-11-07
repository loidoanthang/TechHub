package com.techhub.repository;

import com.techhub.model.entity.ReviewMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewMediaRepository extends JpaRepository<ReviewMedia, UUID> {

    List<ReviewMedia> findByReviewIdOrderByDisplayOrderAsc(UUID reviewId);
}
