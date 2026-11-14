"use client";

import { useEffect, useState, useRef, useCallback } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/features/auth/hooks/useAuth";
import {
  subscribeNotificationStream,
  getIsStreamConnected,
} from "../api/sseClient";
import { notificationKeys } from "../api/notificationQueries";
import type {
  NotificationResponse,
  NotificationListResponse,
  NotificationQueryParams,
} from "../types";
import type { ApiResponse } from "@/types/api";

export function useNotificationStream() {
  const { isAuthenticated, user } = useAuth();
  const queryClient = useQueryClient();
  const [isConnected, setIsConnected] = useState(getIsStreamConnected());
  const [hasNewPing, setHasNewPing] = useState(false);
  const [latestNotification, setLatestNotification] =
    useState<NotificationResponse | null>(null);
  const pingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const dismissToast = useCallback(() => {
    setLatestNotification(null);
  }, []);

  useEffect(() => {
    if (!isAuthenticated || !user) {
      setIsConnected(false);
      setHasNewPing(false);
      setLatestNotification(null);
      return;
    }

    const unsubscribe = subscribeNotificationStream({
      onConnect: () => {
        setIsConnected(true);
      },
      onNotification: (notification: NotificationResponse) => {
        // 1. Play bounce ping animation for 3 seconds
        setHasNewPing(true);
        if (pingTimeoutRef.current) {
          clearTimeout(pingTimeoutRef.current);
        }
        pingTimeoutRef.current = setTimeout(() => {
          setHasNewPing(false);
        }, 3000);

        // 2. Set active notification for in-app floating toast
        setLatestNotification(notification);

        // 3. Update TanStack Query Cache with respect to query category filters
        const queriesData = queryClient.getQueriesData<
          ApiResponse<NotificationListResponse>
        >({ queryKey: notificationKeys.all });

        queriesData.forEach(([key, old]) => {
          if (!old?.data) return;

          const params = (key?.[2] || {}) as NotificationQueryParams;

          // If this cached query is filtered to a different category or read-only items, only increment unreadCount
          if (
            (params?.type && params.type !== notification.type) ||
            params?.isRead === true
          ) {
            queryClient.setQueryData<ApiResponse<NotificationListResponse>>(
              key,
              {
                ...old,
                data: {
                  ...old.data,
                  unreadCount: old.data.unreadCount + 1,
                },
              }
            );
            return;
          }

          // Avoid duplicate notifications in array
          const exists = old.data.notifications.content.some(
            (item) => item.id === notification.id
          );
          if (exists) return;

          queryClient.setQueryData<ApiResponse<NotificationListResponse>>(key, {
            ...old,
            data: {
              ...old.data,
              unreadCount: old.data.unreadCount + 1,
              notifications: {
                ...old.data.notifications,
                content: [notification, ...old.data.notifications.content],
                totalElements: old.data.notifications.totalElements + 1,
              },
            },
          });
        });
      },
      onError: () => {
        setIsConnected(false);
      },
    });

    return () => {
      unsubscribe();
      if (pingTimeoutRef.current) {
        clearTimeout(pingTimeoutRef.current);
      }
    };
  }, [isAuthenticated, user, queryClient]);

  return { isConnected, hasNewPing, latestNotification, dismissToast };
}
