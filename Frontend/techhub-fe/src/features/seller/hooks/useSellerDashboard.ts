import { useQuery } from "@tanstack/react-query";
import { sellerDashboardApi } from "../api/sellerDashboardApi";
import type { SellerDashboardParams } from "@/types/sellerDashboard";

export const SELLER_DASHBOARD_KEYS = {
  metrics: (params?: SellerDashboardParams) =>
    ["seller", "dashboard", "metrics", params] as const,
};

export function useSellerDashboardMetricsQuery(params?: SellerDashboardParams) {
  return useQuery({
    queryKey: SELLER_DASHBOARD_KEYS.metrics(params),
    queryFn: async () => {
      const res = await sellerDashboardApi.getMetrics(params);
      return res.data;
    },
    staleTime: 60 * 1000,
  });
}
