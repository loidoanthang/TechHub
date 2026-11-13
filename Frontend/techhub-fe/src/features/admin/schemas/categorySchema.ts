import { z } from "zod";

export const categoryAttributeSchema = z
  .object({
    name: z
      .string()
      .trim()
      .min(1, "Tên thuộc tính không được để trống")
      .max(100, "Tên thuộc tính tối đa 100 ký tự"),
    dataType: z.enum(["TEXT", "NUMBER", "SELECT"]),
    optionsText: z.string().optional().or(z.literal("")),
    sampleValue: z
      .string()
      .max(255, "Giá trị mẫu tối đa 255 ký tự")
      .optional()
      .or(z.literal("")),
  })
  .refine(
    (data) => {
      if (data.dataType === "SELECT") {
        const opts = (data.optionsText || "")
          .split(",")
          .map((s) => s.trim())
          .filter(Boolean);
        return opts.length > 0;
      }
      return true;
    },
    {
      message: "Kiểu SELECT cần nhập ít nhất 1 lựa chọn (ngăn cách bằng dấu phẩy)",
      path: ["optionsText"],
    }
  );

export type CategoryAttributeFormValues = z.infer<typeof categoryAttributeSchema>;

export const createCategorySchema = z.object({
  name: z
    .string()
    .trim()
    .min(2, "Tên danh mục tối thiểu 2 ký tự")
    .max(100, "Tên danh mục tối đa 100 ký tự"),
  description: z
    .string()
    .max(500, "Mô tả tối đa 500 ký tự")
    .optional()
    .or(z.literal("")),
  attributes: z
    .array(categoryAttributeSchema)
    .min(1, "Danh mục bắt buộc phải có ít nhất 1 thuộc tính kỹ thuật")
    .max(50, "Tối đa 50 thuộc tính"),
});

export type CreateCategoryFormValues = z.infer<typeof createCategorySchema>;

export const updateCategorySchema = z.object({
  name: z
    .string()
    .trim()
    .min(2, "Tên danh mục tối thiểu 2 ký tự")
    .max(100, "Tên danh mục tối đa 100 ký tự"),
  description: z
    .string()
    .max(500, "Mô tả tối đa 500 ký tự")
    .optional()
    .or(z.literal("")),
});

export type UpdateCategoryFormValues = z.infer<typeof updateCategorySchema>;
