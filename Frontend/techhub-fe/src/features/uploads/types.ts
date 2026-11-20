export interface PresignedUrlResponse {
  uploadUrl: string;
  publicUrl: string;
  fileKey: string;
}

export interface UploadFileRequest {
  fileName: string;
  contentType: string;
}

export const UPLOAD_LIMITS = {
  USER_AVATAR: 2 * 1024 * 1024, // 2 MB
  SHOP_AVATAR: 2 * 1024 * 1024, // 2 MB
  SHOP_BANNER: 4 * 1024 * 1024, // 4 MB
  PRODUCT_IMAGE: 5 * 1024 * 1024, // 5 MB
  REVIEW_MEDIA: 5 * 1024 * 1024, // 5 MB (Images only for current phase)
  RETURN_PROOF: 5 * 1024 * 1024, // 5 MB
} as const;

export const ALLOWED_IMAGE_TYPES = [
  "image/jpeg",
  "image/png",
  "image/webp",
] as const;

export type UploadStatus = "idle" | "uploading" | "success" | "error";

export interface UploadQueueItem {
  id: string;
  file: File;
  previewUrl: string;
  status: UploadStatus;
  progress: number;
  publicUrl?: string;
  errorMessage?: string;
}

export interface UploadSlotBase {
  key: string;
}

export interface UploadUrlSlot extends UploadSlotBase {
  kind: "url";
  url: string;
}

export interface UploadPendingSlot extends UploadSlotBase {
  kind: "upload";
  item: UploadQueueItem;
}

export type UploadSlot = UploadUrlSlot | UploadPendingSlot;

/**
 * Normalizes file MIME type based on file extension fallback.
 * Prevents signature mismatch caused by empty file.type on mobile/Windows.
 */
export function normalizeMimeType(file: File): string {
  if (
    file.type &&
    (ALLOWED_IMAGE_TYPES as readonly string[]).includes(file.type)
  ) {
    return file.type;
  }
  const ext = file.name.split(".").pop()?.toLowerCase();
  switch (ext) {
    case "jpg":
    case "jpeg":
      return "image/jpeg";
    case "png":
      return "image/png";
    case "webp":
      return "image/webp";
    case "heic":
    case "heif":
      return "image/heic";
    default:
      return file.type || "application/octet-stream";
  }
}

/**
 * Validates an image before it is sent to the presign endpoint.
 * Returns an error message, or null when the file is acceptable.
 */
export function validateImageFile(file: File, maxSize: number): string | null {
  const normalizedType = normalizeMimeType(file);

  if (normalizedType === "image/heic" || normalizedType === "image/heif") {
    return "HEIC format is not supported. Please select JPG, PNG, or WEBP.";
  }

  if (!(ALLOWED_IMAGE_TYPES as readonly string[]).includes(normalizedType)) {
    return "Unsupported image format. Please select JPG, PNG, or WEBP.";
  }

  if (file.size > maxSize) {
    const maxMb = Math.round((maxSize / (1024 * 1024)) * 10) / 10;
    return `File size exceeds ${maxMb} MB limit.`;
  }

  return null;
}
