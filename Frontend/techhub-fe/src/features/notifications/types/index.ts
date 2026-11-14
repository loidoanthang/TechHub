import type { PageResponse } from "@/types/api";

export type PortalContext = "buyer" | "seller" | "admin";

export type NotificationType =
  | "ORDER"
  | "REVIEW"
  | "PROMOTION"
  | "SYSTEM"
  | "SHOP";

export type NotificationReferenceType =
  | "ORDER"
  | "PRODUCT"
  | "REVIEW"
  | "SHOP"
  | "PROMOTION";

export interface NotificationResponse {
  id: string;
  type: NotificationType;
  title: string;
  content: string;
  isRead: boolean;
  read?: boolean; // Jackson serialization fallback
  referenceType?: NotificationReferenceType | string;
  referenceId?: string;
  createdAt: string;
}

export interface NotificationListResponse {
  unreadCount: number;
  notifications: PageResponse<NotificationResponse>;
}

export interface NotificationQueryParams {
  isRead?: boolean;
  type?: NotificationType;
  page?: number;
  size?: number;
}
