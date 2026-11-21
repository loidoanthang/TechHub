import { z } from "zod";

export const rejectionReasonSchema = z.object({
  reason: z
    .string()
    .trim()
    .min(5, "Vui lòng nhập lý do từ chối chi tiết (tối thiểu 5 ký tự)")
    .max(1000, "Lý do không được vượt quá 1000 ký tự"),
});

export type RejectionReasonFormValues = z.infer<typeof rejectionReasonSchema>;

export const statusReasonSchema = z.object({
  reason: z
    .string()
    .trim()
    .min(3, "Vui lòng nhập lý do xử lý (tối thiểu 3 ký tự)")
    .max(500, "Lý do không được vượt quá 500 ký tự"),
});

export type StatusReasonFormValues = z.infer<typeof statusReasonSchema>;

export const adminCancelOrderSchema = z.object({
  reasonCode: z.enum([
    "SELLER_FORCE_MAJEURE",
    "SELLER_OUT_OF_STOCK",
    "BUYER_CHANGED_MIND",
    "BUYER_INCORRECT_ADDRESS",
    "BUYER_PAYMENT_ISSUE",
    "OTHER",
  ]),
  reasonNote: z
    .string()
    .trim()
    .max(500, "Ghi chú không được vượt quá 500 ký tự")
    .optional(),
});

export type AdminCancelOrderFormValues = z.infer<typeof adminCancelOrderSchema>;
