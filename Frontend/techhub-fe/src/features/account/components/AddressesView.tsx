"use client";

import { useState } from "react";
import {
  Plus,
  Edit2,
  Trash2,
  Phone,
  MapPin,
  Home,
  Building2,
  CheckCircle2,
  Loader2,
  AlertCircle,
  AlertTriangle,
  X,
} from "lucide-react";
import {
  useAddressesQuery,
  useCreateAddressMutation,
  useUpdateAddressMutation,
  useDeleteAddressMutation,
  useSetDefaultAddressMutation,
} from "../hooks/useAccountQueries";
import type { AddressResponse } from "@/types/address";
import type { AddressLabel } from "@/types/enums";
import { AddressModal } from "./AddressModal";

function formatBackendErrorMessage(rawMessage?: string): string {
  if (!rawMessage) return "Đã xảy ra lỗi. Vui lòng thử lại.";
  const lower = rawMessage.toLowerCase();
  if (lower.includes("invalid vietnamese phone number")) {
    return "Số điện thoại không hợp lệ (Bắt đầu bằng 03, 05, 07, 08, 09 hoặc +84 và gồm 10 chữ số).";
  }
  if (lower.includes("limit") || lower.includes("maximum 10")) {
    return "Đã đạt giới hạn tối đa: Bạn chỉ có thể lưu tối đa 10 địa chỉ nhận hàng.";
  }
  if (lower.includes("cannot delete default")) {
    return "Không thể xóa địa chỉ mặc định. Vui lòng đặt một địa chỉ khác làm mặc định trước.";
  }
  if (lower.includes("cannot unset default")) {
    return "Không thể hủy mặc định. Vui lòng thiết lập một địa chỉ khác làm mặc định.";
  }
  if (lower.includes("recipient name")) {
    return "Họ và tên người nhận không được để trống hoặc vượt quá 150 ký tự.";
  }
  if (lower.includes("street address")) {
    return "Địa chỉ cụ thể không được để trống hoặc vượt quá 500 ký tự.";
  }
  return rawMessage;
}

