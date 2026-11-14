import type { OrderStatus } from "@/types/enums";
import type { OrderTimelineStepResponse } from "@/types/order";

export const STATUS_PROGRESS_MAP: Record<OrderStatus, number> = {
  PENDING_PAYMENT: 1,
  PENDING_CONFIRMATION: 1,
  CONFIRMED: 2,
  PROCESSING: 2,
  PREPARING: 3,
  WAITING_FOR_PICKUP: 3,
  SHIPPING: 4,
  SHIPPED: 4,
  DELIVERED: 5,
  RETURN_REQUESTED: 5,
  COMPLETED: 6,
  CANCELLED: 1,
  RETURNED: 6,
  REFUNDED: 6,
};

export function isTerminalOrderStatus(status: OrderStatus): boolean {
  return (
    status === "COMPLETED" ||
    status === "CANCELLED" ||
    status === "RETURNED" ||
    status === "REFUNDED"
  );
}

export interface NormalizedTimelineStep {
  stepNum: number;
  title: string;
  description: string;
  timestamp?: string | null;
  isCompleted: boolean;
  isCurrent: boolean;
  isLast: boolean;
  isNextDone: boolean;
}

export function buildNormalizedTimeline(
  timeline: OrderTimelineStepResponse[] | undefined,
  orderStatus: OrderStatus
): NormalizedTimelineStep[] {
  const isTerminal = isTerminalOrderStatus(orderStatus);
  const currentOrderStep = STATUS_PROGRESS_MAP[orderStatus] ?? 1;

  const rawSteps = (timeline || []).map((step, idx) => {
    const stepNum = idx + 1;
    const rawCompleted = step.completed;
    const rawCurrent = step.current;

    const isCompleted =
      typeof rawCompleted === "boolean"
        ? rawCompleted
        : stepNum < currentOrderStep || orderStatus === "COMPLETED";

    const isCurrent =
      !isTerminal &&
      (typeof rawCurrent === "boolean"
        ? rawCurrent
        : stepNum === currentOrderStep);

    return {
      stepNum,
      title: step.title,
      description: step.description,
      timestamp: step.timestamp,
      isCompleted,
      isCurrent,
    };
  });

  return rawSteps.map((step, idx) => {
    const isLast = idx === rawSteps.length - 1;
    const nextStep = rawSteps[idx + 1];
    const isNextDone = nextStep ? nextStep.isCompleted || nextStep.isCurrent : false;

    return {
      ...step,
      isLast,
      isNextDone,
    };
  });
}
