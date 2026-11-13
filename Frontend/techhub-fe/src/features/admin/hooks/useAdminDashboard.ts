import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { adminDashboardApi } from "../api/adminDashboardApi";
import type { AdminDashboardQueryParams } from "@/types/admin";

export const ADMIN_DASHBOARD_KEYS = {
  metrics: (params?: AdminDashboardQueryParams) =>
    ["admin", "dashboard", "metrics", params] as const,
};

export function useAdminDashboardMetricsQuery(
  params?: AdminDashboardQueryParams,
  enabled = true
) {
  return useQuery({
    queryKey: ADMIN_DASHBOARD_KEYS.metrics(params),
    queryFn: async () => {
      const res = await adminDashboardApi.getMetrics(params);
      return res.data;
    },
    enabled,
    placeholderData: keepPreviousData,
  });
}
