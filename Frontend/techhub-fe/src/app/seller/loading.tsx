import React from "react";

export default function SellerLoading() {
  return (
    <div className="flex-1 w-full p-4 sm:p-6 lg:p-8 flex flex-col gap-5 max-w-[1440px] mx-auto animate-pulse motion-reduce:animate-none">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-2xl bg-slate-200/80 shrink-0" />
        <div className="flex flex-col gap-2">
          <div className="h-5 w-48 rounded-lg bg-slate-200/80" />
          <div className="h-3 w-64 rounded-md bg-slate-200/60" />
        </div>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200/80 p-4 flex flex-col gap-3">
        <div className="h-10 w-full rounded-xl bg-slate-100" />
      </div>

      <div className="bg-white rounded-2xl border border-slate-200/80 p-4 flex flex-col gap-3">
        {[1, 2, 3, 4, 5].map((row) => (
          <div
            key={row}
            className="h-12 w-full rounded-xl bg-slate-50 border border-slate-100"
          />
        ))}
      </div>
    </div>
  );
}
