import { z } from "zod";

// Regex kiểm tra số điện thoại Việt Nam khớp với Backend Spring Boot:
// Bắt đầu bằng 0 hoặc +84, tiếp theo là 3, 5, 7, 8, 9, và đúng 8 chữ số tiếp theo
export const VN_PHONE_REGEX = /^(0|\+84)[35789][0-9]{8}$/;

export const addressSchema = z.object({
  recipientName: z
    .string()
    .trim()
    .min(1, "Vui lòng nhập họ và tên người nhận")
    .max(150, "Họ và tên người nhận không được vượt quá 150 ký tự"),

  phone: z
    .string()
    .trim()
    .min(1, "Vui lòng nhập số điện thoại liên hệ")
    .refine(
      (val) => {
        const cleaned = val.replace(/[\s\-\(\)\.]/g, "");
        return VN_PHONE_REGEX.test(cleaned);
      },
      {
        message: "Số điện thoại di động không hợp lệ (Ví dụ: 0912345678 hoặc +84912345678)",
      }
    ),

  province: z
    .string()
    .trim()
    .min(1, "Vui lòng nhập hoặc chọn Tỉnh / Thành phố")
    .max(100, "Tỉnh / Thành phố không được vượt quá 100 ký tự"),

  district: z
    .string()
    .trim()
    .min(1, "Vui lòng nhập Quận / Huyện")
    .max(100, "Quận / Huyện không được vượt quá 100 ký tự"),

  ward: z
    .string()
    .trim()
    .min(1, "Vui lòng nhập Phường / Xã")
    .max(100, "Phường / Xã không được vượt quá 100 ký tự"),

  streetAddress: z
    .string()
    .trim()
    .min(1, "Vui lòng nhập địa chỉ cụ thể (số nhà, tên đường, tòa nhà...)")
    .max(500, "Địa chỉ cụ thể không được vượt quá 500 ký tự"),

  label: z.enum(["HOME", "OFFICE", "OTHER"]),

  isDefault: z.boolean(),
});

export type AddressFormData = z.infer<typeof addressSchema>;
