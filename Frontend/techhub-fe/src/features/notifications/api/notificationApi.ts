import { apiClient } from "@/lib/api/client";
import { API_ENDPOINTS } from "@/lib/constants";
import { normalizeNotification } from "../utils/notificationHelpers";
import type { ApiResponse } from "@/types/api";
import type {
  NotificationListResponse,
  NotificationQueryParams,
} from "../types";

export const notificationApi = {
  getNotifications: async (
    params?: NotificationQueryParams
  ): Promise<ApiResponse<NotificationListResponse>> => {
    const res: ApiResponse<NotificationListResponse> = await apiClient.get(
      API_ENDPOINTS.NOTIFICATIONS.BASE,
      { params }
    );
    if (res?.data?.notifications?.content) {
      res.data.notifications.content = res.data.notifications.content.map(
        (item) => normalizeNotification(item as unknown as Record<string, unknown>)
      );
    }
    return res;
  },

  markAsRead: (id: string): Promise<ApiResponse<void>> => {
    return apiClient.patch(API_ENDPOINTS.NOTIFICATIONS.READ(id));
  },

  markAllAsRead: (): Promise<ApiResponse<void>> => {
    return apiClient.patch(API_ENDPOINTS.NOTIFICATIONS.READ_ALL);
  },

  deleteNotification: (id: string): Promise<ApiResponse<void>> => {
    return apiClient.delete(API_ENDPOINTS.NOTIFICATIONS.DELETE(id));
  },

  adminBroadcast: (data: {
    title: string;
    content: string;
    targetRole?: string;
  }): Promise<ApiResponse<void>> => {
    return apiClient.post(API_ENDPOINTS.ADMIN.NOTIFICATIONS_BROADCAST, data);
  },
};
