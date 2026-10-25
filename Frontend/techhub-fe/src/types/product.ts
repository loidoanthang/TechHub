export interface Product {
  id: string;
  title: string;
  slug?: string;
  price: number;
  originalPrice?: number;
  discountPercentage?: number;
  rating: number;
  reviewsCount: number;
  vendorName: string;
  isVerifiedVendor?: boolean;
  imageUrl: string;
  tags?: string[];
  inStock?: boolean;
}
