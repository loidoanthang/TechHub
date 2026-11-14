"use client";

import React, { createContext, useContext } from "react";
import { useNotificationStream } from "@/features/notifications/hooks/useNotificationStream";
import { NotificationToast } from "@/features/notifications/components/NotificationToast";
import type { NotificationResponse } from "@/features/notifications/types";

interface NotificationContextValue {
  isConnected: boolean;
  hasNewPing: boolean;
  activeToast: NotificationResponse | null;
  dismissToast: () => void;
}

const NotificationContext = createContext<NotificationContextValue>({
  isConnected: false,
  hasNewPing: false,
  activeToast: null,
  dismissToast: () => {},
});

export function NotificationProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const { isConnected, hasNewPing, latestNotification, dismissToast } =
    useNotificationStream();

  return (
    <NotificationContext.Provider
      value={{
        isConnected,
        hasNewPing,
        activeToast: latestNotification,
        dismissToast,
      }}
    >
      {children}
      <NotificationToast />
    </NotificationContext.Provider>
  );
}

export function useNotificationContext() {
  return useContext(NotificationContext);
}
