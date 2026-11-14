"use client";

import React, { useState } from "react";
import Link from "next/link";
import {
  ReceiptText,
  ExternalLink,
  Copy,
  Check,
  ArrowUpRight,
  ArrowDownLeft,
  RefreshCw,
} from "lucide-react";
import { Pagination } from "@/components/shared/Pagination";
import { formatPrice } from "@/lib/utils";
import { toast } from "@/lib/toast";
import { ROUTES } from "@/lib/constants";
import { formatTransactionDate } from "../../utils/walletUtils";
import type {
  SellerWalletTransactionResponse,
  SellerWalletTransactionType,
} from "../../types/wallet";

interface TransactionTableProps {
  transactions: SellerWalletTransactionResponse[];
  isLoading: boolean;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  onResetFilters?: () => void;
}

interface TxTypeMeta {
  label: string;
  badgeClass: string;
  iconClass: string;
  Icon: React.ComponentType<{ className?: string }>;
  isCredit: boolean;
  isOrderLink: boolean;
}

const TX_META: Record<SellerWalletTransactionType, TxTypeMeta> = {
  ORDER_SETTLEMENT: {
    label: "Quyết toán đơn",
    badgeClass: "bg-emerald-50 text-emerald-700 border-emerald-200",
    iconClass: "text-emerald-600",
    Icon: ArrowUpRight,
    isCredit: true,
    isOrderLink: true,
  },
  WITHDRAWAL: {
    label: "Rút tiền ví",
    badgeClass: "bg-rose-50 text-rose-700 border-rose-200",
    iconClass: "text-rose-600",
    Icon: ArrowDownLeft,
    isCredit: false,
    isOrderLink: false,
  },
  WITHDRAWAL_REFUND: {
    label: "Hoàn tiền rút",
    badgeClass: "bg-sky-50 text-sky-700 border-sky-200",
    iconClass: "text-sky-600",
    Icon: RefreshCw,
    isCredit: true,
    isOrderLink: false,
  },
};

