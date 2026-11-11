import { z } from "zod";

export const shopProfileSchema = z.object({
  name: z
    .string()
    .trim()
    .min(2, "Tên gian hàng phải có ít nhất 2 ký tự")
    .max(200, "Tên gian hàng không được vượt quá 200 ký tự"),
  description: z
    .string()
    .max(1000, "Mô tả không được vượt quá 1000 ký tự")
    .optional()
    .or(z.literal("")),
  avatarUrl: z
    .string()
    .max(500, "Đường dẫn ảnh đại diện không quá 500 ký tự")
    .regex(/^(https?:\/\/.*)?$/, "Đường dẫn ảnh đại diện phải bắt đầu bằng http:// hoặc https://")
    .optional()
    .or(z.literal("")),
  bannerUrl: z
    .string()
    .max(500, "Đường dẫn ảnh bìa không quá 500 ký tự")
    .regex(/^(https?:\/\/.*)?$/, "Đường dẫn ảnh bìa phải bắt đầu bằng http:// hoặc https://")
    .optional()
    .or(z.literal("")),
});

export type ShopProfileFormValues = z.infer<typeof shopProfileSchema>;

export const warehouseAddressSchema = z.object({
  contactName: z
    .string()
    .trim()
    .min(2, "Tên người liên hệ phải có ít nhất 2 ký tự")
    .max(150, "Tên người liên hệ không được vượt quá 150 ký tự"),
  phone: z
    .string()
    .trim()
    .regex(
      /^(0|\+84)[0-9]{9}$/,
      "Số điện thoại phải là số điện thoại Việt Nam hợp lệ (10 chữ số bắt đầu bằng 0 hoặc +84)"
    ),
  province: z
    .string()
    .trim()
    .min(1, "Vui lòng chọn hoặc nhập Tỉnh / Thành phố")
    .max(100, "Tên Tỉnh/Thành phố không quá 100 ký tự"),
  district: z
    .string()
    .trim()
    .min(1, "Vui lòng nhập Quận / Huyện")
    .max(100, "Tên Quận/Huyện không quá 100 ký tự"),
  ward: z
    .string()
    .trim()
    .min(1, "Vui lòng nhập Phường / Xã")
    .max(100, "Tên Phường/Xã không quá 100 ký tự"),
  streetAddress: z
    .string()
    .trim()
    .min(3, "Địa chỉ chi tiết (số nhà, tên đường) phải có ít nhất 3 ký tự")
    .max(500, "Địa chỉ chi tiết không được vượt quá 500 ký tự"),
});

export type WarehouseAddressFormValues = z.infer<typeof warehouseAddressSchema>;

export const shippingOptionSchema = z.object({
  shippingFee: z
    .number({
      message: "Vui lòng nhập mức cước phí hợp lệ",
    })
    .min(0, "Phí vận chuyển không được âm")
    .max(10000000, "Phí vận chuyển không được vượt quá 10,000,000đ"),
});

export type ShippingOptionFormValues = z.infer<typeof shippingOptionSchema>;

export const payoutProfileSchema = z.object({
  sellerName: z
    .string()
    .trim()
    .min(3, "Tên định danh phải có ít nhất 3 ký tự")
    .max(50, "Tên định danh không được vượt quá 50 ký tự")
    .regex(
      /^[a-z0-9-]+$/,
      "Tên định danh chỉ được chứa chữ thường không dấu, số và dấu gạch ngang (ví dụ: apple-store)"
    ),
  bankName: z
    .string()
    .trim()
    .min(2, "Tên ngân hàng phải có ít nhất 2 ký tự")
    .max(100, "Tên ngân hàng không được vượt quá 100 ký tự"),
  bankAccountNumber: z
    .string()
    .trim()
    .min(5, "Số tài khoản phải có ít nhất 5 ký tự")
    .max(50, "Số tài khoản không được vượt quá 50 ký tự")
    .regex(
      /^[0-9A-Za-z]+$/,
      "Số tài khoản chỉ được chứa chữ và số, không chứa khoảng trắng"
    ),
  bankAccountHolder: z
    .string()
    .trim()
    .min(2, "Tên chủ tài khoản phải có ít nhất 2 ký tự")
    .max(150, "Tên chủ tài khoản không được vượt quá 150 ký tự"),
});

export type PayoutProfileFormValues = z.infer<typeof payoutProfileSchema>;
