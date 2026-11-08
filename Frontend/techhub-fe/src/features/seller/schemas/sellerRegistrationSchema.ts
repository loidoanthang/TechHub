import { z } from "zod";

export const sellerRegistrationSchema = z.object({
  shopName: z
    .string()
    .trim()
    .min(3, "Tên gian hàng phải từ 3 đến 100 ký tự")
    .max(100, "Tên gian hàng tối đa 100 ký tự"),
  sellerName: z
    .string()
    .trim()
    .min(3, "Định danh cửa hàng (Shop Handle) phải có ít nhất 3 ký tự")
    .max(50, "Định danh cửa hàng tối đa 50 ký tự")
    .regex(/^[a-z0-9-]+$/, "Chỉ cho phép chữ thường không dấu (a-z), chữ số (0-9) và dấu gạch nối (-)"),
  description: z
    .string()
    .trim()
    .min(10, "Mô tả cửa hàng phải có ít nhất 10 ký tự")
    .max(500, "Mô tả cửa hàng tối đa 500 ký tự"),
  contactName: z
    .string()
    .trim()
    .min(2, "Tên người phụ trách kho ít nhất 2 ký tự")
    .max(100, "Tên người phụ trách kho tối đa 100 ký tự"),
  contactPhone: z
    .string()
    .trim()
    .min(9, "Số điện thoại liên hệ phải có ít nhất 9 số")
    .max(15, "Số điện thoại không được vượt quá 15 ký tự"),
  province: z.string().min(1, "Vui lòng chọn Tỉnh / Thành phố"),
  district: z.string().min(1, "Vui lòng chọn Quận / Huyện"),
  ward: z.string().min(1, "Vui lòng chọn Phường / Xã"),
  streetAddress: z
    .string()
    .trim()
    .min(5, "Địa chỉ chi tiết kho hàng phải từ 5 ký tự trở lên")
    .max(200, "Địa chỉ chi tiết tối đa 200 ký tự"),
  shippingFee: z
    .number()
    .min(0, "Phí vận chuyển không được âm"),
  bankName: z.string().min(1, "Vui lòng chọn ngân hàng nhận thanh toán"),
  bankAccountNumber: z
    .string()
    .trim()
    .min(6, "Số tài khoản ngân hàng phải có ít nhất 6 chữ số")
    .max(30, "Số tài khoản tối đa 30 ký tự"),
  bankAccountHolder: z
    .string()
    .trim()
    .min(2, "Tên chủ tài khoản không được để trống")
    .max(100, "Tên chủ tài khoản tối đa 100 ký tự"),
  agreeTerms: z.boolean().refine((val) => val === true, {
    message: "Bạn phải đồng ý với Điều khoản Người bán của TechHub để tiếp tục",
  }),
});

export type SellerRegistrationFormValues = z.infer<typeof sellerRegistrationSchema>;
