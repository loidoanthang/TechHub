import { z } from "zod";
import { WALLET_LIMITS } from "../types/wallet";

export const createWithdrawalSchema = (availableBalance: number) =>
  z.object({
    amount: z
      .number({
        message: "Số tiền muốn rút phải là số hợp lệ",
      })
      .min(
        WALLET_LIMITS.MIN_WITHDRAWAL,
        `Số tiền rút tối thiểu là ${new Intl.NumberFormat("vi-VN").format(WALLET_LIMITS.MIN_WITHDRAWAL)} ₫ / lần`
      )
      .max(
        WALLET_LIMITS.MAX_DAILY_WITHDRAWAL,
        `Số tiền rút tối đa là ${new Intl.NumberFormat("vi-VN").format(WALLET_LIMITS.MAX_DAILY_WITHDRAWAL)} ₫ / ngày (hạn mức tối đa trong ngày)`
      )
      .refine((val) => val <= availableBalance, {
        message: "Số tiền rút vượt quá số dư khả dụng hiện tại",
      }),
  });

export type WithdrawalFormValues = z.infer<
  ReturnType<typeof createWithdrawalSchema>
>;
