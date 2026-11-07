package com.techhub.model.dto.response.dashboard;

public record PlatformGrowthDto(
        long newUsersCount,
        long newShopsCount,
        long totalActiveShops,
        long totalSuspendedShops,
        long totalPausedShops,
        long totalBannedShops
) {}
