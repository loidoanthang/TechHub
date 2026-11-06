import { z } from "zod";
import { passwordSchema } from "@/features/auth/schemas/authSchemas";

export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, "Vui lòng nhập mật khẩu hiện tại"),
    newPassword: passwordSchema,
    confirmPassword: z.string().min(1, "Vui lòng xác nhận mật khẩu mới"),
  })
  .refine((data) => data.newPassword !== data.currentPassword, {
    message: "Mật khẩu mới không được trùng với mật khẩu hiện tại",
    path: ["newPassword"],
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "Mật khẩu xác nhận không khớp",
    path: ["confirmPassword"],
  });

export type ChangePasswordFormData = z.infer<typeof changePasswordSchema>;

export const deleteAccountSchema = z.object({
  password: z.string().min(1, "Vui lòng nhập mật khẩu để xác nhận"),
  reason: z.string().max(500, "Lý do không được vượt quá 500 ký tự").optional(),
  confirmConsent: z.boolean().refine((val) => val === true, {
    message: "Bạn cần đồng ý với các điều khoản trước khi xóa tài khoản",
  }),
});

export type DeleteAccountFormData = z.infer<typeof deleteAccountSchema>;
