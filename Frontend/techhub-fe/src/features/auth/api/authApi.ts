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
    return (await apiClient.post(API_ENDPOINTS.AUTH.LOGIN, payload)) as unknown as ApiResponse<AuthTokens>;
  },

  register: async (payload: RegisterRequest): Promise<ApiResponse<void>> => {
    return (await apiClient.post(API_ENDPOINTS.AUTH.REGISTER, payload)) as unknown as ApiResponse<void>;
  },

  verifyEmail: async (payload: VerifyEmailRequest): Promise<ApiResponse<void>> => {
    return (await apiClient.post(API_ENDPOINTS.AUTH.VERIFY_EMAIL, payload)) as unknown as ApiResponse<void>;
  },

  resendVerification: async (payload: ResendVerificationRequest): Promise<ApiResponse<void>> => {
    return (await apiClient.post(API_ENDPOINTS.AUTH.RESEND_VERIFICATION, payload)) as unknown as ApiResponse<void>;
  },

  forgotPassword: async (payload: ForgotPasswordRequest): Promise<ApiResponse<void>> => {
    return (await apiClient.post(API_ENDPOINTS.AUTH.FORGOT_PASSWORD, payload)) as unknown as ApiResponse<void>;
  },

  resetPassword: async (payload: ResetPasswordRequest): Promise<ApiResponse<void>> => {
    return (await apiClient.post(API_ENDPOINTS.AUTH.RESET_PASSWORD, payload)) as unknown as ApiResponse<void>;
  },

  changePassword: async (payload: ChangePasswordRequest): Promise<ApiResponse<void>> => {
    return (await apiClient.post(API_ENDPOINTS.AUTH.CHANGE_PASSWORD, payload)) as unknown as ApiResponse<void>;
  },

  logout: async (payload: LogoutRequest): Promise<ApiResponse<void>> => {
    return (await apiClient.post(API_ENDPOINTS.AUTH.LOGOUT, payload)) as unknown as ApiResponse<void>;
  },

  deleteAccount: async (payload: DeleteAccountRequest): Promise<ApiResponse<void>> => {
    return (await apiClient.post(API_ENDPOINTS.AUTH.DELETE_ACCOUNT, payload)) as unknown as ApiResponse<void>;
  },

  getProfile: async (): Promise<ApiResponse<UserProfile>> => {
    return (await apiClient.get(API_ENDPOINTS.USER.PROFILE)) as unknown as ApiResponse<UserProfile>;
  },
};
