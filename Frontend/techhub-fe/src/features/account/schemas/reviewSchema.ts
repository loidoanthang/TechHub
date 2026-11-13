import { z } from "zod";

export const reviewMediaSchema = z.object({
  mediaUrl: z.string().url("Đường dẫn ảnh/video không hợp lệ").max(500, "Đường dẫn tối đa 500 ký tự"),
  mediaType: z.enum(["IMAGE", "VIDEO"]),
  displayOrder: z.number().int().min(0).max(4),
});

export const createReviewSchema = z.object({
  orderItemId: z.string().uuid("Mã sản phẩm đơn hàng không hợp lệ"),
  rating: z
    .number()
    .int()
    .min(1, "Vui lòng chọn từ 1 đến 5 sao")
    .max(5, "Số sao tối đa là 5"),
  comment: z
    .string()
    .max(1000, "Bình luận tối đa 1000 ký tự")
    .optional()
    .or(z.literal("")),
  mediaList: z.array(reviewMediaSchema).max(5, "Tối đa 5 hình ảnh/video").optional(),
});

export const updateReviewSchema = z.object({
  rating: z
    .number()
    .int()
    .min(1, "Vui lòng chọn từ 1 đến 5 sao")
    .max(5, "Số sao tối đa là 5"),
  comment: z
    .string()
    .max(1000, "Bình luận tối đa 1000 ký tự")
    .optional()
    .or(z.literal("")),
  mediaList: z.array(reviewMediaSchema).max(5, "Tối đa 5 hình ảnh/video").optional(),
});

export type CreateReviewFormValues = z.infer<typeof createReviewSchema>;
export type UpdateReviewFormValues = z.infer<typeof updateReviewSchema>;
