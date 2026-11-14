import React from "react";
import { cn } from "@/lib/utils";

export interface TimelineStepInfoProps {
  title: string;
  description: string;
  timestamp?: string | null;
  isCompleted: boolean;
  isCurrent: boolean;
  layout?: "horizontal" | "vertical";
}

export function TimelineStepInfo({
  title,
  description,
  timestamp,
  isCompleted,
  isCurrent,
  layout = "horizontal",
}: TimelineStepInfoProps) {
  return (
    <div
      className={cn(
        "flex flex-col",
        layout === "horizontal" ? "items-center mt-3 text-center" : "pt-0.5"
      )}
    >
      <span
        className={cn(
          "text-xs font-bold leading-tight",
          isCompleted || isCurrent ? "text-[#0b1c30]" : "text-slate-500"
        )}
      >
        {title}
      </span>
      <span
        className={cn(
          "text-[11px] text-[#737688] leading-snug",
          layout === "horizontal" ? "mt-1 line-clamp-2 max-w-[150px]" : "mt-0.5"
        )}
      >
        {description}
      </span>
      {timestamp && (
        <span
          className={cn(
            "text-[10px] font-mono",
            layout === "horizontal"
              ? "mt-1.5 font-medium text-slate-600 bg-slate-50 px-2 py-0.5 rounded border border-slate-200"
              : "text-slate-500 mt-1"
          )}
        >
          {new Date(timestamp).toLocaleDateString("vi-VN", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
          })}
        </span>
      )}
    </div>
  );
}
