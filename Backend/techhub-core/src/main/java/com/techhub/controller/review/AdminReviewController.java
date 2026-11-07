package com.techhub.controller.review;

import com.techhub.common.ApiResponse;
import com.techhub.service.review.AdminReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@Validated
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteReview(@PathVariable("id") UUID id) {
        adminReviewService.deleteReview(id);
        return ApiResponse.success(null, "Review removed successfully", null);
    }
}
