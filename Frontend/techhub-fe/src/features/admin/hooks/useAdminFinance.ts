import {
  useQuery,
  useMutation,
  useQueryClient,
  keepPreviousData,
} from "@tanstack/react-query";
import { adminFinanceApi } from "../api/adminFinanceApi";
import type {
  AdminPayoutsQueryParams,
  AdminVouchersQueryParams,
  BroadcastNotificationRequest,
  CreatePlatformVoucherRequest,
  ProcessPayoutRequest,
  ShippingSimulationRequest,
  UpdatePlatformPolicyRequest,
  UpdatePlatformVoucherRequest,
  UpdatePlatformVoucherStatusRequest,
} from "@/types/adminFinance";

export const ADMIN_FINANCE_KEYS = {
  vouchers: (params?: AdminVouchersQueryParams) =>
    ["admin", "vouchers", params] as const,
  voucherDetail: (id: string) => ["admin", "vouchers", "detail", id] as const,
  payouts: (params?: AdminPayoutsQueryParams) =>
    ["admin", "payouts", params] as const,
  policies: ["admin", "policies"] as const,
};

// === VOUCHERS ===
export function useAdminVouchersQuery(
  params?: AdminVouchersQueryParams,
  enabled = true
) {
  return useQuery({
    queryKey: ADMIN_FINANCE_KEYS.vouchers(params),
    queryFn: async () => {
      const res = await adminFinanceApi.getVouchers(params);
      return res.data;
    },
    enabled,
    placeholderData: keepPreviousData,
  });
}

export function useAdminVoucherDetailQuery(id: string | null) {
  return useQuery({
    queryKey: ADMIN_FINANCE_KEYS.voucherDetail(id || ""),
    queryFn: async () => {
      const res = await adminFinanceApi.getVoucherDetail(id!);
      return res.data;
    },
    enabled: Boolean(id),
  });
}

export function useCreatePlatformVoucherMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreatePlatformVoucherRequest) =>
      adminFinanceApi.createPlatformVoucher(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin", "vouchers"] });
    },
  });
}

export function useUpdatePlatformVoucherMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: UpdatePlatformVoucherRequest;
    }) => adminFinanceApi.updatePlatformVoucher(id, data),
    onSuccess: (_res, variables) => {
      qc.invalidateQueries({ queryKey: ["admin", "vouchers"] });
      qc.invalidateQueries({
        queryKey: ADMIN_FINANCE_KEYS.voucherDetail(variables.id),
      });
    },
  });
}

export function useUpdatePlatformVoucherStatusMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: UpdatePlatformVoucherStatusRequest;
    }) => adminFinanceApi.updatePlatformVoucherStatus(id, data),
    onSuccess: (_res, variables) => {
      qc.invalidateQueries({ queryKey: ["admin", "vouchers"] });
      qc.invalidateQueries({
        queryKey: ADMIN_FINANCE_KEYS.voucherDetail(variables.id),
      });
    },
  });
}

export function useDeletePlatformVoucherMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => adminFinanceApi.deletePlatformVoucher(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin", "vouchers"] });
    },
  });
}

// === PAYOUTS ===
export function useAdminPayoutsQuery(
  params?: AdminPayoutsQueryParams,
  enabled = true
) {
  return useQuery({
    queryKey: ADMIN_FINANCE_KEYS.payouts(params),
    queryFn: async () => {
      const res = await adminFinanceApi.getPayouts(params);
      return res.data;
    },
    enabled,
    placeholderData: keepPreviousData,
  });
}

export function useProcessPayoutMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ProcessPayoutRequest }) =>
      adminFinanceApi.processPayout(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin", "payouts"] });
      qc.invalidateQueries({ queryKey: ["admin", "dashboard"] });
    },
  });
}

// === POLICIES ===
export function useAdminPoliciesQuery() {
  return useQuery({
    queryKey: ADMIN_FINANCE_KEYS.policies,
    queryFn: async () => {
      const res = await adminFinanceApi.getPolicies();
      return res.data;
    },
  });
}

export function useUpdateAdminPolicyMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdatePlatformPolicyRequest) =>
      adminFinanceApi.updatePolicy(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ADMIN_FINANCE_KEYS.policies });
    },
  });
}

// === BROADCAST NOTIFICATIONS ===
export function useBroadcastNotificationMutation() {
  return useMutation({
    mutationFn: (data: BroadcastNotificationRequest) =>
      adminFinanceApi.broadcastNotification(data),
  });
}

// === SHIPPING SIMULATION ===
export function useSimulateShippingMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data?: ShippingSimulationRequest) =>
      adminFinanceApi.simulateShippingAdvance(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin", "orders"] });
      qc.invalidateQueries({ queryKey: ["admin", "dashboard"] });
    },
  });
}
