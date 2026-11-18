import axios, { AxiosError } from "axios";
import type { ApiErrorResponse, ApiResponse } from "@/types/api";

export function isApiError(
  error: unknown
): error is AxiosError<ApiErrorResponse | ApiResponse<unknown>> {
  return axios.isAxiosError(error);
}

export function getApiErrorMessage(
  error: unknown,
  fallback = "Đã có lỗi xảy ra. Vui lòng thử lại."
): string {
  if (isApiError(error)) {
    const data = error.response?.data;
    if (data && typeof data === "object" && "message" in data && typeof data.message === "string") {
      return data.message;
    }
    if (error.message) {
      return error.message;
    }
  }

  if (error instanceof Error) {
    return error.message;
  }

  return fallback;
}
