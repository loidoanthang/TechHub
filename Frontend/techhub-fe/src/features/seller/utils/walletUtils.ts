import type {
  SellerBankAccountSummary,
  DateFilterPreset,
} from "../types/wallet";

/**
 * Formats a Date object to YYYY-MM-DD using local timezone (avoiding UTC shift)
 */
export function formatLocalDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

/**
 * Formats an ISO date-time string to Vietnamese locale format (DD/MM/YYYY HH:mm)
 */
export function formatTransactionDate(dateStr: string): string {
  try {
    const d = new Date(dateStr);
    return d.toLocaleString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return dateStr;
  }
}

/**
 * Checks whether bank account details are incomplete
 */
export function isBankAccountIncomplete(
  bankInfo?: SellerBankAccountSummary | null
): boolean {
  if (!bankInfo) return true;
  return (
    !bankInfo.bankName?.trim() ||
    !bankInfo.bankAccountNumber?.trim() ||
    !bankInfo.bankAccountHolder?.trim()
  );
}

/**
 * Resolves the start and end dates for a transaction query preset
 */
export function getDateRangePreset(
  preset: DateFilterPreset,
  customStart?: string,
  customEnd?: string
): { startDate?: string; endDate?: string } {
  if (preset === "ALL") return { startDate: undefined, endDate: undefined };

  const today = new Date();
  const todayStr = formatLocalDate(today);

  if (preset === "TODAY") {
    return { startDate: todayStr, endDate: todayStr };
  }

  if (preset === "7DAYS") {
    const past = new Date();
    past.setDate(today.getDate() - 7);
    return {
      startDate: formatLocalDate(past),
      endDate: todayStr,
    };
  }

  if (preset === "30DAYS") {
    const past = new Date();
    past.setDate(today.getDate() - 30);
    return {
      startDate: formatLocalDate(past),
      endDate: todayStr,
    };
  }

  if (preset === "CUSTOM") {
    if (customStart && customEnd && customStart > customEnd) {
      return { startDate: undefined, endDate: undefined };
    }
    return {
      startDate: customStart,
      endDate: customEnd,
    };
  }

  return { startDate: undefined, endDate: undefined };
}
