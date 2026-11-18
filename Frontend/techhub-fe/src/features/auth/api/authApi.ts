import { apiClient } from "@/lib/api/client";
import { API_ENDPOINTS } from "@/lib/constants";
import type { ApiResponse } from "@/types/api";
import type {
  LoginRequest,
  AuthTokens,
  RegisterRequest,
  VerifyEmailRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  ChangePasswordRequest,
  ResendVerificationRequest,
  DeleteAccountRequest,
  LogoutRequest,
} from "@/types/auth";
import type { UserProfile } from "@/types/user";

export const authApi = {
  login: async (payload: LoginRequest): Promise<ApiResponse<AuthTokens>> => {
    return apiClient.post(API_ENDPOINTS.AUTH.LOGIN, payload);
  },

  register: async (payload: RegisterRequest): Promise<ApiResponse<void>> => {
    return apiClient.post(API_ENDPOINTS.AUTH.REGISTER, payload);
  },

  verifyEmail: async (payload: VerifyEmailRequest): Promise<ApiResponse<void>> => {
    return apiClient.post(API_ENDPOINTS.AUTH.VERIFY_EMAIL, payload);
  },

  resendVerification: async (payload: ResendVerificationRequest): Promise<ApiResponse<void>> => {
    return apiClient.post(API_ENDPOINTS.AUTH.RESEND_VERIFICATION, payload);
  },

  forgotPassword: async (payload: ForgotPasswordRequest): Promise<ApiResponse<void>> => {
    return apiClient.post(API_ENDPOINTS.AUTH.FORGOT_PASSWORD, payload);
  },

  resetPassword: async (payload: ResetPasswordRequest): Promise<ApiResponse<void>> => {
    return apiClient.post(API_ENDPOINTS.AUTH.RESET_PASSWORD, payload);
  },

  changePassword: async (payload: ChangePasswordRequest): Promise<ApiResponse<void>> => {
    return apiClient.post(API_ENDPOINTS.AUTH.CHANGE_PASSWORD, payload);
  },

  logout: async (payload: LogoutRequest): Promise<ApiResponse<void>> => {
    return apiClient.post(API_ENDPOINTS.AUTH.LOGOUT, payload);
  },

  deleteAccount: async (payload: DeleteAccountRequest): Promise<ApiResponse<void>> => {
    return apiClient.post(API_ENDPOINTS.AUTH.DELETE_ACCOUNT, payload);
  },

  getProfile: async (): Promise<ApiResponse<UserProfile>> => {
    return apiClient.get(API_ENDPOINTS.USER.PROFILE);
  },
};
