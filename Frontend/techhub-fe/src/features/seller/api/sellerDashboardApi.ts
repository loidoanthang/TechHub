import { apiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/constants";
import type { ApiResponse } from "@/types/api";
import type {
  SellerDashboardMetricsResponse,
  SellerDashboardParams,
} from "@/types/sellerDashboard";

export const sellerDashboardApi = {
  getMetrics: (
    params?: SellerDashboardParams
  ): Promise<ApiResponse<SellerDashboardMetricsResponse>> => {
    return apiClient.get(ENDPOINTS.SELLER.DASHBOARD_METRICS, { params });
  },
};
