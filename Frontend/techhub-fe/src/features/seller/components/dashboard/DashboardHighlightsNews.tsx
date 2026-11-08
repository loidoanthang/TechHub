"use client";

import React from "react";
import Link from "next/link";
import { BookOpen, Sparkles, ChevronRight, ShieldCheck, TicketPercent, Package } from "lucide-react";

export function DashboardHighlightsNews() {
  const articles = [
    {
      title: "Quy chuẩn đóng gói chống sốc linh kiện phần cứng",
      category: "VẬN HÀNH & KHO",
      desc: "Hướng dẫn quấn xốp nổ 3 lớp, chống tĩnh điện ESD và dán tem niêm phong để được bảo hiểm 100% khi bưu tá lấy hàng.",
      icon: Package,
      iconColor: "text-[#0045cc] bg-[#eff4ff]",
      link: "#logistics-guide",
    },
    {
      title: "Tạo Voucher Shop kích cầu đơn hàng cấu hình cao",
      category: "MARKETING HUB",
      desc: "Cách cấu hình mã giảm giá theo ngưỡng chi tiêu để nâng giá trị đơn trung bình (AOV) của người mua.",
      icon: TicketPercent,
      iconColor: "text-purple-600 bg-purple-50",
      link: "/seller/vouchers",
    },
    {
      title: "Cơ chế bảo chứng Escrow & Chu kỳ quyết toán ví",
      category: "TÀI CHÍNH",
      desc: "Hiểu rõ thời gian giữ tiền an toàn để bảo vệ quyền lợi giữa Người mua và Người bán khi có tranh chấp.",
      icon: ShieldCheck,
      iconColor: "text-emerald-600 bg-emerald-50",
      link: "#escrow-policy",
    },
  ];

  return (
    <div className="bg-white rounded-3xl p-5 sm:p-6 border border-[#e2e8f0] shadow-2xs flex flex-col gap-4">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-[#f1f5f9] pb-3">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-xl bg-[#0045cc]/10 text-[#0045cc] flex items-center justify-center font-bold">
            <BookOpen className="w-4 h-4" />
          </div>
          <div>
            <h3 className="font-heading text-sm sm:text-base font-bold text-[#0b1c30]">
              Cẩm Nang &amp; Thông Báo Người Bán
            </h3>
            <p className="text-[11px] text-[#737688]">
              Tài liệu vận hành và chính sách sàn TechHub Seller Centre.
            </p>
          </div>
        </div>
      </div>

      {/* List */}
      <div className="flex flex-col gap-3">
        {articles.map((art) => {
          const Icon = art.icon;
          return (
            <Link
              key={art.title}
              href={art.link}
              className="p-3.5 rounded-2xl bg-[#f8f9fc] hover:bg-[#eff4ff]/60 border border-[#e2e8f0] hover:border-[#0045cc]/30 transition-all flex items-start gap-3 group"
            >
              <div
                className={`w-9 h-9 rounded-xl ${art.iconColor} flex items-center justify-center shrink-0 mt-0.5`}
              >
                <Icon className="w-4 h-4" />
              </div>
              <div className="flex-1 min-w-0">
                <span className="text-[10px] font-extrabold text-[#737688] tracking-wider uppercase">
                  {art.category}
                </span>
                <h4 className="text-xs font-bold text-[#0b1c30] group-hover:text-[#0045cc] transition-colors line-clamp-1">
                  {art.title}
                </h4>
                <p className="text-[11px] text-[#737688] line-clamp-2 mt-0.5">
                  {art.desc}
                </p>
              </div>
              <ChevronRight className="w-4 h-4 text-[#94a3b8] group-hover:text-[#0045cc] shrink-0 self-center transition-colors" />
            </Link>
          );
        })}
      </div>

      {/* Promo banner */}
      <div className="mt-1 p-4 rounded-2xl bg-gradient-to-r from-[#eff4ff] via-white to-[#eff4ff] border border-[#0045cc]/20 flex items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <Sparkles className="w-4 h-4 text-[#0045cc] shrink-0" />
          <span className="text-xs font-bold text-[#0045cc]">
            Gia tăng tỷ lệ chuyển đổi với Voucher Shop
          </span>
        </div>
        <Link
          href="/seller/vouchers"
          className="text-xs font-bold text-white bg-[#0045cc] hover:bg-[#0d5bff] px-3 py-1.5 rounded-xl transition-all shadow-2xs whitespace-nowrap"
        >
          Tạo mã ngay
        </Link>
      </div>
    </div>
  );
}
