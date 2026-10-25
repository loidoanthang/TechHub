export interface RegisterSellerRequest {
  shopName: string;
  sellerName: string; // unique slug, a-z0-9-
  description?: string;
  contactName: string;
  contactPhone: string;
  province: string;
  district: string;
  ward: string;
  streetAddress: string;
  shippingFee: number;
  bankName: string;
  bankAccountNumber: string;
  bankAccountHolder: string;
}

export type SellerVerificationStatus = "NOT_REGISTERED" | "PENDING" | "APPROVED" | "REJECTED";

export interface SellerProfileDTO {
  id: string;
  sellerName: string;
  verificationStatus: SellerVerificationStatus;
  rejectionReason?: string | null;
  bankName: string;
  bankAccountNumber: string;
  bankAccountHolder: string;
  shop: {
    id: string;
    name: string;
    description?: string;
    status: "ACTIVE" | "PAUSED" | "SUSPENDED" | "BANNED";
  };
  warehouseAddress: {
    recipientName: string;
    phone: string;
    province: string;
    district: string;
    ward: string;
    streetAddress: string;
  };
  shippingOption: {
    name: string;
    shippingFee: number;
    isActive: boolean;
  };
}

export const VIETNAMESE_BANKS = [
  { code: "VCB", name: "Vietcombank (Ngân hàng Ngoại thương Việt Nam)" },
  { code: "MB", name: "MB Bank (Ngân hàng Quân đội)" },
  { code: "TCB", name: "Techcombank (Ngân hàng Kỹ thương Việt Nam)" },
  { code: "ACB", name: "ACB (Ngân hàng Á Châu)" },
  { code: "VPB", name: "VPBank (Ngân hàng Việt Nam Thịnh Vượng)" },
  { code: "BIDV", name: "BIDV (Ngân hàng Đầu tư & Phát triển VN)" },
  { code: "CTG", name: "VietinBank (Ngân hàng Công thương Việt Nam)" },
  { code: "TPB", name: "TPBank (Ngân hàng Tiên Phong)" },
  { code: "STB", name: "Sacombank (Ngân hàng Sài Gòn Thương Tín)" },
];

export const MOCK_CURRENT_SELLER_STATUS: {
  hasApplied: boolean;
  status: SellerVerificationStatus;
  profile?: SellerProfileDTO;
} = {
  hasApplied: false,
  status: "NOT_REGISTERED",
};