export function AddressesView() {
  const { data: addresses = [], isLoading, error, refetch } = useAddressesQuery();
  const createAddressMutation = useCreateAddressMutation();
  const updateAddressMutation = useUpdateAddressMutation();
  const deleteAddressMutation = useDeleteAddressMutation();
  const setDefaultAddressMutation = useSetDefaultAddressMutation();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingAddress, setEditingAddress] = useState<AddressResponse | null>(null);
  const [modalServerError, setModalServerError] = useState<string | null>(null);
  const [addressToDelete, setAddressToDelete] = useState<AddressResponse | null>(null);
  const [toast, setToast] = useState<{ message: string; type?: "success" | "error" } | null>(null);

  const triggerToast = (msg: string, type: "success" | "error" = "success") => {
    setToast({ message: msg, type });
    setTimeout(() => {
      setToast(null);
    }, 4000);
  };

  const handleOpenAdd = () => {
    if (addresses.length >= 10) {
      triggerToast(
        "Đã đạt giới hạn tối đa: Bạn chỉ có thể lưu tối đa 10 địa chỉ nhận hàng.",
        "error"
      );
      return;
    }
    setEditingAddress(null);
    setModalServerError(null);
    setIsModalOpen(true);
  };

  const handleOpenEdit = (addr: AddressResponse) => {
    setEditingAddress(addr);
    setModalServerError(null);
    setIsModalOpen(true);
  };

  const handleSetDefault = async (id: string) => {
    try {
      await setDefaultAddressMutation.mutateAsync(id);
      triggerToast("Đã cập nhật địa chỉ giao hàng mặc định.");
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      triggerToast(
        formatBackendErrorMessage(axiosErr?.response?.data?.message) || "Không thể đặt địa chỉ mặc định.",
        "error"
      );
    }
  };

  const confirmDelete = async () => {
    if (!addressToDelete) return;
    if (addressToDelete.isDefault) {
      triggerToast(
        "Không thể xóa địa chỉ mặc định. Vui lòng chỉ định một địa chỉ khác làm mặc định trước.",
        "error"
      );
      setAddressToDelete(null);
      return;
    }
    try {
      await deleteAddressMutation.mutateAsync(addressToDelete.id);
      triggerToast("Đã xóa địa chỉ nhận hàng thành công.");
      setAddressToDelete(null);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      triggerToast(
        formatBackendErrorMessage(axiosErr?.response?.data?.message) || "Không thể xóa địa chỉ.",
        "error"
      );
    }
  };

  const handleSaveModal = async (data: {
    recipientName: string;
    phone: string;
    province: string;
    district: string;
    ward: string;
    streetAddress: string;
    label: AddressLabel;
    isDefault?: boolean;
  }) => {
    setModalServerError(null);
    try {
      if (editingAddress) {
        await updateAddressMutation.mutateAsync({
          id: editingAddress.id,
          data: {
            recipientName: data.recipientName,
            phone: data.phone,
            province: data.province,
            district: data.district,
            ward: data.ward,
            streetAddress: data.streetAddress,
            label: data.label,
            isDefault: data.isDefault,
          },
        });
        triggerToast("Cập nhật địa chỉ nhận hàng thành công.");
      } else {
        await createAddressMutation.mutateAsync({
          recipientName: data.recipientName,
          phone: data.phone,
          province: data.province,
          district: data.district,
          ward: data.ward,
          streetAddress: data.streetAddress,
          label: data.label,
          isDefault: addresses.length === 0 ? true : data.isDefault,
        });
        triggerToast("Thêm địa chỉ nhận hàng mới thành công.");
      }
      setIsModalOpen(false);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      const friendlyMessage = formatBackendErrorMessage(axiosErr?.response?.data?.message);
      setModalServerError(friendlyMessage);
    }
  };

  if (isLoading) {
    return (
      <div className="w-full h-80 flex flex-col items-center justify-center gap-3 bg-white rounded-2xl border border-[#c3c5d9]/25">
        <Loader2 className="w-8 h-8 animate-spin text-[#0045cc]" />
        <span className="text-xs text-slate-500 font-medium">Đang tải danh sách địa chỉ...</span>
      </div>
    );
  }

  return (
    <main className="w-full flex flex-col gap-6">
      {/* Top Controls: Title, Counter & Add Button */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex flex-col gap-0.5">
          <div className="flex items-center gap-2.5">
            <h1 className="font-heading text-xl font-bold text-[#0b1c30]">
              Sổ địa chỉ nhận hàng
            </h1>
            <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-[#eff4ff] text-[#0045cc] border border-[#0045cc]/20">
              {addresses.length}/10 đã lưu
            </span>
          </div>
          <p className="text-xs text-[#737688]">
            Quản lý các địa chỉ giao nhận cho các đơn mua thiết bị công nghệ &amp; linh kiện phần cứng.
          </p>
        </div>

        <button
          type="button"
          onClick={handleOpenAdd}
          disabled={addresses.length >= 10}
          className="px-4 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold transition-all shadow-sm flex items-center gap-2 self-start sm:self-center shrink-0 active:scale-[0.99] cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <Plus className="w-4 h-4" />
          <span>Thêm địa chỉ mới</span>
        </button>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-xs flex items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0 text-amber-600" />
            <span>Không thể kết nối để tải danh sách địa chỉ từ máy chủ.</span>
          </div>
          <button
            type="button"
            onClick={() => refetch()}
            className="px-3 py-1 bg-amber-200/60 hover:bg-amber-200 text-amber-900 rounded-lg font-semibold text-xs transition-colors cursor-pointer"
          >
            Thử lại
          </button>
        </div>
      )}

      {/* Address Cards List */}
      <div className="flex flex-col gap-4">
        {addresses.length === 0 ? (
          <div className="bg-white rounded-2xl p-10 text-center border border-[#c3c5d9]/25 flex flex-col items-center justify-center gap-3">
            <div className="w-14 h-14 rounded-full bg-[#eff4ff] text-[#0045cc] flex items-center justify-center mb-1">
              <MapPin className="w-7 h-7" />
            </div>
            <div className="text-base font-bold text-[#0b1c30]">Bạn chưa có địa chỉ nhận hàng nào</div>
            <p className="text-xs text-[#737688] max-w-sm">
              Lưu địa chỉ nhận hàng giúp quá trình đặt hàng và giao nhận linh kiện công nghệ nhanh chóng hơn.
            </p>
            <button
              onClick={handleOpenAdd}
              className="mt-2 px-5 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold transition-all shadow-sm flex items-center gap-2 cursor-pointer"
            >
              <Plus className="w-4 h-4" />
              <span>Thêm địa chỉ đầu tiên</span>
            </button>
          </div>
        ) : (
          addresses.map((addr) => {
            const fullAddressString =
              addr.fullAddress ||
              `${addr.streetAddress}, ${addr.ward}, ${addr.district}, ${addr.province}`;
            const LabelIcon =
              addr.label === "OFFICE" ? Building2 : addr.label === "HOME" ? Home : MapPin;
            const labelText =
              addr.label === "OFFICE"
                ? "Văn phòng"
                : addr.label === "HOME"
                ? "Nhà riêng"
                : "Khác";

            return (
              <div
                key={addr.id}
                className={`bg-white rounded-2xl p-5 sm:p-6 shadow-sm relative overflow-hidden flex flex-col gap-4 border transition-all ${
                  addr.isDefault
                    ? "border-l-4 border-l-[#0045cc] border-[#0045cc]/30 bg-gradient-to-r from-blue-50/20 via-white to-white"
                    : "border-[#c3c5d9]/25 hover:border-[#0045cc]/30"
                }`}
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div className="flex items-center gap-2.5 flex-wrap">
                    <h3 className="font-heading text-base sm:text-lg font-bold text-[#0b1c30]">
                      {addr.recipientName}
                    </h3>

                    {/* Tag Label */}
                    <span
                      className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold tracking-wide ${
                        addr.label === "HOME"
                          ? "bg-blue-50 text-[#0045cc] border border-blue-100"
                          : addr.label === "OFFICE"
                          ? "bg-purple-50 text-purple-700 border border-purple-100"
                          : "bg-slate-100 text-slate-700 border border-slate-200"
                      }`}
                    >
                      <LabelIcon className="w-3 h-3" />
                      {labelText}
                    </span>

                    {/* Default Badge */}
                    {addr.isDefault && (
                      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-[#eff4ff] text-[#0045cc] border border-[#0045cc]/20">
                        <CheckCircle2 className="w-3 h-3 text-[#0045cc]" />
                        Mặc định
                      </span>
                    )}
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-1.5 self-end sm:self-auto">
                    <button
                      type="button"
                      onClick={() => handleOpenEdit(addr)}
                      className="p-2 rounded-xl text-slate-500 hover:text-[#0045cc] hover:bg-[#eff4ff] transition-colors cursor-pointer flex items-center gap-1 text-xs font-semibold"
                      title="Chỉnh sửa địa chỉ"
                    >
                      <Edit2 className="w-3.5 h-3.5" />
                      <span className="hidden sm:inline">Sửa</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => setAddressToDelete(addr)}
                      className={`p-2 rounded-xl transition-colors cursor-pointer flex items-center gap-1 text-xs font-semibold ${
                        addr.isDefault
                          ? "text-slate-300 cursor-not-allowed"
                          : "text-slate-500 hover:text-rose-600 hover:bg-rose-50"
                      }`}
                      title={
                        addr.isDefault
                          ? "Địa chỉ mặc định không thể xóa"
                          : "Xóa địa chỉ này"
                      }
                      disabled={addr.isDefault || deleteAddressMutation.isPending}
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                      <span className="hidden sm:inline">Xóa</span>
                    </button>
                  </div>
                </div>

                {/* Details Section */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs pt-2 border-t border-slate-100">
                  <div className="flex items-start gap-2.5 text-[#0b1c30]">
                    <Phone className="w-4 h-4 text-[#0045cc] mt-0.5 shrink-0" />
                    <div className="flex flex-col gap-0.5">
                      <span className="font-bold text-sm tracking-wide">{addr.phone}</span>
                      <span className="text-[#737688] text-[11px]">Số điện thoại liên hệ</span>
                    </div>
                  </div>

                  <div className="flex items-start gap-2.5 text-[#0b1c30]">
                    <MapPin className="w-4 h-4 text-[#0045cc] mt-0.5 shrink-0" />
                    <div className="flex flex-col gap-0.5">
                      <span className="leading-relaxed font-medium text-slate-800">
                        {fullAddressString}
                      </span>
                      <span className="text-[#737688] text-[11px]">Địa chỉ giao nhận hàng</span>
                    </div>
                  </div>
                </div>

                {/* Card Footer: Set Default Action */}
                {!addr.isDefault && (
                  <div className="pt-2 flex justify-end border-t border-slate-50">
                    <button
                      type="button"
                      onClick={() => handleSetDefault(addr.id)}
                      disabled={setDefaultAddressMutation.isPending}
                      className="text-xs font-bold text-[#0045cc] hover:underline cursor-pointer disabled:opacity-50"
                    >
                      Thiết lập làm địa chỉ mặc định
                    </button>
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>

      {/* Address Create/Edit Modal */}
      <AddressModal
        isOpen={isModalOpen}
        onClose={() => {
          setIsModalOpen(false);
          setModalServerError(null);
        }}
        onSave={handleSaveModal}
        editingAddress={editingAddress}
        isLoading={createAddressMutation.isPending || updateAddressMutation.isPending}
        serverError={modalServerError}
        isFirstAddress={addresses.length === 0}
      />

      {/* Delete Confirmation Modal */}
      {addressToDelete && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm animate-in fade-in duration-200"
        >
          <div
            className="fixed inset-0"
            onClick={() => !deleteAddressMutation.isPending && setAddressToDelete(null)}
            aria-hidden="true"
          />
          <div className="relative z-10 w-full max-w-md bg-white rounded-2xl shadow-2xl border border-slate-200/80 p-6 flex flex-col gap-4">
            <div className="flex items-center justify-between pb-2 border-b border-slate-100">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-full bg-rose-50 text-rose-600 flex items-center justify-center shrink-0">
                  <AlertTriangle className="w-4 h-4" />
                </div>
                <h3 className="font-heading text-base font-bold text-slate-900">
                  Xác nhận xóa địa chỉ
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setAddressToDelete(null)}
                disabled={deleteAddressMutation.isPending}
                className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors cursor-pointer"
                aria-label="Đóng"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <p className="text-xs text-slate-600 leading-relaxed">
              Bạn có chắc chắn muốn xóa địa chỉ của người nhận{" "}
              <strong className="text-slate-900">{addressToDelete.recipientName}</strong> (
              <span className="text-slate-700">
                {addressToDelete.streetAddress}, {addressToDelete.ward}, {addressToDelete.district},{" "}
                {addressToDelete.province}
              </span>
              )? Thao tác này không thể hoàn tác.
            </p>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setAddressToDelete(null)}
                disabled={deleteAddressMutation.isPending}
                className="px-4 py-2 rounded-xl border border-slate-200 text-slate-700 text-xs font-semibold hover:bg-slate-50 transition-colors cursor-pointer disabled:opacity-50"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={confirmDelete}
                disabled={deleteAddressMutation.isPending}
                className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold shadow-sm transition-all cursor-pointer flex items-center gap-2 disabled:opacity-50"
              >
                {deleteAddressMutation.isPending && (
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                )}
                <span>{deleteAddressMutation.isPending ? "Đang xóa..." : "Xóa vĩnh viễn"}</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Floating Toast Notification */}
      {toast && (
        <div
          className={`fixed bottom-8 right-8 z-50 text-white px-5 py-3 rounded-2xl shadow-xl flex items-center gap-3 animate-in fade-in slide-in-from-bottom-4 duration-300 ${
            toast.type === "error"
              ? "bg-rose-900 border border-rose-700"
              : "bg-[#0b1c30] border border-slate-700"
          }`}
        >
          {toast.type === "error" ? (
            <AlertCircle className="w-5 h-5 text-rose-400 shrink-0" />
          ) : (
            <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
          )}
          <span className="text-xs font-semibold max-w-sm leading-snug">
            {toast.message}
          </span>
          <button
            type="button"
            onClick={() => setToast(null)}
            className="text-slate-400 hover:text-white ml-1 p-0.5"
            aria-label="Đóng thông báo"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}
    </main>
  );
}
