import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";
import { API_BASE_URL, API_ENDPOINTS, ROUTES } from "@/lib/constants";
import {
  getAccessToken,
  getRefreshToken,
  setTokens,
  clearTokens,
} from "@/lib/api/token";
import type { ApiResponse } from "@/types/api";
import type { AuthTokens } from "@/types/auth";

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 15000,
});

// Request Interceptor: Attach Bearer Token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getAccessToken();
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Mutex Refresh Queue for 401 Unauthorized
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}> = [];

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach((promise) => {
    if (error) {
      promise.reject(error);
    } else if (token) {
      promise.resolve(token);
    }
  });
  failedQueue = [];
};

/**
 * Centrally refreshed access & refresh tokens using mutex queue
 */
export async function refreshAuthTokens(): Promise<string | null> {
  if (isRefreshing) {
    return new Promise((resolve, reject) => {
      failedQueue.push({ resolve, reject });
    });
  }

  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    clearTokens();
    return null;
  }

  isRefreshing = true;

  try {
    const refreshResponse = await axios.post<ApiResponse<AuthTokens>>(
      `${API_BASE_URL}${API_ENDPOINTS.AUTH.REFRESH}`,
      { refreshToken },
      { headers: { "Content-Type": "application/json" } }
    );

    const newTokens = refreshResponse.data?.data;
    if (newTokens?.accessToken && newTokens?.refreshToken) {
      setTokens(newTokens.accessToken, newTokens.refreshToken);
      processQueue(null, newTokens.accessToken);
      return newTokens.accessToken;
    }

    const err = new Error("Invalid token payload");
    processQueue(err, null);
    clearTokens();
    throw err;
  } catch (refreshErr) {
    processQueue(refreshErr, null);
    clearTokens();
    throw refreshErr;
  } finally {
    isRefreshing = false;
  }
}

apiClient.interceptors.response.use(
  (response) => {
    // Backend returns ApiResponse<T>
    return response.data;
  },
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
    };

    const status = error.response?.status;
    const errorData = error.response?.data;
    const errorCode = (errorData?.metadata as { errorCode?: string })?.errorCode;
    const rawMessage = (errorData?.message || "").toLowerCase();

    // Handle account suspension / lockout
    if (
      errorCode === "ACCOUNT_SUSPENDED" ||
      errorCode === "ACCOUNT_BANNED" ||
      rawMessage.includes("suspended") ||
      rawMessage.includes("banned")
    ) {
      clearTokens();
      if (typeof window !== "undefined") {
        window.location.href = ROUTES.SUSPENDED;
      }
      return Promise.reject(error);
    }

    // Skip refresh token logic if error is not 401 or request was to auth endpoints
    const isAuthEndpoint =
      originalRequest?.url?.includes(API_ENDPOINTS.AUTH.LOGIN) ||
      originalRequest?.url?.includes(API_ENDPOINTS.AUTH.REFRESH) ||
      originalRequest?.url?.includes(API_ENDPOINTS.AUTH.REGISTER);

    if (status === 401 && originalRequest && !originalRequest._retry && !isAuthEndpoint) {
      originalRequest._retry = true;

      try {
        const newAccessToken = await refreshAuthTokens();
        if (newAccessToken) {
          if (originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
          }
          return apiClient(originalRequest);
        }
        throw error;
      } catch (refreshErr) {
        if (typeof window !== "undefined") {
          window.location.href = `${ROUTES.LOGIN}?returnUrl=${encodeURIComponent(
            window.location.pathname
          )}`;
        }
        return Promise.reject(refreshErr);
      }
    }

    return Promise.reject(error);
  }
);
