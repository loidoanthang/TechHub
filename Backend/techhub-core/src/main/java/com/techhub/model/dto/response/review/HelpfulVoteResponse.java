package com.techhub.model.dto.response.review;

import java.util.UUID;

public record HelpfulVoteResponse(
        UUID reviewId,
        boolean isHelpful,
        long helpfulCount
) {}
