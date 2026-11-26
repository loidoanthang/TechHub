"use client";

import React, { useEffect, useId, useRef, useState } from "react";
import { Check, ChevronDown } from "lucide-react";

export interface SelectDropdownOption {
  value: string;
  label: string;
  disabled?: boolean;
}

interface SelectDropdownProps {
  value: string;
  onChange: (value: string) => void;
  options: SelectDropdownOption[];
  placeholder?: string;
  disabled?: boolean;
  id?: string;
  ariaLabel?: string;
  className?: string;
  buttonClassName?: string;
  menuClassName?: string;
  align?: "left" | "right";
  placement?: "bottom" | "top";
  leadingIcon?: React.ReactNode;
}

export function SelectDropdown({
  value,
  onChange,
  options,
  placeholder = "Chọn...",
  disabled = false,
  id,
  ariaLabel,
  className = "w-full",
  buttonClassName = "h-10 px-3 bg-white border border-slate-200 rounded-xl text-xs font-medium text-slate-700 hover:border-slate-300 focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/15 shadow-2xs",
  menuClassName = "",
  align = "left",
  placement = "bottom",
  leadingIcon,
}: SelectDropdownProps) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const generatedId = useId();
  const listboxId = `${id || generatedId}-listbox`;

  const selectedOption = options.find((opt) => opt.value === value);
  const displayLabel = selectedOption ? selectedOption.label : placeholder;

  useEffect(() => {
    if (!isOpen) return;

    const handleClickOutside = (event: MouseEvent) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setIsOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen]);

  const positionClasses =
    placement === "top"
      ? "bottom-full mb-1.5"
      : "top-full mt-1.5";

  const alignClasses = align === "right" ? "right-0" : "left-0";

  return (
    <div ref={containerRef} className={`relative ${className}`}>
      <button
        id={id}
        type="button"
        disabled={disabled}
        aria-label={ariaLabel}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        aria-controls={isOpen ? listboxId : undefined}
        onClick={() => {
          if (!disabled) setIsOpen((prev) => !prev);
        }}
        className={`w-full flex items-center justify-between gap-2 text-left transition-colors ${
          disabled ? "opacity-50 cursor-not-allowed" : "cursor-pointer"
        } ${buttonClassName}`}
      >
        <span className="flex items-center gap-2 min-w-0 flex-1">
          {leadingIcon && <span className="shrink-0">{leadingIcon}</span>}
          <span
            className={`truncate ${
              !selectedOption && placeholder ? "text-slate-400" : ""
            }`}
          >
            {displayLabel}
          </span>
        </span>
        <ChevronDown
          className={`w-3.5 h-3.5 text-slate-400 shrink-0 transition-transform duration-150 ${
            isOpen ? "rotate-180 text-[#0045cc]" : ""
          }`}
        />
      </button>

      {isOpen && (
        <div
          id={listboxId}
          role="listbox"
          aria-label={ariaLabel}
          className={`absolute ${positionClasses} ${alignClasses} min-w-full w-max max-w-[min(22rem,calc(100vw-2rem))] max-h-64 overflow-y-auto bg-white rounded-xl border border-[#dce9ff] shadow-[0_12px_30px_-8px_rgba(11,28,48,0.14)] py-1.5 z-40 animate-in fade-in zoom-in-95 duration-150 ${menuClassName}`}
        >
          {options.map((opt) => {
            const isSelected = opt.value === value;
            return (
              <button
                key={`${opt.value}-${opt.label}`}
                type="button"
                role="option"
                aria-selected={isSelected}
                disabled={opt.disabled}
                onClick={() => {
                  if (opt.disabled) return;
                  onChange(opt.value);
                  setIsOpen(false);
                }}
                className={`w-full px-3.5 py-2 text-left text-xs flex items-center justify-between gap-3 transition-colors ${
                  opt.disabled
                    ? "opacity-40 cursor-not-allowed text-slate-400"
                    : isSelected
                    ? "bg-[#eff4ff] text-[#0045cc] font-bold cursor-pointer"
                    : "text-[#434656] hover:bg-[#f8f9ff] hover:text-[#0b1c30] font-medium cursor-pointer"
                }`}
              >
                <span className="truncate">{opt.label}</span>
                {isSelected && (
                  <Check className="w-3.5 h-3.5 text-[#0045cc] shrink-0" />
                )}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
