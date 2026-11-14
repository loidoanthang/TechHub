import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/constants";
import { isValidUuid } from "@/lib/utils";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  ProductRatingSummaryResponse,
  ProductReviewResponse,
  ProductReviewQueryParams,
  HelpfulVoteResponse,
} from "@/types/review";

export const reviewApi = {
  /**
   * Lấy tóm tắt xếp hạng sản phẩm (điểm TB, tổng review, breakdown 1-5 sao)
   */
  getProductRatingSummary: async (
    productId: string
  ): Promise<ApiResponse<ProductRatingSummaryResponse>> => {
    return apiClient.get(ENDPOINTS.REVIEWS.PRODUCT_RATING_SUMMARY(productId));
  },

  /**
   * Lấy danh sách đánh giá của sản phẩm có phân trang và bộ lọc
   */
  getProductReviews: async (
    productId: string,
    params?: ProductReviewQueryParams
  ): Promise<ApiResponse<PageResponse<ProductReviewResponse>>> => {
    return apiClient.get(ENDPOINTS.REVIEWS.PRODUCT_REVIEWS(productId), {
      params,
    });
  },

  /**
   * Toggle hữu ích / bỏ hữu ích cho 1 đánh giá
   */
  toggleHelpfulVote: async (
    reviewId: string
  ): Promise<ApiResponse<HelpfulVoteResponse>> => {
    return apiClient.post(ENDPOINTS.REVIEWS.TOGGLE_HELPFUL(reviewId));
  },
};

export function useProductRatingSummaryQuery(productId: string) {
  return useQuery({
    queryKey: ["product-rating-summary", productId],
    queryFn: () => reviewApi.getProductRatingSummary(productId),
    enabled: isValidUuid(productId),
    staleTime: 5 * 60 * 1000,
  });
}

export function useProductReviewsQuery(
  productId: string,
  params?: ProductReviewQueryParams
) {
  return useQuery({
    queryKey: ["product-reviews", productId, params],
    queryFn: () => reviewApi.getProductReviews(productId, params),
    enabled: isValidUuid(productId),
  });
}

export function useToggleHelpfulVoteMutation(productId?: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (reviewId: string) => reviewApi.toggleHelpfulVote(reviewId),
    onSuccess: () => {
      if (productId) {
        queryClient.invalidateQueries({
          queryKey: ["product-reviews", productId],
        });
      }
    },
  });
}
