import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/constants";
import type { ApiResponse } from "@/types/api";
import type {
  CreateReviewRequest,
  UpdateReviewRequest,
  ProductReviewResponse,
} from "@/types/review";

export const buyerReviewApi = {
  createReview: async (
    data: CreateReviewRequest
  ): Promise<ApiResponse<ProductReviewResponse>> => {
    return apiClient.post(ENDPOINTS.REVIEWS.BUYER_CREATE, data);
  },

  updateReview: async (
    id: string,
    data: UpdateReviewRequest
  ): Promise<ApiResponse<ProductReviewResponse>> => {
    return apiClient.put(ENDPOINTS.REVIEWS.BUYER_UPDATE(id), data);
  },
};

export function useCreateReviewMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateReviewRequest) => buyerReviewApi.createReview(data),
    onSuccess: () => {
      // Invalidate relevant queries so orders & products update
      queryClient.invalidateQueries({ queryKey: ["buyer-orders"] });
      queryClient.invalidateQueries({ queryKey: ["buyer-order-detail"] });
      queryClient.invalidateQueries({ queryKey: ["product-reviews"] });
      queryClient.invalidateQueries({ queryKey: ["product-rating-summary"] });
    },
  });
}

export function useUpdateReviewMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateReviewRequest }) =>
      buyerReviewApi.updateReview(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["buyer-orders"] });
      queryClient.invalidateQueries({ queryKey: ["buyer-order-detail"] });
      queryClient.invalidateQueries({ queryKey: ["product-reviews"] });
      queryClient.invalidateQueries({ queryKey: ["product-rating-summary"] });
    },
  });
}
