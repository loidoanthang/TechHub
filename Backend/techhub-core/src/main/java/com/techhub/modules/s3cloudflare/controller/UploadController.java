package com.techhub.modules.s3cloudflare.controller;

import com.techhub.common.ApiResponse;
import com.techhub.modules.s3cloudflare.dto.PresignedUrlResponse;
import com.techhub.modules.s3cloudflare.dto.UploadFileRequest;
import com.techhub.modules.s3cloudflare.service.S3CloudFlareR2Service;
import com.techhub.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Validated
public class UploadController {

    private final S3CloudFlareR2Service r2Service;

    public record PresignRequest(String folder, String fileName, String contentType) {}

    /**
     * 1. Upload Avatar người dùng (Gắn userId đang đăng nhập)
     */
    @PostMapping("/avatar")
    public ApiResponse<PresignedUrlResponse> generateUploadAvatar(@Valid @RequestBody UploadFileRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        PresignedUrlResponse response = r2Service.generateUploadUrl(
                "avatars",
                "user_" + currentUserId,
                request.fileName(),
                request.contentType()
        );
        return ApiResponse.success(response, "Khởi tạo link upload avatar thành công.", null);
    }

    /**
     * 2. Upload Logo/Avatar của Shop
     */
    @PostMapping("/shops/{shopId}/avatar")
    public ApiResponse<PresignedUrlResponse> generateUploadShopAvatar(
            @PathVariable Long shopId,
            @Valid @RequestBody UploadFileRequest request
    ) {
        PresignedUrlResponse response = r2Service.generateUploadUrl(
                "shops/avatars",
                "shop_" + shopId,
                request.fileName(),
                request.contentType()
        );
        return ApiResponse.success(response, "Khởi tạo link upload avatar shop thành công.", null);
    }

    /**
     * 3. Upload Banner của Shop
     */
    @PostMapping("/shops/{shopId}/banner")
    public ApiResponse<PresignedUrlResponse> generateUploadShopBanner(
            @PathVariable Long shopId,
            @Valid @RequestBody UploadFileRequest request
    ) {
        PresignedUrlResponse response = r2Service.generateUploadUrl(
                "shops/banners",
                "shop_banner_" + shopId,
                request.fileName(),
                request.contentType()
        );
        return ApiResponse.success(response, "Khởi tạo link upload banner shop thành công.", null);
    }

    /**
     * 4. Upload ảnh sản phẩm / biến thể sản phẩm
     */
    @PostMapping("/products")
    public ApiResponse<PresignedUrlResponse> generateUploadProductImage(@Valid @RequestBody UploadFileRequest request) {
        PresignedUrlResponse response = r2Service.generateUploadUrl(
                "products",
                "prod",
                request.fileName(),
                request.contentType()
        );
        return ApiResponse.success(response, "Khởi tạo link upload ảnh sản phẩm thành công.", null);
    }

    /**
     * 5. Upload hình ảnh / video đánh giá sản phẩm (Review)
     */
    @PostMapping("/reviews")
    public ApiResponse<PresignedUrlResponse> generateUploadReviewMedia(@Valid @RequestBody UploadFileRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        PresignedUrlResponse response = r2Service.generateUploadUrl(
                "reviews",
                "review_user_" + currentUserId,
                request.fileName(),
                request.contentType()
        );
        return ApiResponse.success(response, "Khởi tạo link upload media review thành công.", null);
    }

    /**
     * 6. Upload ảnh chụp bằng chứng khiếu nại / đổi trả hàng (Return Request)
     */
    @PostMapping("/returns")
    public ApiResponse<PresignedUrlResponse> generateUploadReturnProof(@Valid @RequestBody UploadFileRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        PresignedUrlResponse response = r2Service.generateUploadUrl(
                "returns",
                "return_proof_" + currentUserId,
                request.fileName(),
                request.contentType()
        );
        return ApiResponse.success(response, "Khởi tạo link upload bằng chứng đổi trả thành công.", null);
    }

    /**
     * 7. Upload ảnh gửi trong tin nhắn chat (Conversation)
     */
    @PostMapping("/chat")
    public ApiResponse<PresignedUrlResponse> generateUploadChatMedia(@Valid @RequestBody UploadFileRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        PresignedUrlResponse response = r2Service.generateUploadUrl(
                "chats",
                "chat_user_" + currentUserId,
                request.fileName(),
                request.contentType()
        );
        return ApiResponse.success(response, "Khởi tạo link upload ảnh chat thành công.", null);
    }

//    /**
//     * 8. Endpoint General (dùng chung linh hoạt nếu cần chỉ định folder)
//     */
//    @PostMapping("/presigned-url")
//    public ApiResponse<PresignedUrlResponse> getPresignedUrl(@RequestBody PresignRequest request) {
//        PresignedUrlResponse response = r2Service.generateUploadUrl(
//                request.folder(),
//                request.fileName(),
//                request.contentType()
//        );
//        return ApiResponse.success(response, "Khởi tạo link upload thành công.", null);
//    }
}
