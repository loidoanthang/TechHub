"use client";

import React, { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  Sliders,
  Megaphone,
  Truck,
  Loader2,
  AlertCircle,
  CheckCircle2,
  Edit,
  Send,
  Play,
  X,
} from "lucide-react";
import {
  useAdminPoliciesQuery,
  useUpdateAdminPolicyMutation,
  useBroadcastNotificationMutation,
  useSimulateShippingMutation,
} from "../../hooks/useAdminFinance";
import { useAdminDialogA11y } from "../../hooks/useAdminDialogA11y";
import {
  policyUpdateSchema,
  broadcastSchema,
  type PolicyUpdateFormValues,
  type BroadcastFormValues,
} from "../../schemas/financeSettingsSchema";
import type {
  BroadcastNotificationResponse,
  PlatformPolicyResponse,
  ShippingSimulationResponse,
} from "@/types/adminFinance";

type SettingsTab = "POLICIES" | "BROADCAST" | "SHIPPING_SIM";

export function AdminSettingsView() {
  const [activeTab, setActiveTab] = useState<SettingsTab>("POLICIES");
  const [toastMsg, setToastMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Policies
  const policiesQuery = useAdminPoliciesQuery();
  const updatePolicyMutation = useUpdateAdminPolicyMutation();
  const [editingPolicy, setEditingPolicy] =
    useState<PlatformPolicyResponse | null>(null);

  useAdminDialogA11y(Boolean(editingPolicy), () => setEditingPolicy(null));

  const policyForm = useForm<PolicyUpdateFormValues>({
    resolver: zodResolver(policyUpdateSchema),
    defaultValues: { policyKey: "", policyValue: "", description: "" },
  });

  // Broadcast
  const broadcastMutation = useBroadcastNotificationMutation();
  const [broadcastResult, setBroadcastResult] =
    useState<BroadcastNotificationResponse | null>(null);

  const broadcastForm = useForm<BroadcastFormValues>({
    resolver: zodResolver(broadcastSchema),
    defaultValues: {
      targetAudience: "ALL_USERS",
      type: "PROMOTION",
      title: "",
      content: "",
    },
  });

  // Shipping Sim
  const simulateMutation = useSimulateShippingMutation();
  const [forceAdvance, setForceAdvance] = useState(true);
  const [batchSize, setBatchSize] = useState(50);
  const [simResult, setSimResult] = useState<ShippingSimulationResponse | null>(
    null
  );

  const showToast = (msg: string) => {
    setErrorMsg(null);
    setToastMsg(msg);
    setTimeout(() => setToastMsg(null), 4000);
  };

  const openEditPolicy = (policy: PlatformPolicyResponse) => {
    setEditingPolicy(policy);
    policyForm.reset({
      policyKey: policy.policyKey,
      policyValue: policy.policyValue,
      description: policy.description || "",
    });
  };

  const handleSavePolicy = async (values: PolicyUpdateFormValues) => {
    try {
      await updatePolicyMutation.mutateAsync({
        policyKey: values.policyKey,
        policyValue: values.policyValue.trim(),
        description: values.description?.trim() || undefined,
      });
      showToast(`Đã cập nhật chính sách "${values.policyKey}" thành công!`);
      setEditingPolicy(null);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Cập nhật chính sách thất bại.";
      setErrorMsg(msg);
    }
  };

  const handleBroadcastSubmit = async (values: BroadcastFormValues) => {
    setBroadcastResult(null);
    try {
      const res = await broadcastMutation.mutateAsync({
        targetAudience: values.targetAudience,
        type: values.type,
        title: values.title.trim(),
        content: values.content.trim(),
      });
      setBroadcastResult(res.data);
      showToast(
        `Đã phát thông báo thành công tới ${res.data.totalSent} người dùng!`
      );
      broadcastForm.reset({
        targetAudience: values.targetAudience,
        type: values.type,
        title: "",
        content: "",
      });
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Gửi thông báo toàn sàn thất bại.";
      setErrorMsg(msg);
    }
  };

  const handleRunSimulation = async () => {
    setSimResult(null);
    try {
      const res = await simulateMutation.mutateAsync({
        forceAdvance,
        batchSize,
      });
      setSimResult(res.data);
      showToast("Đã chạy tiến trình giả lập giao hàng thành công!");
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Chạy giả lập thất bại.";
      setErrorMsg(msg);
    }
  };

  return (
    <div className="flex-1 w-full p-4 sm:p-6 flex flex-col gap-4 max-w-[1440px] mx-auto">
      {/* Header */}
      <div>
        <h1 className="font-heading text-lg font-bold text-[#0b1c30]">
          Chính sách Sàn & Công cụ Vận hành
        </h1>
        <p className="text-[11px] text-slate-500">
          Cấu hình tham số vận hành hệ thống, phát thông báo toàn sàn và công cụ mô phỏng giao hàng phục vụ demo.
        </p>
      </div>

      {/* Feedbacks */}
      {toastMsg && (
        <div className="p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-semibold flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
          <span>{toastMsg}</span>
        </div>
      )}
      {errorMsg && (
        <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs font-semibold flex items-center gap-2">
          <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
          <span>{errorMsg}</span>
        </div>
      )}

      {/* Tabs */}
      <div
        role="tablist"
        aria-label="Chính sách sàn và công cụ vận hành"
        className="bg-white rounded-2xl border border-slate-200/80 p-2 shadow-2xs flex items-center gap-1.5 overflow-x-auto no-scrollbar"
      >
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === "POLICIES"}
          onClick={() => setActiveTab("POLICIES")}
          className={`px-4 py-2.5 rounded-xl text-xs font-bold flex items-center gap-2 whitespace-nowrap cursor-pointer min-h-[40px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary ${
            activeTab === "POLICIES"
              ? "bg-on-surface text-white"
              : "text-slate-600 hover:bg-slate-100"
          }`}
        >
          <Sliders className="w-4 h-4" />
          <span>Chính sách Giao dịch & Hoa hồng</span>
        </button>

        <button
          type="button"
          role="tab"
          aria-selected={activeTab === "BROADCAST"}
          onClick={() => setActiveTab("BROADCAST")}
          className={`px-4 py-2.5 rounded-xl text-xs font-bold flex items-center gap-2 whitespace-nowrap cursor-pointer min-h-[40px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary ${
            activeTab === "BROADCAST"
              ? "bg-on-surface text-white"
              : "text-slate-600 hover:bg-slate-100"
          }`}
        >
          <Megaphone className="w-4 h-4" />
          <span>Phát Thông báo Toàn sàn</span>
        </button>

        <button
          type="button"
          role="tab"
          aria-selected={activeTab === "SHIPPING_SIM"}
          onClick={() => setActiveTab("SHIPPING_SIM")}
          className={`px-4 py-2.5 rounded-xl text-xs font-bold flex items-center gap-2 whitespace-nowrap cursor-pointer min-h-[40px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary ${
            activeTab === "SHIPPING_SIM"
              ? "bg-on-surface text-white"
              : "text-slate-600 hover:bg-slate-100"
          }`}
        >
          <Truck className="w-4 h-4" />
          <span>Giả lập Giao vận (Demo)</span>
        </button>
      </div>

      {/* TAB 1: POLICIES */}
      {activeTab === "POLICIES" && (
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs overflow-hidden">
          {policiesQuery.isLoading ? (
            <div className="p-12 flex flex-col items-center justify-center gap-3">
              <Loader2 className="w-7 h-7 text-[#0045cc] animate-spin" />
              <span className="text-xs text-slate-500">
                Đang tải cấu hình chính sách...
              </span>
            </div>
          ) : policiesQuery.isError ? (
            <div className="p-8 text-center">
              <button
                type="button"
                onClick={() => policiesQuery.refetch()}
                className="px-4 py-2 rounded-xl bg-[#0045cc] text-white text-xs font-bold"
              >
                Thử lại
              </button>
            </div>
          ) : (
            <div className="overflow-x-auto no-scrollbar">
              <table className="w-full min-w-[700px] text-left text-xs border-collapse">
                <thead>
                  <tr className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold">
                    <th className="py-3 px-4 w-[28%]">Mã Tham số (Key)</th>
                    <th className="py-3 px-3 w-[18%]">Giá trị (Value)</th>
                    <th className="py-3 px-3 w-[34%]">Mô tả nghiệp vụ</th>
                    <th className="py-3 px-4 w-[20%] text-right">Cập nhật</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {(policiesQuery.data || []).map((pol) => (
                    <tr key={pol.id} className="hover:bg-slate-50/70">
                      <td className="py-3.5 px-4 font-mono font-bold text-[#0b1c30]">
                        {pol.policyKey}
                      </td>
                      <td className="py-3.5 px-3">
                        <span className="px-2.5 py-1 rounded-lg bg-[#eff4ff] text-[#0045cc] font-mono font-extrabold">
                          {pol.policyValue}
                        </span>
                      </td>
                      <td className="py-3.5 px-3 text-slate-600">
                        {pol.description || "—"}
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <button
                          type="button"
                          onClick={() => openEditPolicy(pol)}
                          className="px-3 py-1.5 rounded-xl bg-slate-100 hover:bg-[#0045cc] text-slate-700 hover:text-white text-xs font-bold inline-flex items-center gap-1 transition-colors duration-150 cursor-pointer"
                        >
                          <Edit className="w-3.5 h-3.5" />
                          <span>Sửa</span>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* TAB 2: BROADCAST */}
      {activeTab === "BROADCAST" && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          <div className="lg:col-span-7 bg-white rounded-2xl border border-slate-200/80 p-6 shadow-2xs">
            <h2 className="font-heading text-base font-extrabold text-[#0b1c30] mb-1">
              Soạn Thông báo Phát Toàn Sàn (Broadcast)
            </h2>
            <p className="text-xs text-slate-500 mb-5">
              Gửi thông báo trực tiếp tới hộp thư in-app của toàn bộ Buyer hoặc Seller trên hệ thống.
            </p>

            <form
              onSubmit={broadcastForm.handleSubmit(handleBroadcastSubmit)}
              className="flex flex-col gap-4"
            >
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div className="flex flex-col gap-1">
                  <label className="text-xs font-bold text-slate-700">
                    Đối tượng nhận *
                  </label>
                  <select
                    {...broadcastForm.register("targetAudience")}
                    className="h-10 px-3 rounded-xl border border-slate-200 text-xs font-semibold bg-white"
                  >
                    <option value="ALL_USERS">
                      Tất cả Người dùng (ALL_USERS)
                    </option>
                    <option value="ALL_BUYERS">
                      Tất cả Người mua (ALL_BUYERS)
                    </option>
                    <option value="ALL_SELLERS">
                      Tất cả Người bán (ALL_SELLERS)
                    </option>
                  </select>
                </div>

                <div className="flex flex-col gap-1">
                  <label className="text-xs font-bold text-slate-700">
                    Phân loại thông báo *
                  </label>
                  <select
                    {...broadcastForm.register("type")}
                    className="h-10 px-3 rounded-xl border border-slate-200 text-xs font-semibold bg-white"
                  >
                    <option value="PROMOTION">Khuyến mãi (PROMOTION)</option>
                    <option value="SYSTEM">Hệ thống (SYSTEM)</option>
                    <option value="SHOP">Gian hàng (SHOP)</option>
                  </select>
                </div>
              </div>

              <div className="flex flex-col gap-1">
                <label className="text-xs font-bold text-slate-700">
                  Tiêu đề thông báo *
                </label>
                <input
                  type="text"
                  {...broadcastForm.register("title")}
                  placeholder="VD: TechHub Mega Sale 11.11 - Săn Voucher FreeShip 0đ!"
                  className="w-full h-10 px-3.5 rounded-xl border border-slate-200 text-xs"
                />
                {broadcastForm.formState.errors.title && (
                  <p className="text-[11px] text-rose-600">
                    {broadcastForm.formState.errors.title.message}
                  </p>
                )}
              </div>

              <div className="flex flex-col gap-1">
                <label className="text-xs font-bold text-slate-700">
                  Nội dung chi tiết *
                </label>
                <textarea
                  rows={4}
                  {...broadcastForm.register("content")}
                  placeholder="Nhập thông điệp gửi đến người dùng..."
                  className="w-full p-3.5 rounded-xl border border-slate-200 text-xs"
                />
                {broadcastForm.formState.errors.content && (
                  <p className="text-[11px] text-rose-600">
                    {broadcastForm.formState.errors.content.message}
                  </p>
                )}
              </div>

              <div className="flex justify-end pt-2">
                <button
                  type="submit"
                  disabled={broadcastMutation.isPending}
                  className="h-10 px-5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold flex items-center gap-2 cursor-pointer"
                >
                  {broadcastMutation.isPending ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <Send className="w-4 h-4" />
                  )}
                  <span>Phát Thông Báo Ngay</span>
                </button>
              </div>
            </form>
          </div>

          <div className="lg:col-span-5 flex flex-col gap-4">
            <div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-2xs">
              <h3 className="text-xs font-bold text-slate-800 mb-2">
                Kết quả Phát sóng Gần nhất
              </h3>
              {broadcastResult ? (
                <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-xs flex flex-col gap-1.5">
                  <div className="flex justify-between">
                    <span className="text-slate-600">Nhóm nhận:</span>
                    <strong className="text-emerald-800">
                      {broadcastResult.targetAudience}
                    </strong>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-600">Đã gửi thành công:</span>
                    <strong className="font-mono text-emerald-700">
                      {broadcastResult.totalSent} /{" "}
                      {broadcastResult.totalTargetUsers} users
                    </strong>
                  </div>
                  <p className="text-[11px] text-slate-500 mt-1">
                    {broadcastResult.title}
                  </p>
                </div>
              ) : (
                <p className="text-xs text-slate-400">
                  Chưa có phiên phát thông báo nào trong phiên làm việc này.
                </p>
              )}
            </div>
          </div>
        </div>
      )}

      {/* TAB 3: SHIPPING SIMULATOR */}
      {activeTab === "SHIPPING_SIM" && (
        <div className="bg-white rounded-2xl border border-slate-200/80 p-6 shadow-2xs max-w-2xl flex flex-col gap-5">
          <div>
            <h2 className="font-heading text-base font-extrabold text-[#0b1c30]">
              Công cụ Giả lập Tiến trình Giao vận (Demo Tool)
            </h2>
            <p className="text-xs text-slate-500 mt-1 leading-relaxed">
              Thúc đẩy tự động trạng thái các đơn hàng từ{" "}
              <code>WAITING_FOR_PICKUP → SHIPPING → DELIVERED</code> và đồng bộ
              trạng thái thanh toán COD, giúp kiểm thử nhanh toàn bộ vòng đời đơn hàng khi demo.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 bg-slate-50 p-4 rounded-2xl border border-slate-200/70">
            <label className="flex items-center gap-2.5 text-xs font-semibold text-slate-800 cursor-pointer">
              <input
                type="checkbox"
                checked={forceAdvance}
                onChange={(e) => setForceAdvance(e.target.checked)}
                className="w-4 h-4 accent-[#0045cc]"
              />
              <span>Ép chuyển ngay không chờ độ trễ (forceAdvance)</span>
            </label>

            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold text-slate-700">
                Số đơn tối đa / lượt:
              </span>
              <input
                type="number"
                min={1}
                max={200}
                value={batchSize}
                onChange={(e) => setBatchSize(Number(e.target.value) || 50)}
                className="w-24 h-9 px-3 rounded-xl border border-slate-200 bg-white text-xs font-mono font-bold"
              />
            </div>
          </div>

          <div>
            <button
              type="button"
              onClick={handleRunSimulation}
              disabled={simulateMutation.isPending}
              className="h-11 px-6 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold flex items-center gap-2 shadow-xs cursor-pointer"
            >
              {simulateMutation.isPending ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <Play className="w-4 h-4" />
              )}
              <span>Chạy Giả Lập Chuyển Trạng Thái Giao Hàng</span>
            </button>
          </div>

          {simResult && (
            <div className="p-4 rounded-2xl bg-[#eff4ff] border border-[#0045cc]/20 text-xs flex flex-col gap-2">
              <span className="font-bold text-[#0b1c30]">
                {simResult.message}
              </span>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-1">
                <div className="bg-white p-2.5 rounded-xl border border-slate-200">
                  <span className="text-[10px] text-slate-500 block">
                    Pickup → Shipping
                  </span>
                  <strong className="font-mono text-sm text-[#0045cc]">
                    {simResult.processedWaitingOrders}
                  </strong>
                </div>
                <div className="bg-white p-2.5 rounded-xl border border-slate-200">
                  <span className="text-[10px] text-slate-500 block">
                    Shipping → Delivered
                  </span>
                  <strong className="font-mono text-sm text-emerald-700">
                    {simResult.processedShippingOrders}
                  </strong>
                </div>
                <div className="bg-white p-2.5 rounded-xl border border-slate-200">
                  <span className="text-[10px] text-slate-500 block">
                    COD Updated
                  </span>
                  <strong className="font-mono text-sm text-slate-900">
                    {simResult.updatedCodPayments}
                  </strong>
                </div>
                <div className="bg-white p-2.5 rounded-xl border border-slate-200">
                  <span className="text-[10px] text-slate-500 block">
                    Thời gian xử lý
                  </span>
                  <strong className="font-mono text-sm text-slate-700">
                    {simResult.executionDurationMs}ms
                  </strong>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Edit Policy Modal */}
      {editingPolicy && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="edit-policy-title"
          className="fixed inset-0 z-50 flex items-center justify-center p-4"
        >
          <div
            className="fixed inset-0 bg-slate-950/50"
            onClick={() => setEditingPolicy(null)}
          />
          <div className="relative z-10 w-full max-w-md bg-white rounded-3xl p-6 shadow-2xl border border-slate-200">
            <div className="flex items-center justify-between mb-4">
              <h3
                id="edit-policy-title"
                className="font-heading text-sm font-extrabold text-on-surface"
              >
                Sửa Chính sách: {editingPolicy.policyKey}
              </h3>
              <button
                type="button"
                aria-label="Đóng hộp thoại"
                onClick={() => setEditingPolicy(null)}
                className="min-w-[36px] min-h-[36px] inline-flex items-center justify-center rounded-lg text-slate-500 hover:bg-slate-100 cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
            <form
              onSubmit={policyForm.handleSubmit(handleSavePolicy)}
              className="flex flex-col gap-3.5"
            >
              <div className="flex flex-col gap-1">
                <label className="text-xs font-bold text-slate-700">
                  Giá trị cấu hình *
                </label>
                <input
                  type="text"
                  {...policyForm.register("policyValue")}
                  className="h-10 px-3 rounded-xl border border-slate-200 text-xs font-mono font-bold"
                />
                {policyForm.formState.errors.policyValue && (
                  <p className="text-[11px] text-rose-600">
                    {policyForm.formState.errors.policyValue.message}
                  </p>
                )}
              </div>

              <div className="flex flex-col gap-1">
                <label className="text-xs font-bold text-slate-700">
                  Mô tả
                </label>
                <textarea
                  rows={2}
                  {...policyForm.register("description")}
                  className="p-3 rounded-xl border border-slate-200 text-xs"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setEditingPolicy(null)}
                  className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={updatePolicyMutation.isPending}
                  className="px-4 py-2 rounded-xl bg-[#0045cc] text-white text-xs font-bold flex items-center gap-1.5"
                >
                  {updatePolicyMutation.isPending && (
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  )}
                  <span>Lưu Cấu Hình</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
