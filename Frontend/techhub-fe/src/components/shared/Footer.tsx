import Link from "next/link";
import Image from "next/image";

export function Footer() {
  return (
    <footer className="w-full bg-[#eff4ff] mt-10">
      <div className="max-w-6xl mx-auto px-6 lg:px-12 py-12 grid grid-cols-1 md:grid-cols-5 gap-8">
        <div className="md:col-span-2 flex flex-col gap-4">
          <div className="flex items-center gap-2.5">
            <div className="relative w-7 h-7 flex items-center justify-center">
              <Image
                src="/logo.svg"
                alt="TechHub Logo"
                width={28}
                height={28}
                className="object-contain"
              />
            </div>
            <span className="text-xl font-bold tracking-tight text-[#0b1c30] font-[family-name:var(--font-heading)]">
              Tech<span className="text-[#0045cc]">Hub</span>
            </span>
          </div>
          <p className="text-sm text-[#434656] max-w-sm leading-relaxed">
            The verified multi-vendor marketplace engineered for technical transparency, certified components, and enthusiast consumer electronics.
          </p>
        </div>

        <div className="flex flex-col gap-2.5 text-xs">
          <h5 className="font-bold text-sm text-[#0b1c30] mb-1 font-[family-name:var(--font-heading)]">
            Enthusiast Gear
          </h5>
          <Link href="/products?category=keyboards" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Custom Keyboards
          </Link>
          <Link href="/products?category=displays" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            High-Refresh Displays
          </Link>
          <Link href="/products?category=laptops" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Workstations & Laptops
          </Link>
          <Link href="/products?category=audio" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Audiophile DACs
          </Link>
          <Link href="/products?category=cooling" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Thermal Solutions
          </Link>
        </div>

        <div className="flex flex-col gap-2.5 text-xs">
          <h5 className="font-bold text-sm text-[#0b1c30] mb-1 font-[family-name:var(--font-heading)]">
            Vendor Ecosystem
          </h5>
          <Link href="/seller/register" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Sell on TechHub
          </Link>
          <Link href="/seller/dashboard" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Merchant Dashboard
          </Link>
          <Link href="#" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Fulfillment by TechHub
          </Link>
          <Link href="#" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Authenticity Verification
          </Link>
          <Link href="#" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Vendor Terms
          </Link>
        </div>

        <div className="flex flex-col gap-2.5 text-xs">
          <h5 className="font-bold text-sm text-[#0b1c30] mb-1 font-[family-name:var(--font-heading)]">
            Client Support
          </h5>
          <Link href="/account/orders" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Order Tracking
          </Link>
          <Link href="#" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Buyer Protection Escrow
          </Link>
          <Link href="#" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Returns & Hardware RMA
          </Link>
          <Link href="#" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Dispute Resolution
          </Link>
          <Link href="#" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Security & Compliance
          </Link>
        </div>
      </div>

      <div className="bg-[#e5eeff] py-4">
        <div className="max-w-6xl mx-auto px-6 lg:px-12 flex flex-col md:flex-row items-center justify-between gap-2 text-xs text-[#434656]">
          <span>© 2026 TechHub Global Marketplace Inc. All rights reserved.</span>
          <div className="flex items-center gap-6">
            <Link href="#" className="hover:text-[#0b1c30] transition-colors">Privacy Policy</Link>
            <Link href="#" className="hover:text-[#0b1c30] transition-colors">Terms of Service</Link>
          </div>
        </div>
      </div>
    </footer>
  );
}
