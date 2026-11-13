import { z } from "zod";

export const policyUpdateSchema = z.object({
  policyKey: z.string().trim().min(1, "Key chính sách không được trống"),
  policyValue: z
    .string()
    .trim()
    .min(1, "Giá trị cấu hình không được để trống")
    .max(255, "Giá trị tối đa 255 ký tự"),
  description: z.string().max(1000).optional().or(z.literal("")),
});

export type PolicyUpdateFormValues = z.infer<typeof policyUpdateSchema>;

export const broadcastSchema = z.object({
  targetAudience: z.enum(["ALL_USERS", "ALL_BUYERS", "ALL_SELLERS"]),
  type: z.enum(["PROMOTION", "SYSTEM", "ORDER", "REVIEW", "SHOP"]),
  title: z
    .string()
    .trim()
    .min(3, "Tiêu đề thông báo tối thiểu 3 ký tự")
    .max(300, "Tiêu đề tối đa 300 ký tự"),
  content: z
    .string()
    .trim()
    .min(5, "Nội dung thông báo tối thiểu 5 ký tự")
    .max(2000, "Nội dung tối đa 2000 ký tự"),
});

export type BroadcastFormValues = z.infer<typeof broadcastSchema>;
