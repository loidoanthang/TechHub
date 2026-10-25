import Image from "next/image";
import { Zap, Heart, ShieldCheck, ShoppingCart } from "lucide-react";
import { MOCK_FLASH_DEALS } from "@/mocks";

export function FlashDeals() {
  return (
    <section className="max-w-6xl mx-auto px-6 lg:px-12 w-full py-16">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-8 gap-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-[#a90900]/10 text-[#a90900] flex items-center justify-center">
            <Zap className="w-5 h-5 fill-current" />
          </div>
          <div>
            <h2 className="text-2xl font-bold text-[#0b1c30] tracking-tight font-[family-name:var(--font-heading)]">
              Flash Deals
            </h2>
            <p className="text-xs text-[#434656]">Limited-quantity hardware drops refreshed hourly</p>
          </div>
        </div>

        {/* Countdown Timer from Stitch */}
        <div className="flex items-center gap-3">
          <span className="text-xs font-semibold text-[#434656] uppercase tracking-wider font-mono">Ends In</span>
          <div className="flex items-center gap-1 font-mono text-xs font-bold">
            <span className="px-2 py-1 rounded bg-[#0b1c30] text-white">04</span>
            <span>:</span>
            <span className="px-2 py-1 rounded bg-[#0b1c30] text-white">28</span>
            <span>:</span>
            <span className="px-2 py-1 rounded bg-[#0b1c30] text-white">15</span>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {MOCK_FLASH_DEALS.map((deal) => {
          const progressPercent = Math.round((deal.soldCount / deal.totalStock) * 100);
          return (
            <div
              key={deal.id}
              className="bg-white rounded-xl shadow-sm p-4 flex flex-col justify-between hover:shadow-lg hover:-translate-y-1 transition-all duration-200 group relative border border-[#eff4ff]"
            >
              <button
                aria-label="Save to Wishlist"
                className="absolute top-6 right-6 z-20 text-[#737688] hover:text-[#a90900] transition-colors"
              >
                <Heart className="w-5 h-5" />
              </button>

              <div>
                <div className="relative w-full aspect-square rounded-lg bg-[#eff4ff] overflow-hidden mb-3 p-4 flex items-center justify-center">
                  <Image
                    src={deal.imageUrl}
                    alt={deal.title}
                    fill
                    sizes="(max-width: 768px) 100vw, 33vw"
                    className="object-contain p-2 group-hover:scale-105 transition-transform duration-300"
                    unoptimized
                  />
                  <span className="absolute top-2 left-2 bg-[#a90900] text-white text-[11px] font-bold px-2 py-0.5 rounded-full z-10">
                    -{deal.discountPercentage}%
                  </span>
                </div>

                <div className="flex items-center gap-1.5 text-[11px] text-[#434656] mb-1">
                  <span className="font-semibold truncate">{deal.vendorName}</span>
                  {deal.isVerifiedVendor && (
                    <ShieldCheck className="w-3.5 h-3.5 text-[#0045cc] flex-shrink-0" />
                  )}
                </div>

                <h3 className="text-sm font-semibold text-[#0b1c30] line-clamp-2 hover:text-[#0045cc] transition-colors mb-3 leading-snug">
                  {deal.title}
                </h3>

                <div className="flex items-baseline gap-2 font-mono mb-4">
                  <span className="text-lg font-bold text-[#0b1c30]">
                    ${deal.price.toFixed(2)}
                  </span>
                  <span className="text-xs text-[#737688] line-through">
                    ${deal.originalPrice.toFixed(2)}
                  </span>
                </div>

                {/* Progress bar */}
                <div className="w-full bg-[#dce9ff] h-2 rounded-full overflow-hidden mb-1.5">
                  <div
                    className="bg-[#0045cc] h-full rounded-full transition-all"
                    style={{ width: `${progressPercent}%` }}
                  />
                </div>
                <div className="flex items-center justify-between text-[11px] text-[#434656] font-mono mb-4">
                  <span>Sold: {deal.soldCount}</span>
                  <span>Available: {deal.totalStock - deal.soldCount}</span>
                </div>
              </div>

              <button className="w-full py-2.5 rounded-lg bg-[#eff4ff] hover:bg-[#0045cc] text-[#0045cc] hover:text-white font-semibold text-xs transition-colors flex items-center justify-center gap-2">
                <ShoppingCart className="w-4 h-4" />
                <span>Claim Deal</span>
              </button>
            </div>
          );
        })}
      </div>
    </section>
  );
}
