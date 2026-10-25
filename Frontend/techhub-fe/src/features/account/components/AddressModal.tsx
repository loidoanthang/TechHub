"use client";

import { useState, useEffect } from "react";
import { X, Home, Building2, MapPin } from "lucide-react";
import { type AddressDTO } from "@/mocks";

interface AddressModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (address: Partial<AddressDTO>) => void;
  editingAddress?: AddressDTO | null;
}

export function AddressModal({
  isOpen,
  onClose,
  onSave,
  editingAddress,
}: AddressModalProps) {
  const [recipientName, setRecipientName] = useState("");
  const [phone, setPhone] = useState("");
  const [province, setProvince] = useState("");
  const [district, setDistrict] = useState("");
  const [ward, setWard] = useState("");
  const [streetAddress, setStreetAddress] = useState("");
  const [label, setLabel] = useState<"HOME" | "OFFICE" | "OTHER">("HOME");
  const [isDefault, setIsDefault] = useState(false);

  useEffect(() => {
    if (editingAddress) {
      setRecipientName(editingAddress.recipientName);
      setPhone(editingAddress.phone);
      setProvince(editingAddress.province);
      setDistrict(editingAddress.district);
      setWard(editingAddress.ward);
      setStreetAddress(editingAddress.streetAddress);
      setLabel(editingAddress.label);
      setIsDefault(editingAddress.isDefault);
    } else {
      setRecipientName("Alex Morgan");
      setPhone("(+84) 912 345 678");
      setProvince("");
      setDistrict("");
      setWard("");
      setStreetAddress("");
      setLabel("HOME");
      setIsDefault(false);
    }
  }, [editingAddress, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSave({
      id: editingAddress ? editingAddress.id : `addr-${Date.now()}`,
      recipientName,
      phone,
      province,
      district,
      ward,
      streetAddress,
      label,
      isDefault,
      phoneTag: label === "OFFICE" ? "Ext. 409 (Work Desk)" : "Direct Mobile",
    });
    onClose();
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm animate-in fade-in duration-200"
    >
      <div
        className="fixed inset-0"
        onClick={onClose}
        aria-hidden="true"
      />

      <div className="relative z-10 w-full max-w-lg bg-white rounded-2xl shadow-2xl border border-slate-200/80 p-6 sm:p-7 flex flex-col gap-5 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100">
          <h2 className="text-xl font-bold text-slate-900 font-heading">
            {editingAddress ? "Edit Shipping Address" : "Add New Address"}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors cursor-pointer"
            aria-label="Close modal"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {/* Recipient Full Name */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-slate-700">
              Recipient Full Name
            </label>
            <input
              type="text"
              value={recipientName}
              onChange={(e) => setRecipientName(e.target.value)}
              placeholder="e.g. Alex Morgan"
              required
              className="rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm text-slate-900 focus:outline-none focus:border-[#0d5bff] focus:ring-2 focus:ring-[#0d5bff]/15 transition-all"
            />
          </div>

          {/* Phone Number */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-slate-700">
              Phone Number
            </label>
            <input
              type="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="e.g. (+84) 912 345 678"
              required
              className="rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm text-slate-900 focus:outline-none focus:border-[#0d5bff] focus:ring-2 focus:ring-[#0d5bff]/15 transition-all"
            />
          </div>

          {/* Province / City & District Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-semibold text-slate-700">
                City / Province
              </label>
              <input
                type="text"
                value={province}
                onChange={(e) => setProvince(e.target.value)}
                placeholder="e.g. Ho Chi Minh City"
                required
                className="rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm text-slate-900 focus:outline-none focus:border-[#0d5bff] focus:ring-2 focus:ring-[#0d5bff]/15 transition-all"
              />
            </div>

            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-semibold text-slate-700">
                District / County
              </label>
              <input
                type="text"
                value={district}
                onChange={(e) => setDistrict(e.target.value)}
                placeholder="e.g. District 1"
                required
                className="rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm text-slate-900 focus:outline-none focus:border-[#0d5bff] focus:ring-2 focus:ring-[#0d5bff]/15 transition-all"
              />
            </div>
          </div>

          {/* Ward / Sub-district */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-slate-700">
              Ward / Sub-district
            </label>
            <input
              type="text"
              value={ward}
              onChange={(e) => setWard(e.target.value)}
              placeholder="e.g. Da Kao Ward"
              required
              className="rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm text-slate-900 focus:outline-none focus:border-[#0d5bff] focus:ring-2 focus:ring-[#0d5bff]/15 transition-all"
            />
          </div>

          {/* Street Address */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-slate-700">
              Street Address &amp; House Number
            </label>
            <input
              type="text"
              value={streetAddress}
              onChange={(e) => setStreetAddress(e.target.value)}
              placeholder="e.g. 123 Dien Bien Phu Street, Floor 12"
              required
              className="rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm text-slate-900 focus:outline-none focus:border-[#0d5bff] focus:ring-2 focus:ring-[#0d5bff]/15 transition-all"
            />
          </div>

          {/* Address Type Tag */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-slate-700">
              Address Type
            </label>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setLabel("HOME")}
                className={`px-3.5 py-1.5 rounded-lg text-xs font-medium flex items-center gap-1.5 transition-all cursor-pointer ${
                  label === "HOME"
                    ? "bg-[#0d5bff]/10 text-[#0045cc] border border-[#0d5bff]/30 font-semibold"
                    : "border border-slate-200 text-slate-600 hover:bg-slate-50"
                }`}
              >
                <Home className="w-3.5 h-3.5" />
                <span>Home</span>
              </button>

              <button
                type="button"
                onClick={() => setLabel("OFFICE")}
                className={`px-3.5 py-1.5 rounded-lg text-xs font-medium flex items-center gap-1.5 transition-all cursor-pointer ${
                  label === "OFFICE"
                    ? "bg-[#0d5bff]/10 text-[#0045cc] border border-[#0d5bff]/30 font-semibold"
                    : "border border-slate-200 text-slate-600 hover:bg-slate-50"
                }`}
              >
                <Building2 className="w-3.5 h-3.5" />
                <span>Office</span>
              </button>

              <button
                type="button"
                onClick={() => setLabel("OTHER")}
                className={`px-3.5 py-1.5 rounded-lg text-xs font-medium flex items-center gap-1.5 transition-all cursor-pointer ${
                  label === "OTHER"
                    ? "bg-[#0d5bff]/10 text-[#0045cc] border border-[#0d5bff]/30 font-semibold"
                    : "border border-slate-200 text-slate-600 hover:bg-slate-50"
                }`}
              >
                <MapPin className="w-3.5 h-3.5" />
                <span>Other</span>
              </button>
            </div>
          </div>

          {/* Set as Default Checkbox */}
          <div className="flex items-start gap-2.5 pt-1">
            <input
              id="default-address-checkbox"
              type="checkbox"
              checked={isDefault}
              onChange={(e) => setIsDefault(e.target.checked)}
              disabled={editingAddress?.isDefault}
              className="mt-0.5 h-4 w-4 rounded border-slate-300 text-[#0045cc] focus:ring-[#0045cc] accent-[#0045cc] cursor-pointer"
            />
            <label
              htmlFor="default-address-checkbox"
              className="text-xs text-slate-700 cursor-pointer select-none leading-relaxed"
            >
              Set as default shipping address
              {editingAddress?.isDefault && (
                <span className="text-[11px] text-slate-400 block">
                  (This is currently your default shipping address)
                </span>
              )}
            </label>
          </div>

          {/* Modal Actions */}
          <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              className="px-5 py-2.5 rounded-xl border border-slate-200 text-slate-700 text-xs font-semibold hover:bg-slate-50 transition-colors cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-6 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-semibold shadow-sm transition-all cursor-pointer active:scale-[0.99]"
            >
              {editingAddress ? "Update Address" : "Save Address"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
