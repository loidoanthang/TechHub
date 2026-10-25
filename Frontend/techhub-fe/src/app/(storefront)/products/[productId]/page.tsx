import { Suspense } from "react";
import { ProductDetailView } from "@/features/products/components";
import { getMockProductDetail } from "@/mocks";

interface ProductPageProps {
  params: Promise<{
    productId: string;
  }>;
}

async function ProductDetailLoader({ params }: ProductPageProps) {
  const { productId } = await params;
  const productData = getMockProductDetail(productId);
  return <ProductDetailView product={productData} />;
}

export default function ProductDetailPage(props: ProductPageProps) {
  return (
    <Suspense
      fallback={
        <div className="max-w-6xl mx-auto px-6 py-20 text-center text-sm text-slate-500">
          Loading hardware specifications...
        </div>
      }
    >
      <ProductDetailLoader {...props} />
    </Suspense>
  );
}
