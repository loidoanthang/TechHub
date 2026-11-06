import Link from "next/link";
import { ArrowLeft, Home, Search, ShieldAlert } from "lucide-react";

export default function NotFound() {
  return (
    <div className="min-h-[70vh] flex items-center justify-center px-4 py-16">
      <div className="max-w-md w-full bg-white rounded-3xl border border-slate-200/80 p-8 sm:p-10 shadow-xl flex flex-col items-center text-center gap-5">
        <div className="w-16 h-16 rounded-2xl bg-[#0045cc]/10 text-[#0045cc] flex items-center justify-center shrink-0">
          <ShieldAlert className="w-8 h-8" />
        </div>

        <div className="flex flex-col gap-1.5">
          <span className="text-xs font-mono font-bold uppercase tracking-wider text-[#0045cc]">
            404 • Not found
          </span>
          <h1 className="font-heading text-2xl sm:text-3xl font-bold text-[#0b1c30] tracking-tight">
            Sản phẩm hoặc trang không tồn tại
          </h1>
          <p className="text-xs text-[#737688] leading-relaxed mt-1">
            Mặt hàng bạn đang tìm kiếm có thể đã hết hàng, bị người bán gỡ xuống hoặc đường dẫn liên kết không chính xác.
          </p>
        </div>

        <div className="flex flex-col sm:flex-row items-center gap-3 w-full pt-2">
          <Link
            href="/"
            className="w-full sm:w-1/2 py-2.5 px-4 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold transition-all shadow-sm flex items-center justify-center gap-2"
          >
            <Home className="w-3.5 h-3.5" />
            <span>Trang chủ</span>
          </Link>
          <Link
            href="/search"
            className="w-full sm:w-1/2 py-2.5 px-4 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-semibold transition-all flex items-center justify-center gap-2"
          >
            <Search className="w-3.5 h-3.5 text-[#0045cc]" />
            <span>Tìm sản phẩm khác</span>
          </Link>
        </div>
      </div>
    </div>
  );
}
