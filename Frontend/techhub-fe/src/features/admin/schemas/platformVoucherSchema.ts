import { z } from "zod";

export const platformVoucherSchema = z
  .object({
    code: z
      .string()
      .trim()
      .regex(
        /^[a-zA-Z0-9_-]{3,20}$/,
        "Mã voucher gồm 3-20 ký tự chữ, số, dấu gạch ngang hoặc gạch dưới"
      ),
    name: z
      .string()
      .trim()
      .min(2, "Tên chương trình từ 2 đến 200 ký tự")
      .max(200, "Tên chương trình tối đa 200 ký tự"),
    appliesTo: z.enum(["PRODUCT", "SHIPPING_FEE"]),
    discountType: z.enum(["PERCENTAGE", "FIXED_AMOUNT"]),
    discountValue: z
      .number({ message: "Vui lòng nhập mức giảm hợp lệ" })
      .min(1, "Mức giảm phải lớn hơn 0"),
    minOrderAmount: z
      .number({ message: "Giá trị đơn tối thiểu không hợp lệ" })
      .min(0, "Đơn tối thiểu không được âm"),
    maxDiscountAmount: z.number().nullable().optional(),
    usageLimit: z.number().int().min(1, "Lượt dùng tối thiểu là 1").nullable().optional(),
    usageLimitPerUser: z
      .number({ message: "Nhập số lượt mỗi người" })
      .int()
      .min(1, "Tối thiểu 1 lượt/người"),
    startDate: z.string().min(1, "Vui lòng chọn thời gian bắt đầu"),
    endDate: z.string().min(1, "Vui lòng chọn thời gian kết thúc"),
  })
  .refine(
    (data) => {
      if (data.discountType === "PERCENTAGE" && data.discountValue > 100) {
        return false;
      }
      return true;
    },
    {
      message: "Giảm theo phần trăm không vượt quá 100%",
      path: ["discountValue"],
    }
  )
  .refine(
    (data) => {
      if (
        data.usageLimit !== null &&
        data.usageLimit !== undefined &&
        data.usageLimit < data.usageLimitPerUser
      ) {
        return false;
      }
      return true;
    },
    {
      message: "Tổng lượt dùng không được nhỏ hơn lượt dùng mỗi tài khoản",
      path: ["usageLimit"],
    }
  )
  .refine(
    (data) => {
      if (data.startDate && data.endDate) {
        return new Date(data.endDate) > new Date(data.startDate);
      }
      return true;
    },
    {
      message: "Thời gian kết thúc phải sau thời gian bắt đầu",
      path: ["endDate"],
    }
  );

export type PlatformVoucherFormValues = z.infer<typeof platformVoucherSchema>;
