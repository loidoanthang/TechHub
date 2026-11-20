import axios from "axios";
import { apiClient } from "@/lib/api/client";
import { API_ENDPOINTS } from "@/lib/constants";
import type { ApiResponse } from "@/types/api";
import type {
  PresignedUrlResponse,
  UploadFileRequest,
} from "@/features/uploads/types";
import { normalizeMimeType } from "@/features/uploads/types";

// Bare Axios instance for direct R2 PUT (avoids auth headers & signature mismatch)
const r2Client = axios.create();

async function requestPresignedUrl(
  endpoint: string,
  file: File
): Promise<PresignedUrlResponse> {
  const normalizedType = normalizeMimeType(file);
  const body: UploadFileRequest = {
    fileName: file.name,
    contentType: normalizedType,
  };

  const res = (await apiClient.post(
    endpoint,
    body
  )) as unknown as ApiResponse<PresignedUrlResponse>;

  if (!res?.success || !res.data?.uploadUrl) {
    throw new Error(res?.message || "Failed to initialize image upload");
  }

  return res.data;
}

export const uploadApi = {
  getAvatarPresignedUrl: (file: File) =>
    requestPresignedUrl(API_ENDPOINTS.UPLOAD.AVATAR, file),

  getShopAvatarPresignedUrl: (shopId: number | string, file: File) =>
    requestPresignedUrl(API_ENDPOINTS.UPLOAD.SHOP_AVATAR(shopId), file),

  getShopBannerPresignedUrl: (shopId: number | string, file: File) =>
    requestPresignedUrl(API_ENDPOINTS.UPLOAD.SHOP_BANNER(shopId), file),

  getProductImagePresignedUrl: (file: File) =>
    requestPresignedUrl(API_ENDPOINTS.UPLOAD.PRODUCTS, file),

  getReviewMediaPresignedUrl: (file: File) =>
    requestPresignedUrl(API_ENDPOINTS.UPLOAD.REVIEWS, file),

  getReturnProofPresignedUrl: (file: File) =>
    requestPresignedUrl(API_ENDPOINTS.UPLOAD.RETURNS, file),

  putBinaryToR2: async (
    uploadUrl: string,
    file: File,
    options?: {
      signal?: AbortSignal;
      onProgress?: (percent: number) => void;
    }
  ): Promise<void> => {
    const normalizedType = normalizeMimeType(file);

    await r2Client.put(uploadUrl, file, {
      headers: {
        "Content-Type": normalizedType,
      },
      signal: options?.signal,
      onUploadProgress: (progressEvent) => {
        const total = progressEvent.total ?? file.size;
        if (total && options?.onProgress) {
          const percent = Math.round(
            (progressEvent.loaded / total) * 100
          );
          options.onProgress(Math.min(100, Math.max(0, percent)));
        }
      },
    });
  },
};
