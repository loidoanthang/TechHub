export interface ProductVariant {
  id: string;
  sku: string;
  name: string;
  attributes: Record<string, string>; // e.g. { "Color": "Carbon Black", "Switch / Edition": "Gateron Jupiter Red" }
  price: number;
  originalPrice: number;
  stock: number;
  imageUrl?: string;
  status?: "ACTIVE" | "INACTIVE";
}

export interface AttributeOptionGroup {
  name: string; // e.g. "Color", "Switch / Edition"
  values: string[]; // e.g. ["Carbon Black", "Silver Gray"]
}

export interface ProductSpec {
  label: string;
  value: string;
}

export interface MerchantInfo {
  id: string;
  name: string;
  sellerName?: string;
  avatarText: string;
  joinedText: string;
  rating: number; // e.g. 4.9 (Average rating across shop reviews in DB)
  positiveFeedback: string;
  totalProducts: number;
  verified: boolean;
  status?: "ACTIVE" | "PAUSED" | "SUSPENDED";
}

export interface CustomerReview {
  id: string;
  userName: string;
  userAvatarText: string;
  avatarColor: string;
  date: string;
  rating: number;
  comment: string;
  helpfulCount: number;
  mediaUrls?: string[];
  sellerResponse?: string;
  sellerResponseAt?: string;
}

export interface RecommendedProductItem {
  id: string;
  title: string;
  brand: string;
  price: number;
  rating: number;
  reviewsCount: number;
  imageUrl: string;
  specs: string[];
}

export interface ProductDetailData {
  id: string;
  slug?: string;
  title: string;
  brandName?: string;
  categoryName: string;
  categorySlug: string;
  flagshipBadge?: string;
  rating: number;
  reviewsCount: number;
  soldCount: number;
  images: string[];
  variants: ProductVariant[];
  attributeGroups?: AttributeOptionGroup[];
  specs: ProductSpec[];
  merchant: MerchantInfo;
  description: {
    intro: string[];
    features: string[];
    materials: string[];
    inTheBox: string[];
  };
  reviewStats: {
    average: number;
    totalReviews: number;
    breakdown: { star: number; percentage: number }[];
  };
  reviews: CustomerReview[];
  recommendations: RecommendedProductItem[];
}
