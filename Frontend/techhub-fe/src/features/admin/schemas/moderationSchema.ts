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
