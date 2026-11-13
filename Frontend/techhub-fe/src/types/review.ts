export type ReviewMediaType = "IMAGE" | "VIDEO";

export interface ReviewMediaResponse {
  id: string;
  mediaUrl: string;
  mediaType: ReviewMediaType;
  displayOrder: number;
}

export interface ReviewMediaRequest {
  mediaUrl: string;
  mediaType: ReviewMediaType;
  displayOrder: number;
}

export interface ProductReviewResponse {
  id: string;
  orderItemId: string;
  rating: number;
  comment: string | null;
  variantName: string | null;
  reviewerName: string;
  reviewerAvatar: string | null;
  mediaList: ReviewMediaResponse[];
  sellerResponse: string | null;
  sellerResponseAt: string | null;
  helpfulCount: number;
  hasVotedHelpful: boolean;
  createdAt: string;
}

export interface ProductRatingSummaryResponse {
  productId: string;
  averageRating: number;
  totalReviews: number;
  mediaReviewCount: number;
  ratingCounts: Record<number, number>;
}

export interface CreateReviewRequest {
  orderItemId: string;
  rating: number;
  comment?: string;
  mediaList?: ReviewMediaRequest[];
}

export interface UpdateReviewRequest {
  rating: number;
  comment?: string;
  mediaList?: ReviewMediaRequest[];
}

export interface HelpfulVoteResponse {
  reviewId: string;
  isHelpful: boolean;
  helpfulCount: number;
}

export interface SellerReviewResponse {
  id: string;
  orderItemId: string;
  orderId: string | null;
  orderNumber: string | null;
  productId: string | null;
  productName: string | null;
  variantName: string | null;
  sku: string | null;
  imageUrl: string | null;
  rating: number;
  comment: string | null;
  mediaList: ReviewMediaResponse[];
  reviewerName: string;
  reviewerAvatar: string | null;
  sellerResponse: string | null;
  sellerResponseAt: string | null;
  helpfulCount: number;
  createdAt: string;
}

export interface SellerResponseRequest {
  response: string;
}

export interface ProductReviewQueryParams {
  rating?: number;
  hasMedia?: boolean;
  sortBy?: "createdAt" | "rating";
  sortDirection?: "ASC" | "DESC";
  page?: number;
  size?: number;
}

export interface SellerReviewQueryParams {
  rating?: number;
  hasResponded?: boolean;
  hasMedia?: boolean;
  productId?: string;
  keyword?: string;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
  sortBy?: "createdAt" | "rating";
  sortDirection?: "ASC" | "DESC";
}
