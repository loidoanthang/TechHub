package com.techhub.modules.s3cloudflare.dto;

public record PresignedUrlResponse(
        String uploadUrl,
        String publicUrl,
        String fileKey
) {}
