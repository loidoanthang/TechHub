import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/constants";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  SellerReviewResponse,
  SellerReviewQueryParams,
  SellerResponseRequest,
} from "@/types/review";

export const sellerReviewApi = {
  /**
   * Lấy danh sách đánh giá của Shop phân trang & lọc
   */
  getShopReviews: async (
    params?: SellerReviewQueryParams
  ): Promise<ApiResponse<PageResponse<SellerReviewResponse>>> => {
    return apiClient.get(ENDPOINTS.REVIEWS.SELLER_REVIEWS, { params });
  },

  /**
   * Người bán phản hồi 1 đánh giá (chỉ được gửi 1 lần theo BR-20.25)
   */
  respondToReview: async (
    reviewId: string,
    data: SellerResponseRequest
  ): Promise<ApiResponse<SellerReviewResponse>> => {
    return apiClient.post(ENDPOINTS.REVIEWS.SELLER_RESPOND(reviewId), data);
  },
};

export function useSellerReviewsQuery(params?: SellerReviewQueryParams) {
  return useQuery({
    queryKey: ["seller-reviews", params],
    queryFn: () => sellerReviewApi.getShopReviews(params),
  });
}

export function useRespondToReviewMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      reviewId,
      data,
    }: {
      reviewId: string;
      data: SellerResponseRequest;
    }) => sellerReviewApi.respondToReview(reviewId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["seller-reviews"] });
    },
  });
}
