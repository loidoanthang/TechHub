import { apiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/constants";
import type { ApiResponse } from "@/types/api";
import type {
  AdminDashboardMetricsResponse,
  AdminDashboardQueryParams,
} from "@/types/admin";

export const adminDashboardApi = {
  getMetrics: (
    params?: AdminDashboardQueryParams
  ): Promise<ApiResponse<AdminDashboardMetricsResponse>> => {
    const cleanParams: Record<string, string> = {};
    if (params?.period) cleanParams.period = params.period;
    if (params?.startDate) cleanParams.startDate = params.startDate;
    if (params?.endDate) cleanParams.endDate = params.endDate;

    return apiClient.get(ENDPOINTS.ADMIN.DASHBOARD_METRICS, {
      params: cleanParams,
    });
  },
};
