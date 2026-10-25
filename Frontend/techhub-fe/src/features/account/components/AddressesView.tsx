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
} from "lucide-react";
import { MOCK_ADDRESSES, type AddressDTO } from "@/mocks";
import { AddressModal } from "./AddressModal";

export function AddressesView() {
  const [addresses, setAddresses] = useState<AddressDTO[]>(MOCK_ADDRESSES);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingAddress, setEditingAddress] = useState<AddressDTO | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const triggerToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => {
      setToastMessage(null);
    }, 3000);
  };

  const handleOpenAdd = () => {
    if (addresses.length >= 10) {
      triggerToast("Maximum limit reached: You can store up to 10 shipping addresses.");
      return;
    }
    setEditingAddress(null);
    setIsModalOpen(true);
  };

  const handleOpenEdit = (addr: AddressDTO) => {
    setEditingAddress(addr);
    setIsModalOpen(true);
  };

  const handleSetDefault = (id: string) => {
    setAddresses((prev) =>
      prev.map((a) => ({
        ...a,
        isDefault: a.id === id,
      }))
    );
    triggerToast("Default shipping address updated.");
  };

  const handleDelete = (id: string) => {
    const target = addresses.find((a) => a.id === id);
    if (target?.isDefault) {
      triggerToast("Cannot delete default address. Please assign another address as default first.");
      return;
    }
    setAddresses((prev) => prev.filter((a) => a.id !== id));
    triggerToast("Shipping address removed.");
  };

  const handleSaveModal = (data: Partial<AddressDTO>) => {
    if (editingAddress) {
      // Update existing
      setAddresses((prev) =>
        prev.map((item) => {
          if (item.id === editingAddress.id) {
            const updated = { ...item, ...data } as AddressDTO;
            return updated;
          }
          if (data.isDefault) {
            return { ...item, isDefault: false };
          }
          return item;
        })
      );
      triggerToast("Address updated successfully.");
    } else {
      // Add new
      const newAddress: AddressDTO = {
        id: `addr-${Date.now()}`,
        recipientName: data.recipientName || "Alex Morgan",
        phone: data.phone || "(+84) 912 345 678",
        province: data.province || "",
        district: data.district || "",
        ward: data.ward || "",
        streetAddress: data.streetAddress || "",
        label: data.label || "HOME",
        isDefault: addresses.length === 0 ? true : !!data.isDefault,
        phoneTag: data.phoneTag || "Direct Mobile",
      };

      setAddresses((prev) => {
        if (newAddress.isDefault) {
          return [newAddress, ...prev.map((a) => ({ ...a, isDefault: false }))];
        }
        return [...prev, newAddress];
      });
      triggerToast("New address added successfully.");
    }
  };

  return (
    <main className="w-full flex flex-col gap-6">
      {/* Top Controls: Title, Counter & Add Button */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex flex-col gap-0.5">
          <div className="flex items-center gap-2.5">
            <h1 className="font-heading text-xl font-bold text-[#0b1c30]">
              Shipping Addresses
            </h1>
            <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-[#eff4ff] text-[#0045cc]">
              {addresses.length}/10 stored
            </span>
          </div>
          <p className="text-xs text-[#737688]">
            Manage delivery locations for hardware components &amp; certified shipments.
          </p>
        </div>

        <button
          type="button"
          onClick={handleOpenAdd}
          className="px-4 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-semibold transition-all shadow-sm flex items-center gap-2 self-start sm:self-center shrink-0 active:scale-[0.99] cursor-pointer"
        >
          <Plus className="w-4 h-4" />
          <span>Add New Address</span>
        </button>
      </div>

      {/* Address Cards List */}
      <div className="flex flex-col gap-4">
        {addresses.map((addr) => {
          const fullAddressString = `${addr.streetAddress}, ${addr.ward}, ${addr.district}, ${addr.province}, Vietnam`;
          const LabelIcon = addr.label === "OFFICE" ? Building2 : addr.label === "HOME" ? Home : MapPin;

          return (
            <div
              key={addr.id}
              className={`bg-white rounded-2xl p-6 shadow-sm relative overflow-hidden flex flex-col gap-4 border transition-all ${
                addr.isDefault
                  ? "border-t-4 border-t-[#0045cc] border-[#c3c5d9]/30"
                  : "border-[#c3c5d9]/25 hover:border-[#0045cc]/30"
              }`}
            >
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div className="flex items-center gap-2.5 flex-wrap">
                  <h3 className="font-heading text-lg font-bold text-[#0b1c30]">
                    {addr.recipientName}
                  </h3>
                  <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase bg-[#eff4ff] text-[#0045cc]">
                    <LabelIcon className="w-3 h-3" />
                    <span>{addr.label}</span>
                  </span>
                  {addr.isDefault && (
                    <span className="bg-[#0045cc]/10 text-[#0045cc] text-[11px] font-bold px-2.5 py-0.5 rounded-full">
                      DEFAULT ADDRESS
                    </span>
                  )}
                </div>

                <div className="flex items-center gap-2 self-end sm:self-auto">
                  {!addr.isDefault && (
                    <button
                      type="button"
                      onClick={() => handleSetDefault(addr.id)}
                      className="px-3 py-1.5 rounded-lg border border-[#c3c5d9]/40 hover:bg-[#eff4ff] text-[#0b1c30] text-xs font-semibold transition-colors cursor-pointer"
                    >
                      Set as Default
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={() => handleOpenEdit(addr)}
                    className="px-3 py-1.5 rounded-lg hover:bg-[#eff4ff] text-[#434656] hover:text-[#0045cc] text-xs font-semibold transition-colors flex items-center gap-1 cursor-pointer"
                  >
                    <Edit2 className="w-3.5 h-3.5" />
                    <span>Edit</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => handleDelete(addr.id)}
                    disabled={addr.isDefault}
                    title={addr.isDefault ? "Cannot delete default address" : "Delete Address"}
                    className={`p-1.5 rounded-lg transition-colors ${
                      addr.isDefault
                        ? "text-neutral-300 cursor-not-allowed"
                        : "text-[#737688] hover:text-[#ba1a1a] hover:bg-[#ffdad6]/40 cursor-pointer"
                    }`}
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>

              {/* Address details */}
              <div className="flex flex-col gap-2 pt-1 text-xs">
                <div className="flex items-center gap-2 text-[#434656]">
                  <Phone className="w-3.5 h-3.5 text-[#737688] shrink-0" />
                  <span className="font-semibold text-[#0b1c30]">{addr.phone}</span>
                  {addr.phoneTag && (
                    <>
                      <span className="text-[#c3c5d9]">•</span>
                      <span className="text-[#737688]">{addr.phoneTag}</span>
                    </>
                  )}
                </div>

                <div className="flex items-start gap-2 text-[#0b1c30] text-sm">
                  <MapPin className="w-4 h-4 text-[#737688] mt-0.5 shrink-0" />
                  <span className="leading-relaxed text-xs sm:text-sm text-[#434656]">
                    {fullAddressString}
                  </span>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Add / Edit Address Modal */}
      <AddressModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSave={handleSaveModal}
        editingAddress={editingAddress}
      />

      {/* Toast Notification */}
      <div
        className={`fixed bottom-8 right-8 z-50 bg-[#0b1c30] text-white px-5 py-3 rounded-2xl shadow-xl flex items-center gap-3 transform transition-all duration-300 ${
          toastMessage
            ? "translate-y-0 opacity-100 pointer-events-auto"
            : "translate-y-12 opacity-0 pointer-events-none"
        }`}
      >
        <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
        <span className="text-xs font-semibold">{toastMessage}</span>
      </div>
    </main>
  );
}
