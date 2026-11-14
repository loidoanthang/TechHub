"use client";

import { useQuery, useMutation, useQueryClient, type QueryKey } from "@tanstack/react-query";
import { notificationApi } from "./notificationApi";
import type {
  NotificationQueryParams,
  NotificationListResponse,
} from "../types";
import type { ApiResponse } from "@/types/api";

export const notificationKeys = {
  all: ["notifications"] as const,
  lists: () => [...notificationKeys.all, "list"] as const,
  list: (params: NotificationQueryParams) =>
    [...notificationKeys.lists(), params] as const,
};

export function useNotificationsQuery(
  params: NotificationQueryParams = {},
  options?: { enabled?: boolean }
) {
  return useQuery({
    queryKey: notificationKeys.list(params),
    queryFn: () => notificationApi.getNotifications(params),
    enabled: options?.enabled !== undefined ? options.enabled : true,
    staleTime: 30 * 1000,
    gcTime: 5 * 60 * 1000,
    refetchOnWindowFocus: false,
  });
}

/**
 * Reusable hook to fetch and format the unread notification badge count.
 */
export function useUnreadNotificationCount(options?: { enabled?: boolean }) {
  const { data } = useNotificationsQuery({ page: 0, size: 1 }, options);
  const unreadCount = data?.data?.unreadCount || 0;
  return {
    unreadCount,
    badgeText: unreadCount > 99 ? "99+" : String(unreadCount),
    hasUnread: unreadCount > 0,
  };
}

/**
 * Orchestrates dual queries for SYSTEM & SHOP events (aggregated under the "Hệ thống" category per spec line 162).
 * Implements deterministic cumulative slicing so pagination on page > 0 preserves chronological ordering
 * without dropping boundary records.
 */
export function useSystemCategoryNotificationsQuery(
  options: {
    page?: number;
    size?: number;
    isRead?: boolean;
    enabled?: boolean;
  } = {}
) {
  const { page = 0, size = 15, isRead, enabled = true } = options;

  // Cumulative fetch limit to guarantee deterministic merge across multi-stream pages
  const cumulativeSize = (page + 1) * size;

  const { data: systemRes, isLoading: isSystemLoading } = useNotificationsQuery(
    { type: "SYSTEM", isRead, page: 0, size: cumulativeSize },
    { enabled }
  );

  const { data: shopRes, isLoading: isShopLoading } = useNotificationsQuery(
    { type: "SHOP", isRead, page: 0, size: cumulativeSize },
    { enabled }
  );

  const allMerged = [
    ...(systemRes?.data?.notifications?.content || []),
    ...(shopRes?.data?.notifications?.content || []),
  ].sort(
    (a, b) =>
      new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  );

  const startIndex = page * size;
  const notifications = allMerged.slice(startIndex, startIndex + size);

  const totalElements =
    (systemRes?.data?.notifications?.totalElements || 0) +
    (shopRes?.data?.notifications?.totalElements || 0);

  const totalPages = Math.ceil(totalElements / size);
  const unreadCount = systemRes?.data?.unreadCount || 0;
  const isLoading = isSystemLoading || isShopLoading;

  return {
    notifications,
    totalElements,
    totalPages,
    unreadCount,
    isLoading,
  };
}

/**
 * Helper to inspect cache queries for unread status.
 */
function isItemUnreadInCache(
  previousData:
    | [QueryKey, ApiResponse<NotificationListResponse> | undefined][]
    | undefined,
  id: string
): boolean {
  if (!previousData) return false;
  for (const [, cache] of previousData) {
    const found = cache?.data?.notifications?.content?.find(
      (item) => item.id === id
    );
    if (found) return !found.isRead;
  }
  return false;
}

/**
 * Reusable optimistic mutation helper across notification mutations.
 * Cancels in-flight queries, applies an updater to all matching caches,
 * rollbacks on error, and settles with query invalidation.
 */
function useOptimisticNotificationMutation<TVariables>(
  mutationFn: (variables: TVariables) => Promise<ApiResponse<void>>,
  updateCache: (
    oldData: NotificationListResponse,
    variables: TVariables,
    previousData?: [QueryKey, ApiResponse<NotificationListResponse> | undefined][]
  ) => NotificationListResponse
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn,
    onMutate: async (variables: TVariables) => {
      await queryClient.cancelQueries({ queryKey: notificationKeys.all });

      const queryFilter = { queryKey: notificationKeys.all };
      const previousData = queryClient.getQueriesData<
        ApiResponse<NotificationListResponse>
      >(queryFilter);

      queryClient.setQueriesData<ApiResponse<NotificationListResponse>>(
        queryFilter,
        (old) => {
          if (!old?.data) return old;
          return {
            ...old,
            data: updateCache(old.data, variables, previousData),
          };
        }
      );

      return { previousData };
    },
    onError: (_err, _vars, context) => {
      if (context?.previousData) {
        context.previousData.forEach(([key, data]) => {
          queryClient.setQueryData(key, data);
        });
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

export function useMarkAsReadMutation() {
  return useOptimisticNotificationMutation(
    (id: string) => notificationApi.markAsRead(id),
    (oldData, id, previousData) => {
      const itemWasUnread = isItemUnreadInCache(previousData, id);

      const updatedContent = oldData.notifications.content.map((item) => {
        if (item.id === id) {
          return { ...item, isRead: true, read: true };
        }
        return item;
      });

      return {
        ...oldData,
        unreadCount: itemWasUnread
          ? Math.max(0, oldData.unreadCount - 1)
          : oldData.unreadCount,
        notifications: {
          ...oldData.notifications,
          content: updatedContent,
        },
      };
    }
  );
}

export function useMarkAllAsReadMutation() {
  return useOptimisticNotificationMutation(
    () => notificationApi.markAllAsRead(),
    (oldData) => ({
      ...oldData,
      unreadCount: 0,
      notifications: {
        ...oldData.notifications,
        content: oldData.notifications.content.map((item) => ({
          ...item,
          isRead: true,
          read: true,
        })),
      },
    })
  );
}

export function useDeleteNotificationMutation() {
  return useOptimisticNotificationMutation(
    (params: { id: string; wasUnread?: boolean } | string) => {
      const id = typeof params === "string" ? params : params.id;
      return notificationApi.deleteNotification(id);
    },
    (oldData, params, previousData) => {
      const id = typeof params === "string" ? params : params.id;
      const removedItemWasUnread =
        typeof params === "object" && params.wasUnread !== undefined
          ? params.wasUnread
          : isItemUnreadInCache(previousData, id);

      const updatedContent = oldData.notifications.content.filter(
        (item) => item.id !== id
      );

      return {
        ...oldData,
        unreadCount: removedItemWasUnread
          ? Math.max(0, oldData.unreadCount - 1)
          : oldData.unreadCount,
        notifications: {
          ...oldData.notifications,
          content: updatedContent,
          totalElements: Math.max(0, oldData.notifications.totalElements - 1),
        },
      };
    }
  );
}
