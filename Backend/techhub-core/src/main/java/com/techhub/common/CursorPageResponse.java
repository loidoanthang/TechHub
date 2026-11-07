package com.techhub.common;

import java.time.LocalDateTime;
import java.util.List;

public record CursorPageResponse<T>(
        List<T> content,
        LocalDateTime nextCursor,
        boolean hasMore,
        int limit
) {
}
