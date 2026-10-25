import {
  HeroBento,
  CategoryGrid,
  FlashDeals,
  RecommendedProducts,
} from "@/features/home/components";

export default function HomePage() {
  return (
    <div className="w-full flex flex-col">
      <HeroBento />
      <CategoryGrid />
      <FlashDeals />
      <RecommendedProducts />
    </div>
  );
}
