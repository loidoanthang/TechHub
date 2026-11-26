import { z } from "zod";

// Vietnamese mobile phone regex (10 digits: 0 / +84)
export const VN_PHONE_REGEX = /^(0|\+84)[35789][0-9]{8}$/;

export const profileSchema = z.object({
  firstName: z
    .string()
    .trim()
    .min(1, "Vui lòng nhập họ")
    .max(100, "Họ không được vượt quá 100 ký tự"),

  lastName: z
    .string()
    .trim()
    .min(1, "Vui lòng nhập tên")
    .max(100, "Tên không được vượt quá 100 ký tự"),

  phone: z
    .string()
    .trim()
    .min(1, "Vui lòng nhập số điện thoại")
    .refine(
      (val) => {
        const cleaned = val.replace(/[\s\-\(\)\.]/g, "");
        return VN_PHONE_REGEX.test(cleaned);
      },
      {
        message:
          "Số điện thoại Việt Nam không hợp lệ (VD: 0912345678 hoặc +84912345678)",
      }
    ),

  avatarUrl: z
    .string()
    .max(500, "URL ảnh đại diện quá dài")
    .nullable()
    .optional(),
});

export type ProfileFormValues = z.infer<typeof profileSchema>;
