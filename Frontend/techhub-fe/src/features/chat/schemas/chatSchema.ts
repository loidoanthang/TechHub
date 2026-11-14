import { z } from "zod";

export const chatMessageSchema = z.object({
  content: z
    .string()
    .trim()
    .min(1, "Tin nhắn không được để trống")
    .max(2000, "Tin nhắn tối đa 2000 ký tự"),
});

export type ChatMessageFormValues = z.infer<typeof chatMessageSchema>;
