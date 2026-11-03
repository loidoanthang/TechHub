import { z } from "zod";

export const passwordSchema = z
  .string()
  .min(8, "Mật khẩu phải có ít nhất 8 ký tự")
  .max(100, "Mật khẩu không được vượt quá 100 ký tự")
  .regex(/[A-Z]/, "Phải chứa ít nhất 1 chữ cái in hoa")
  .regex(/[a-z]/, "Phải chứa ít nhất 1 chữ cái in thường")
  .regex(/[0-9]/, "Phải chứa ít nhất 1 chữ số")
  .regex(/[^A-Za-z0-9]/, "Phải chứa ít nhất 1 ký tự đặc biệt (!@#$%...)");

export const loginSchema = z.object({
  email: z
    .string()
    .trim()
    .toLowerCase()
    .min(1, "Vui lòng nhập địa chỉ email")
    .email("Định dạng email không hợp lệ"),
  password: z
    .string()
    .min(1, "Vui lòng nhập mật khẩu"),
  rememberMe: z.boolean(),
});

export type LoginFormData = z.infer<typeof loginSchema>;

export const registerSchema = z
  .object({
    firstName: z
      .string()
      .trim()
      .min(1, "Vui lòng nhập họ và tên đệm")
      .max(100, "Họ không vượt quá 100 ký tự"),
    lastName: z
      .string()
      .trim()
      .min(1, "Vui lòng nhập tên")
      .max(100, "Tên không vượt quá 100 ký tự"),
    email: z
      .string()
      .trim()
      .toLowerCase()
      .min(1, "Vui lòng nhập email")
      .email("Định dạng email không hợp lệ"),
    phone: z
      .string()
      .trim()
      .optional()
      .refine(
        (val) => !val || /(84|0[3|5|7|8|9])+([0-9]{8})\b/.test(val),
        "Số điện thoại không hợp lệ (VD: 0987654321)"
      ),
    password: passwordSchema,
    confirmPassword: z.string().min(1, "Vui lòng xác nhận mật khẩu"),
    agreeTerms: z.boolean().refine((val) => val === true, {
      message: "Bạn phải đồng ý với Điều khoản dịch vụ và Chính sách bảo mật",
    }),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Mật khẩu xác nhận không khớp",
    path: ["confirmPassword"],
  });

export type RegisterFormData = z.infer<typeof registerSchema>;

export const forgotPasswordSchema = z.object({
  email: z
    .string()
    .trim()
    .toLowerCase()
    .min(1, "Vui lòng nhập địa chỉ email đã đăng ký")
    .email("Định dạng email không hợp lệ"),
});

export type ForgotPasswordFormData = z.infer<typeof forgotPasswordSchema>;

export const otpSchema = z.object({
  token: z
    .string()
    .trim()
    .length(6, "Mã OTP phải có đúng 6 chữ số")
    .regex(/^\d{6}$/, "Mã OTP chỉ bao gồm chữ số"),
});

export type OtpFormData = z.infer<typeof otpSchema>;

export const resetPasswordSchema = z
  .object({
    token: z
      .string()
      .trim()
      .length(6, "Mã OTP phải đúng 6 chữ số"),
    newPassword: passwordSchema,
    confirmPassword: z.string().min(1, "Vui lòng xác nhận mật khẩu mới"),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "Mật khẩu xác nhận không khớp",
    path: ["confirmPassword"],
  });

export type ResetPasswordFormData = z.infer<typeof resetPasswordSchema>;
