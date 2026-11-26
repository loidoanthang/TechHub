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
            Sàn thương mại điện tử đa nhà bán chuyên sâu thiết bị công nghệ, linh kiện chính hãng và enthusiast gear.
          </p>
        </div>

        <div className="flex flex-col gap-2.5 text-xs">
          <h5 className="font-bold text-sm text-[#0b1c30] mb-1 font-[family-name:var(--font-heading)]">
            Thiết bị công nghệ
          </h5>
          <Link href="/products?category=keyboards" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Bàn phím cơ Custom
          </Link>
          <Link href="/products?category=displays" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Màn hình tần số quét cao
          </Link>
          <Link href="/products?category=laptops" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Laptop &amp; Workstation
          </Link>
          <Link href="/products?category=audio" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            DAC &amp; Âm thanh Hi-Fi
          </Link>
          <Link href="/products?category=cooling" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Giải pháp tản nhiệt
          </Link>
        </div>

        <div className="flex flex-col gap-2.5 text-xs">
          <h5 className="font-bold text-sm text-[#0b1c30] mb-1 font-[family-name:var(--font-heading)]">
            Hệ sinh thái Nhà bán
          </h5>
          <Link href="/seller/register" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Bán hàng cùng TechHub
          </Link>
          <Link href="/seller/dashboard" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Kênh Người Bán
          </Link>
          <Link href="#" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Vận hành bởi TechHub
          </Link>
          <Link href="#" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Kiểm định chính hãng
          </Link>
          <Link href="#" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Điều khoản Nhà bán
          </Link>
        </div>

        <div className="flex flex-col gap-2.5 text-xs">
          <h5 className="font-bold text-sm text-[#0b1c30] mb-1 font-[family-name:var(--font-heading)]">
            Hỗ trợ khách hàng
          </h5>
          <Link href="/account/orders" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Theo dõi đơn hàng
          </Link>
          <Link href="#" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Bảo vệ người mua
          </Link>
          <Link href="#" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Đổi trả &amp; Bảo hành
          </Link>
          <Link href="#" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Giải quyết khiếu nại
          </Link>
          <Link href="#" className="text-[#434656] hover:text-[#0b1c30] transition-colors">
            Bảo mật &amp; Tuân thủ
          </Link>
        </div>
      </div>

      <div className="bg-[#e5eeff] py-4">
        <div className="max-w-6xl mx-auto px-6 lg:px-12 flex flex-col md:flex-row items-center justify-between gap-2 text-xs text-[#434656]">
          <span>© 2026 TechHub Marketplace. Bảo lưu mọi quyền.</span>
          <div className="flex items-center gap-6">
            <Link href="#" className="hover:text-[#0b1c30] transition-colors">Chính sách bảo mật</Link>
            <Link href="#" className="hover:text-[#0b1c30] transition-colors">Điều khoản sử dụng</Link>
          </div>
        </div>
      </div>
    </footer>
  );
}
