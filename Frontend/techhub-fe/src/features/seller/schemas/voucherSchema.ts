import { z } from "zod";

export const voucherFormSchema = z
  .object({
    code: z
      .string()
      .trim()
      .min(3, "Mã voucher phải từ 3 đến 20 ký tự")
      .max(20, "Mã voucher không được vượt quá 20 ký tự")
      .regex(
        /^[a-zA-Z0-9_-]+$/,
        "Mã chỉ được chứa chữ cái, số, gạch nối (-) hoặc gạch dưới (_)"
      ),
    name: z
      .string()
      .trim()
      .min(2, "Tên voucher phải có ít nhất 2 ký tự")
      .max(200, "Tên voucher không được vượt quá 200 ký tự"),
    appliesTo: z.enum(["PRODUCT", "SHIPPING_FEE"]),
    discountType: z.enum(["PERCENTAGE", "FIXED_AMOUNT"]),
    discountValue: z
      .number()
      .min(0.01, "Mức giảm phải lớn hơn 0")
      .max(999999999, "Mức giảm vượt quá giới hạn tối đa"),
    minOrderAmount: z
      .number()
      .min(0, "Giá trị đơn tối thiểu không được âm"),
    maxDiscountAmount: z
      .number()
      .min(0.01, "Mức giảm tối đa phải lớn hơn 0")
      .optional()
      .nullable(),
    usageLimit: z
      .number()
      .int("Tổng lượt dùng phải là số nguyên")
      .min(1, "Tổng lượt dùng tối thiểu là 1")
      .optional()
      .nullable(),
    usageLimitPerUser: z
      .number()
      .int("Lượt dùng/khách phải là số nguyên")
      .min(1, "Mỗi khách hàng phải được dùng ít nhất 1 lần"),
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
      message: "Mức giảm theo phần trăm không được vượt quá 100%",
      path: ["discountValue"],
    }
  )
  .refine(
    (data) => {
      if (data.usageLimit && data.usageLimit < data.usageLimitPerUser) {
        return false;
      }
      return true;
    },
    {
      message: "Tổng lượt dùng không được nhỏ hơn lượt dùng mỗi khách hàng",
      path: ["usageLimit"],
    }
  )
  .refine(
    (data) => {
      if (!data.startDate || !data.endDate) return true;
      return new Date(data.endDate) > new Date(data.startDate);
    },
    {
      message: "Thời gian kết thúc phải sau thời gian bắt đầu",
      path: ["endDate"],
    }
  );

export type VoucherFormValues = z.infer<typeof voucherFormSchema>;
