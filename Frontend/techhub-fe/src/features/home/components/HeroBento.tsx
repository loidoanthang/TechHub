import Link from "next/link";

export function HeroBento() {
  return (
    <section className="max-w-6xl mx-auto px-6 lg:px-12 w-full pt-6 pb-14">
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-stretch min-h-[380px]">
        {/* Left Wide Card (Hero: Upgrade Your Tech Game) */}
        <div
          className="lg:col-span-6 rounded-2xl relative overflow-hidden flex flex-col justify-between p-8 lg:p-10 shadow-md group min-h-[360px] text-[#eaf1ff] bg-[#213145]"
          style={{
            backgroundImage:
              "linear-gradient(to right, rgba(11,28,48,0.92) 0%, rgba(11,28,48,0.7) 50%, rgba(11,28,48,0.4) 100%), url('https://lh3.googleusercontent.com/aida-public/AB6AXuCRerfsF4uN531w_dT2RXSf0XspKm3j-Ej87Su7SqCILbx8yyZYUuqxw9us3hKPQy54xFmnPcdpuUgOGdB2reE1-8K86Hxqg3Uft8mywz7jT1hbuHsH448kKUMBCYCFB71O8D7SLGWAU_K6NHjanNWpHxGAHXlosnyRq_rSI0KiSwjsmxSqPV1vui0CPTwkiYJ6rDzduFVeYJyxtL7O9P3pjEOVuaP-g8U7ZBFl9eiwAsd8mbpE6NDQ0Q')",
            backgroundSize: "cover",
            backgroundPosition: "center",
          }}
        >
          <div className="max-w-md z-10">
            <h1 className="text-3xl lg:text-4xl font-bold text-white tracking-tight leading-tight mb-3 font-[family-name:var(--font-heading)]">
              Upgrade Your Tech<br />Game
            </h1>
            <p className="text-sm text-[#dce9ff] leading-relaxed max-w-sm mb-8">
              Gradually work your way up to more intense games as you level up your battle station.
            </p>
            <Link
              href="/products"
              className="inline-flex items-center justify-center bg-white hover:bg-[#eff4ff] text-[#0b1c30] text-sm font-bold px-6 py-2.5 rounded-lg shadow-sm transition-colors"
            >
              Shop Now
            </Link>
          </div>
        </div>

        {/* Center Medium Card (Studio Headphones) */}
        <div
          className="lg:col-span-3 rounded-2xl relative overflow-hidden flex flex-col justify-between p-6 lg:p-7 shadow-md group min-h-[360px] text-[#eaf1ff] bg-[#213145]"
          style={{
            backgroundImage:
              "linear-gradient(to bottom, rgba(11,28,48,0.9) 0%, rgba(11,28,48,0.65) 50%, rgba(11,28,48,0.95) 100%), url('https://lh3.googleusercontent.com/aida-public/AB6AXuAbgN1n61LQ-44P0lE_y8NQDV1IRAMbPXd2YYSZR-ZzKODtH2DtZFthEoJKrmgUsA2xz_XrGJ2h0orH-4s8SgGdVTsVzlQLvZyvN54khs8-p_85YhJTd6R83oRl9eBAfOBYnmav-xzY-xUK3hJyajHKWbIxObVna0nKKxM3nTe5O-7kDlWNJYac3Gwu9Sw2WrjzxSpsRVc-u919FMowK8DRaapEot4l17d6Ca4bKkFhTXq7-XrRwDqQrg')",
            backgroundSize: "cover",
            backgroundPosition: "center",
          }}
        >
          <div className="z-10">
            <span className="text-[11px] text-[#b6c4ff] font-bold uppercase tracking-wider block mb-2 font-mono">
              PREMIUM ACOUSTICS
            </span>
            <h2 className="text-xl font-bold text-white tracking-tight mb-2 font-[family-name:var(--font-heading)]">
              Studio Sound
            </h2>
            <p className="text-xs text-[#dce9ff] leading-relaxed">
              Planar magnetic precision drivers.
            </p>
          </div>

          <div className="z-10 flex items-center justify-between pt-4">
            <div className="flex flex-col">
              <span className="text-[10px] text-[#dce9ff] uppercase font-bold">Starts at</span>
              <span className="text-xl font-bold text-white font-mono">$399</span>
            </div>
            <Link
              href="/products?category=audio"
              className="w-10 h-10 rounded-full bg-white/20 hover:bg-[#0045cc] text-white flex items-center justify-center transition-colors backdrop-blur-md"
              aria-label="View Studio Sound"
            >
              →
            </Link>
          </div>
        </div>

        {/* Right Column (2 Small Stacked Cards) */}
        <div className="lg:col-span-3 flex flex-col gap-6">
          {/* Top Card: Smart Watch & Trackers */}
          <div
            className="flex-1 rounded-2xl relative overflow-hidden flex flex-col justify-between p-6 shadow-md group min-h-[170px] text-[#eaf1ff] bg-[#213145]"
            style={{
              backgroundImage:
                "linear-gradient(to right, rgba(11,28,48,0.9) 0%, rgba(11,28,48,0.5) 100%), url('https://lh3.googleusercontent.com/aida-public/AB6AXuD8mG3hF8b9iU1m3k_BGBu8p2mXn06oUvWj5vT2cZbK_5A5zY7d1X_kFqXmG_2nKq7jM6aP3_8xY0u_7w4zD2sP5v9a7r1kM2yZ5kX8aB2m')",
              backgroundSize: "cover",
              backgroundPosition: "center",
            }}
          >
            <div className="z-10 max-w-[180px]">
              <span className="text-[10px] text-[#57fbdb] font-bold uppercase tracking-wider block mb-1 font-mono">
                BIO-TELEMETRY
              </span>
              <h3 className="text-base font-bold text-white font-[family-name:var(--font-heading)]">
                Smart Trackers
              </h3>
            </div>
            <Link
              href="/products?category=wearables"
              className="z-10 text-xs font-semibold text-[#ebecff] hover:text-white flex items-center gap-1"
            >
              Discover gear →
            </Link>
          </div>

          {/* Bottom Card: Custom Keyboards */}
          <div
            className="flex-1 rounded-2xl relative overflow-hidden flex flex-col justify-between p-6 shadow-md group min-h-[170px] text-[#eaf1ff] bg-[#213145]"
            style={{
              backgroundImage:
                "linear-gradient(to right, rgba(11,28,48,0.9) 0%, rgba(11,28,48,0.5) 100%), url('https://lh3.googleusercontent.com/aida-public/AB6AXuBU-AYUmK1HALzSK9UW9wd58KIfVwtSHAeEno-3mG0GX2GGxRbVbNzK2vnjU5O-vrBJ0McSrnAeAcu3tnwMPc7GBsgYxyL-0O4zyMRdeDubyHIkBYjJ-gMmX4pwWPVTbsK72faWze_YXEosnPlBxQ5qMCwvywW0ESYcJatsdeY79I4IHgJBj2E_Y0kLEgEC7HE7PcmepnJaJLk2dM-SH3rlKpOuziSHS2OxNQRcDihnW6Hh2ZXH7zUE0w')",
              backgroundSize: "cover",
              backgroundPosition: "center",
            }}
          >
            <div className="z-10 max-w-[180px]">
              <span className="text-[10px] text-[#ffdad4] font-bold uppercase tracking-wider block mb-1 font-mono">
                TACTILE HARDWARE
              </span>
              <h3 className="text-base font-bold text-white font-[family-name:var(--font-heading)]">
                Custom Keyboards
              </h3>
            </div>
            <Link
              href="/products?category=keyboards"
              className="z-10 text-xs font-semibold text-[#ebecff] hover:text-white flex items-center gap-1"
            >
              Shop Hot-swap →
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}
