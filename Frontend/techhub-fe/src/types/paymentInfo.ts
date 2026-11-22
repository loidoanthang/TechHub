import type { PaymentInfoStatus, PaymentType } from "./enums";

export interface PaymentInfoResponse {
  id: string;
  paymentType: PaymentType;
  bankCode: string;
  cardMask: string;
  cardType: string;
  accountHolderName?: string | null;
  status: PaymentInfoStatus;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface VNPayTokenLinkResponse {
  redirectUrl: string;
  txnRef: string;
}
