export type ToastType = "success" | "error" | "info" | "warning";

export interface ToastItem {
  id: string;
  message: string;
  type: ToastType;
}

export const toast = {
  success: (message: string) => emitToast(message, "success"),
  error: (message: string) => emitToast(message, "error"),
  info: (message: string) => emitToast(message, "info"),
  warning: (message: string) => emitToast(message, "warning"),
};

function emitToast(message: string, type: ToastType) {
  if (typeof window !== "undefined") {
    const event = new CustomEvent("techhub:toast", {
      detail: {
        id: Math.random().toString(36).substring(2, 9),
        message,
        type,
      },
    });
    window.dispatchEvent(event);
  }
}
