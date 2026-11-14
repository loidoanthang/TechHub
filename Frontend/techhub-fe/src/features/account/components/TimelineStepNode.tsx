import React from "react";
import { Check } from "lucide-react";
import { cn } from "@/lib/utils";

export interface TimelineStepNodeProps {
  stepNum: number;
  isCompleted: boolean;
  isCurrent: boolean;
}

export function TimelineStepNode({
  stepNum,
  isCompleted,
  isCurrent,
}: TimelineStepNodeProps) {
  return (
    <div
      className={cn(
        "w-9 h-9 rounded-full flex items-center justify-center shrink-0 text-xs font-bold relative z-10 shadow-2xs transition-all",
        isCurrent && "bg-primary text-primary-foreground ring-4 ring-primary/30 shadow-md animate-pulse",
        !isCurrent && isCompleted && "bg-primary text-primary-foreground ring-4 ring-white shadow-sm",
        !isCurrent && !isCompleted && "bg-[#eff4ff] border-2 border-slate-200 ring-4 ring-white text-[#737688]"
      )}
    >
      {isCompleted ? (
        <Check className="w-4.5 h-4.5 stroke-[2.5]" />
      ) : (
        <span>{stepNum}</span>
      )}
    </div>
  );
}
