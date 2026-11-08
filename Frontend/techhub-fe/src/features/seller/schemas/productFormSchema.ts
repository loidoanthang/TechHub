import { z } from "zod";

export const productImageSchema = z.object({
  imageUrl: z
    .string()
    .trim()
    .min(1, "Đường dẫn ảnh không được để trống")
    .max(500, "Đường dẫn ảnh không được vượt quá 500 ký tự")
    .regex(/^https?:\/\/.+/i, "Đường dẫn ảnh phải là liên kết hợp lệ (bắt đầu bằng http:// hoặc https://)"),
  displayOrder: z.number().int().min(0).max(5),
});

export const productVariantSchema = z.object({
  sku: z
    .string()
    .trim()
    .min(1, "Mã phân loại (SKU) không được để trống")
    .max(100, "Mã SKU không được vượt quá 100 ký tự"),
  variantName: z
    .string()
    .trim()
    .min(1, "Tên phân loại không được để trống")
    .max(200, "Tên phân loại không được vượt quá 200 ký tự"),
  description: z.string().optional(),
  price: z
    .number()
    .min(1000, "Giá bán tối thiểu là 1.000 đ")
    .max(1000000000, "Giá bán tối đa là 1.000.000.000 đ"),
  stockQuantity: z
    .number()
    .int("Số lượng tồn kho phải là số nguyên")
    .min(0, "Số lượng tồn kho không được âm")
    .max(1000000, "Số lượng tồn kho tối đa là 1.000.000"),
  imageUrl: z.string().optional(),
  attributes: z.record(z.string(), z.unknown()).optional(),
});

export const createProductFormSchema = z
  .object({
    name: z
      .string()
      .trim()
      .min(5, "Tên sản phẩm phải có ít nhất 5 ký tự")
      .max(300, "Tên sản phẩm không được vượt quá 300 ký tự"),
    categoryId: z
      .string()
      .trim()
      .min(1, "Vui lòng chọn ngành hàng / danh mục linh kiện"),
    brandName: z
      .string()
      .trim()
      .min(1, "Vui lòng nhập hoặc chọn thương hiệu sản phẩm")
      .max(100, "Tên thương hiệu không được vượt quá 100 ký tự"),
    description: z.string().optional(),
    images: z
      .array(productImageSchema)
      .min(1, "Sản phẩm phải có ít nhất 1 hình ảnh làm ảnh bìa")
      .max(6, "Sản phẩm chỉ được có tối đa 6 hình ảnh")
      .refine(
        (imgs) => imgs.some((img) => img.displayOrder === 0),
        { message: "Bắt buộc phải có đúng 1 ảnh được chọn làm Ảnh Bìa chính (vị trí đầu tiên)" }
      ),
    variants: z
      .array(productVariantSchema)
      .min(1, "Sản phẩm phải có ít nhất 1 phiên bản phân loại")
      .max(50, "Sản phẩm chỉ được có tối đa 50 phiên bản phân loại")
      .refine(
        (vars) => {
          const skus = new Set<string>();
          for (const v of vars) {
            const clean = v.sku.trim().toUpperCase();
            if (skus.has(clean)) return false;
            skus.add(clean);
          }
          return true;
        },
        { message: "Mã SKU giữa các phân loại cấu hình không được trùng lặp" }
      ),
    attributes: z.record(z.string(), z.unknown()),
  });

export type CreateProductFormValues = z.infer<typeof createProductFormSchema>;
