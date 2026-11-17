"use client";

import React from "react";
import {
  DashboardPeriodFilterBar,
  type DashboardPeriodType,
} from "@/components/shared";
import type { DashboardPeriod } from "@/types/sellerDashboard";

interface DashboardPeriodFilterProps {
  period: DashboardPeriod;
  onPeriodChange: (p: DashboardPeriod) => void;
  startDate: string;
  endDate: string;
  onStartDateChange: (d: string) => void;
  onEndDateChange: (d: string) => void;
  onRefresh: () => void;
  isFetching?: boolean;
  lastUpdated?: string;
}

export function DashboardPeriodFilter({
  period,
  onPeriodChange,
  startDate,
  endDate,
  onStartDateChange,
  onEndDateChange,
  onRefresh,
  isFetching,
  lastUpdated,
}: DashboardPeriodFilterProps) {
  return (
    <DashboardPeriodFilterBar
      period={period as DashboardPeriodType}
      onPeriodChange={(p) => onPeriodChange(p as DashboardPeriod)}
      startDate={startDate}
      endDate={endDate}
      onDateRangeChange={(start, end) => {
        onStartDateChange(start);
        onEndDateChange(end);
      }}
      onRefresh={onRefresh}
      isFetching={isFetching}
      lastUpdated={lastUpdated}
    />
  );
}
