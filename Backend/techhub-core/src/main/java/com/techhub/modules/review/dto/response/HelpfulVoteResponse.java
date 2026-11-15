package com.techhub.modules.review.dto.response;

import java.util.UUID;

public record HelpfulVoteResponse(
        UUID reviewId,
        boolean isHelpful,
        long helpfulCount
) {}