export function TransactionTable({
  transactions,
  isLoading,
  page,
  size,
  totalElements,
  totalPages,
  onPageChange,
  onResetFilters,
}: TransactionTableProps) {
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const handleCopy = (id: string, label: string) => {
    navigator.clipboard.writeText(id);
    setCopiedId(id);
    toast.success(`Đã sao chép mã ${label}`);
    setTimeout(() => setCopiedId(null), 2000);
  };

  if (isLoading) {
    return (
      <div className="bg-white rounded-2xl border border-[#eff4ff] p-6 shadow-xs space-y-4">
        {[1, 2, 3, 4, 5].map((i) => (
          <div key={i} className="h-12 bg-slate-100/80 rounded-xl animate-pulse" />
        ))}
      </div>
    );
  }

  if (transactions.length === 0) {
    return (
      <div className="bg-white rounded-2xl border border-[#eff4ff] p-12 text-center shadow-xs flex flex-col items-center">
        <div className="w-14 h-14 rounded-2xl bg-slate-100 flex items-center justify-center text-slate-400 mb-4">
          <ReceiptText className="w-7 h-7" />
        </div>
        <h4 className="text-base font-bold text-slate-900 mb-1">
          Chưa có biến động số dư nào
        </h4>
        <p className="text-xs text-slate-500 max-w-sm mb-5 leading-relaxed">
          Hiện chưa ghi nhận giao dịch nào trong khoảng thời gian hoặc bộ lọc đã chọn.
        </p>
        {onResetFilters && (
          <button
            type="button"
            onClick={onResetFilters}
            className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold transition-colors cursor-pointer"
          >
            Đặt lại bộ lọc
          </button>
        )}
      </div>
    );
  }

  return (
    <div className="bg-white rounded-2xl border border-[#eff4ff] shadow-xs overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs">
          <thead className="bg-slate-50/75 border-b border-slate-100 text-slate-500 font-semibold uppercase tracking-wider">
            <tr>
              <th className="py-3.5 px-4 font-semibold">Thời gian</th>
              <th className="py-3.5 px-4 font-semibold">Loại giao dịch</th>
              <th className="py-3.5 px-4 font-semibold text-right">Số tiền</th>
              <th className="py-3.5 px-4 font-semibold text-right">Số dư sau GD</th>
              <th className="py-3.5 px-4 font-semibold">Mã tham chiếu</th>
              <th className="py-3.5 px-4 font-semibold">Mô tả</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 text-slate-700 font-medium">
            {transactions.map((tx) => {
              const meta = TX_META[tx.type] || {
                label: tx.type,
                badgeClass: "bg-slate-100 text-slate-700 border-slate-200",
                iconClass: "text-slate-500",
                Icon: ReceiptText,
                isCredit: tx.amount > 0,
                isOrderLink: false,
              };
              const { Icon } = meta;

              return (
                <tr
                  key={tx.id}
                  className="hover:bg-blue-50/20 transition-colors"
                >
                  {/* Thời gian */}
                  <td className="py-3.5 px-4 text-slate-500 font-mono text-[11px] whitespace-nowrap">
                    {formatTransactionDate(tx.createdAt)}
                  </td>

                  {/* Loại giao dịch */}
                  <td className="py-3.5 px-4 whitespace-nowrap">
                    <span
                      className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-bold border ${meta.badgeClass}`}
                    >
                      <Icon className={`w-3 h-3 ${meta.iconClass}`} />
                      <span>{meta.label}</span>
                    </span>
                  </td>

                  {/* Số tiền */}
                  <td className="py-3.5 px-4 text-right font-mono font-bold whitespace-nowrap">
                    <span
                      className={
                        meta.isCredit ? "text-emerald-600" : "text-rose-600"
                      }
                    >
                      {meta.isCredit ? "+" : "-"}{" "}
                      {formatPrice(Math.abs(tx.amount))}
                    </span>
                  </td>

                  {/* Số dư sau giao dịch */}
                  <td className="py-3.5 px-4 text-right font-mono font-bold text-slate-900 whitespace-nowrap">
                    {formatPrice(tx.balanceAfter)}
                  </td>

                  {/* Mã tham chiếu */}
                  <td className="py-3.5 px-4 whitespace-nowrap">
                    {tx.referenceId ? (
                      meta.isOrderLink ? (
                        <Link
                          href={ROUTES.SELLER.ORDER_DETAIL(tx.referenceId)}
                          className="inline-flex items-center gap-1 text-[#0045cc] hover:underline font-mono text-[11px] font-bold"
                          title="Xem chi tiết đơn hàng đối soát"
                        >
                          <span>{tx.referenceId.slice(0, 8)}...</span>
                          <ExternalLink className="w-3 h-3" />
                        </Link>
                      ) : (
                        <button
                          type="button"
                          onClick={() =>
                            handleCopy(tx.referenceId!, "mã tham chiếu")
                          }
                          className="inline-flex items-center gap-1 font-mono text-[11px] text-slate-500 hover:text-slate-900 cursor-pointer"
                          title="Nhấn để sao chép mã tham chiếu"
                        >
                          <span>{tx.referenceId.slice(0, 8)}...</span>
                          {copiedId === tx.referenceId ? (
                            <Check className="w-3 h-3 text-emerald-600" />
                          ) : (
                            <Copy className="w-3 h-3 text-slate-400" />
                          )}
                        </button>
                      )
                    ) : (
                      <span className="text-slate-300">-</span>
                    )}
                  </td>

                  {/* Mô tả */}
                  <td
                    className="py-3.5 px-4 text-slate-600 max-w-xs truncate"
                    title={tx.description || ""}
                  >
                    {tx.description || "Giao dịch hệ thống"}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="p-4 border-t border-slate-100">
          <Pagination
            page={page}
            size={size}
            totalElements={totalElements}
            totalPages={totalPages}
            onPageChange={onPageChange}
            variant="bottom-full"
          />
        </div>
      )}
    </div>
  );
}
