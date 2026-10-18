package com.techhub.model.dto.response;

import java.time.LocalDateTime;

public record OrderTimelineStepResponse(
        String step,
        String title,
        String description,
        boolean completed,
        boolean current,
        LocalDateTime timestamp
) {}
