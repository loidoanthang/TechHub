import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { accountApi } from "../api/accountApi";
import type { UpdateProfileRequest } from "@/types/user";
import type { CreateAddressRequest, UpdateAddressRequest } from "@/types/address";

export const ACCOUNT_KEYS = {
  profile: ["account", "profile"] as const,
  addresses: ["account", "addresses"] as const,
};

export function useProfileQuery() {
  return useQuery({
    queryKey: ACCOUNT_KEYS.profile,
    queryFn: async () => {
      const res = await accountApi.getProfile();
      return res.data;
    },
  });
}

export function useUpdateProfileMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateProfileRequest) => accountApi.updateProfile(data),
    onSuccess: (res) => {
      queryClient.setQueryData(ACCOUNT_KEYS.profile, res.data);
      queryClient.invalidateQueries({ queryKey: ACCOUNT_KEYS.profile });
    },
  });
}

export function useAddressesQuery() {
  return useQuery({
    queryKey: ACCOUNT_KEYS.addresses,
    queryFn: async () => {
      const res = await accountApi.getAddresses();
      return res.data;
    },
  });
}

export function useCreateAddressMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateAddressRequest) => accountApi.createAddress(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ACCOUNT_KEYS.addresses });
    },
  });
}

export function useUpdateAddressMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateAddressRequest }) =>
      accountApi.updateAddress(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ACCOUNT_KEYS.addresses });
    },
  });
}

export function useDeleteAddressMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => accountApi.deleteAddress(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ACCOUNT_KEYS.addresses });
    },
  });
}

export function useSetDefaultAddressMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => accountApi.setDefaultAddress(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ACCOUNT_KEYS.addresses });
    },
  });
}
