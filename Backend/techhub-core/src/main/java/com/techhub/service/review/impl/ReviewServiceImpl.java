package com.techhub.service.review.impl;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.review.CreateReviewRequest;
import com.techhub.model.dto.request.review.ReviewMediaRequest;
import com.techhub.model.dto.request.review.UpdateReviewRequest;
import com.techhub.model.dto.response.review.HelpfulVoteResponse;
import com.techhub.model.dto.response.review.ReviewResponse;
import com.techhub.model.entity.*;
import com.techhub.model.enums.OrderStatus;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.*;
import com.techhub.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewMediaRepository reviewMediaRepository;
    private final ReviewHelpfulRepository reviewHelpfulRepository;

    private static final int MAX_MEDIA_FILES = 5;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewResponse createReview(UUID userId, CreateReviewRequest request) {
        log.info("Creating review: userId={}, orderItemId={}, rating={}",
                userId, request.orderItemId(), request.rating());

        // 1. Validate User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Attempt to create review for non-active user: userId={}, status={}", userId, user.getStatus());
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 2. Validate OrderItem exists
        OrderItem orderItem = orderItemRepository.findWithOrderAndProductById(request.orderItemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));

        // 3. Validate Ownership (Chống IDOR)
        Order order = orderItem.getOrder();
        if (order == null || order.getUser() == null || !order.getUser().getId().equals(userId)) {
            log.warn("IDOR attempt: User {} tried to review OrderItem {} belonging to another user",
                    userId, request.orderItemId());
            throw new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND);
        }

        // 4. Validate Order Status is COMPLETED (BR-20.2)
        if (order.getStatus() != OrderStatus.COMPLETED) {
            log.warn("Order not completed for review: orderId={}, status={}",
                    order.getId(), order.getStatus());
            throw new BusinessException(ErrorCode.ORDER_NOT_COMPLETED);
        }

        // 5. Validate not already reviewed (BR-20.3)
        if (reviewRepository.existsByOrderItemId(request.orderItemId())) {
            log.warn("OrderItem already reviewed: orderItemId={}", request.orderItemId());
            throw new BusinessException(ErrorCode.ORDER_ITEM_ALREADY_REVIEWED);
        }

        // 6. Validate Media limit
        if (request.mediaList() != null && request.mediaList().size() > MAX_MEDIA_FILES) {
            log.warn("Media limit exceeded: count={}", request.mediaList().size());
            throw new BusinessException(ErrorCode.REVIEW_MEDIA_LIMIT_EXCEEDED);
        }

        // 7. Create Review entity
        String trimmedComment = (request.comment() != null && !request.comment().isBlank())
                ? request.comment().trim()
                : null;
        Review review = Review.builder()
                .orderItem(orderItem)
                .user(user)
                .rating(request.rating())
                .comment(trimmedComment)
                .build();

        Review savedReview = reviewRepository.save(review);

        // 8. Handle Media attachments
        List<ReviewMedia> savedMediaList = new ArrayList<>();
        if (request.mediaList() != null && !request.mediaList().isEmpty()) {
            int index = 0;
            for (ReviewMediaRequest mediaReq : request.mediaList()) {
                int displayOrder = mediaReq.displayOrder() != null ? mediaReq.displayOrder() : index;
                ReviewMedia media = ReviewMedia.builder()
                        .review(savedReview)
                        .mediaUrl(mediaReq.mediaUrl().trim())
                        .mediaType(mediaReq.mediaType())
                        .displayOrder(displayOrder)
                        .build();
                savedMediaList.add(media);
                index++;
            }
            savedMediaList = reviewMediaRepository.saveAll(savedMediaList);
            savedReview.setMediaList(savedMediaList);
        }

        log.info("Review created successfully: reviewId={}, orderItemId={}, userId={}",
                savedReview.getId(), request.orderItemId(), userId);

        long helpfulCount = 0;
        return ReviewResponse.from(savedReview, helpfulCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewResponse updateReview(UUID userId, UUID reviewId, UpdateReviewRequest request) {
        log.info("Updating review: userId={}, reviewId={}, rating={}",
                userId, reviewId, request.rating());

        // 1. Validate User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Attempt to update review for non-active user: userId={}, status={}", userId, user.getStatus());
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 2. Validate Review exists and fetch details via EntityGraph
        Review review = reviewRepository.findWithDetailsById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        // 3. Check soft-deleted
        if (review.getDeletedAt() != null) {
            log.warn("Review is soft-deleted: reviewId={}", reviewId);
            throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND);
        }

        // 4. Validate Ownership (Chống IDOR)
        if (review.getUser() == null || !review.getUser().getId().equals(userId)) {
            log.warn("IDOR attempt: User {} tried to update Review {} belonging to another user",
                    userId, reviewId);
            throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND);
        }

        // 5. Validate 30-Day Edit Window (BR-20.9)
        if (review.getCreatedAt() != null && Duration.between(review.getCreatedAt(), LocalDateTime.now()).toDays() > 30) {
            log.warn("Review edit window expired: reviewId={}, createdAt={}", reviewId, review.getCreatedAt());
            throw new BusinessException(ErrorCode.REVIEW_EDIT_WINDOW_EXPIRED);
        }

        // 6. Validate Media limit
        if (request.mediaList() != null && request.mediaList().size() > MAX_MEDIA_FILES) {
            log.warn("Media limit exceeded: count={}", request.mediaList().size());
            throw new BusinessException(ErrorCode.REVIEW_MEDIA_LIMIT_EXCEEDED);
        }

        // 7. Update rating & comment
        review.setRating(request.rating());
        String trimmedComment = (request.comment() != null && !request.comment().isBlank())
                ? request.comment().trim()
                : null;
        review.setComment(trimmedComment);

        // 8. Replace media attachments
        if (review.getMediaList() == null) {
            review.setMediaList(new ArrayList<>());
        } else {
            review.getMediaList().clear();
        }

        if (request.mediaList() != null && !request.mediaList().isEmpty()) {
            int index = 0;
            for (ReviewMediaRequest mediaReq : request.mediaList()) {
                int displayOrder = mediaReq.displayOrder() != null ? mediaReq.displayOrder() : index;
                ReviewMedia media = ReviewMedia.builder()
                        .review(review)
                        .mediaUrl(mediaReq.mediaUrl().trim())
                        .mediaType(mediaReq.mediaType())
                        .displayOrder(displayOrder)
                        .build();
                review.getMediaList().add(media);
                index++;
            }
        }

        Review savedReview = reviewRepository.save(review);

        log.info("Review updated successfully: reviewId={}, userId={}", reviewId, userId);

        long helpfulCount = reviewHelpfulRepository.countByReviewId(reviewId);
        return ReviewResponse.from(savedReview, helpfulCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HelpfulVoteResponse toggleHelpfulVote(UUID userId, UUID reviewId) {
        log.info("Toggling helpful vote: userId={}, reviewId={}", userId, reviewId);

        // 1. Validate User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Attempt to vote review for non-active user: userId={}, status={}", userId, user.getStatus());
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 2. Validate Review exists & not soft-deleted
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        if (review.getDeletedAt() != null) {
            log.warn("Attempt to vote soft-deleted review: reviewId={}", reviewId);
            throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND);
        }

        // 3. Prevent Self-Voting (BR-20.16)
        if (review.getUser() != null && review.getUser().getId().equals(userId)) {
            log.warn("User attempted to vote their own review: userId={}, reviewId={}", userId, reviewId);
            throw new BusinessException(ErrorCode.CANNOT_VOTE_OWN_REVIEW);
        }

        // 4. Toggle Vote
        Optional<ReviewHelpful> existingVote = reviewHelpfulRepository.findByReviewIdAndUserId(reviewId, userId);
        boolean isHelpful;

        if (existingVote.isPresent()) {
            reviewHelpfulRepository.delete(existingVote.get());
            isHelpful = false;
            log.info("Review vote removed (unmarked): reviewId={}, userId={}", reviewId, userId);
        } else {
            try {
                ReviewHelpful vote = ReviewHelpful.builder()
                        .review(review)
                        .user(user)
                        .build();
                reviewHelpfulRepository.save(vote);
                isHelpful = true;
                log.info("Review vote added (marked): reviewId={}, userId={}", reviewId, userId);
            } catch (DataIntegrityViolationException e) {
                log.warn("Concurrent duplicate vote detected: reviewId={}, userId={}", reviewId, userId);
                isHelpful = true;
            }
        }

        long helpfulCount = reviewHelpfulRepository.countByReviewId(reviewId);
        return new HelpfulVoteResponse(reviewId, isHelpful, helpfulCount);
    }
}
