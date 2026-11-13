import React from "react";

export default function AdminLoading() {
  return (
    <div className="flex-1 w-full p-4 sm:p-6 lg:p-8 flex flex-col gap-5 max-w-[1440px] mx-auto animate-pulse motion-reduce:animate-none">
      {/* Header Skeleton */}
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-2xl bg-slate-200/80 shrink-0" />
        <div className="flex flex-col gap-2">
          <div className="h-5 w-52 rounded-lg bg-slate-200/80" />
          <div className="h-3 w-72 rounded-md bg-slate-200/60" />
        </div>
      </div>

      {/* Filter Bar Skeleton */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-4 flex flex-col gap-3">
        <div className="flex items-center gap-2 pb-2 border-b border-slate-100">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-8 w-24 rounded-xl bg-slate-200/70" />
          ))}
        </div>
        <div className="h-10 w-full rounded-xl bg-slate-100" />
      </div>

      {/* Table Skeleton */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-4 flex flex-col gap-3">
        <div className="h-9 w-full rounded-xl bg-slate-100" />
        {[1, 2, 3, 4, 5, 6].map((row) => (
          <div
            key={row}
            className="h-12 w-full rounded-xl bg-slate-50 border border-slate-100"
          />
        ))}
      </div>
    </div>
  );
}
