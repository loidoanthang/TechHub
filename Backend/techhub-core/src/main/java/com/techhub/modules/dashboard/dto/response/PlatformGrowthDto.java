package com.techhub.modules.dashboard.dto.response;

public record PlatformGrowthDto(
        long newUsersCount,
        long newShopsCount,
        long totalActiveShops,
        long totalSuspendedShops,
        long totalPausedShops,
        long totalBannedShops
) {}
