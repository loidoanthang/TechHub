package com.techhub.modules.s3cloudflare.dto;

import jakarta.validation.constraints.NotBlank;

public record UploadFileRequest(
        @NotBlank(message = "Tên file không được để trống")
        String fileName,

        @NotBlank(message = "Content-Type không được để trống")
        String contentType
) {}
