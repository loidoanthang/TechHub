import { z } from "zod";

// Vietnamese mobile phone regex (10 digits: 0 / +84)
export const VN_PHONE_REGEX = /^(0|\+84)[35789][0-9]{8}$/;

export const profileSchema = z.object({
  firstName: z
    .string()
    .trim()
    .min(1, "First name is required")
    .max(100, "First name cannot exceed 100 characters"),

  lastName: z
    .string()
    .trim()
    .min(1, "Last name is required")
    .max(100, "Last name cannot exceed 100 characters"),

  phone: z
    .string()
    .trim()
    .min(1, "Phone number is required")
    .refine(
      (val) => {
        const cleaned = val.replace(/[\s\-\(\)\.]/g, "");
        return VN_PHONE_REGEX.test(cleaned);
      },
      {
        message:
          "Invalid Vietnamese phone number (e.g. 0912345678 or +84912345678)",
      }
    ),

  avatarUrl: z
    .string()
    .max(500, "Avatar URL is too long")
    .nullable()
    .optional(),
});

export type ProfileFormValues = z.infer<typeof profileSchema>;
