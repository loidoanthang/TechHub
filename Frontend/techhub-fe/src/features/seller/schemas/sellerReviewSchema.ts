import { z } from "zod";

export const sellerResponseSchema = z.object({
  response: z
    .string()
    .trim()
    .min(1, "Nội dung phản hồi không được để trống")
    .max(1000, "Nội dung phản hồi tối đa 1000 ký tự"),
});

export type SellerResponseFormValues = z.infer<typeof sellerResponseSchema>;
