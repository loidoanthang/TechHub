"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { formatCurrencyVND, parseCurrencyVND } from "@/lib/utils";
import {
  Store,
  MapPin,
  Landmark,
  ShieldCheck,
  CheckCircle2,
  Clock,
  XCircle,
  AlertCircle,
  ArrowRight,
  RefreshCw,
} from "lucide-react";
import {
  VIETNAMESE_BANKS,
  SellerVerificationStatus,
} from "@/mocks";
import { VIETNAM_PROVINCES } from "@/mocks/address.mock";
import { SellerHeader } from "./SellerHeader";
import {
  useSellerVerificationStatusQuery,
  useRegisterSellerMutation,
} from "../hooks/useSellerQueries";
import {
  sellerRegistrationSchema,
  type SellerRegistrationFormValues,
} from "../schemas/sellerRegistrationSchema";

export function SellerRegistrationView() {
  const { data: statusData, isLoading: isStatusLoading } = useSellerVerificationStatusQuery();
  const registerSellerMutation = useRegisterSellerMutation();

  // Verification State: "NOT_REGISTERED" | "PENDING" | "APPROVED" | "REJECTED"
  const [verificationStatus, setVerificationStatus] =
    useState<SellerVerificationStatus>("NOT_REGISTERED");
  const [rejectionReason, setRejectionReason] = useState<string | null>(null);
  const [sellerNameEdited, setSellerNameEdited] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // React Hook Form + Zod
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    reset,
    control,
    formState: { errors },
  } = useForm<SellerRegistrationFormValues>({
    resolver: zodResolver(sellerRegistrationSchema),
    defaultValues: {
      shopName: "GearTech Flagship Store",
      sellerName: "geartech-official",
      description:
        "Authorized distributor of high-performance PC components, GPUs, and custom mechanical keyboards.",
      contactName: "Alex Morgan",
      contactPhone: "(+84) 912 345 678",
      province: "Ho Chi Minh City",
      district: "District 1",
      ward: "Ben Nghe Ward",
      streetAddress: "123 Nguyen Hue Boulevard, Tech Tower, Floor 4",
      shippingFee: 30000,
      bankName: VIETNAMESE_BANKS[0].name,
      bankAccountNumber: "0071001234567",
      bankAccountHolder: "ALEX MORGAN",
      agreeTerms: true,
    },
    mode: "onTouched",
  });

  const selectedProvince = watch("province");
  const selectedDistrict = watch("district");
  const currentSellerName = watch("sellerName");
  const currentShopName = watch("shopName");

  // Sync with backend status if available
  useEffect(() => {
    if (statusData) {
      if (statusData.hasApplied && statusData.verificationStatus) {
        setVerificationStatus(statusData.verificationStatus);
        setRejectionReason(statusData.rejectionReason || null);
        if (statusData.shopName) setValue("shopName", statusData.shopName);
        if (statusData.sellerName) setValue("sellerName", statusData.sellerName);
      } else {
        setVerificationStatus("NOT_REGISTERED");
      }
    }
  }, [statusData, setValue]);

  // Handle dynamic districts based on province
  const currentProvinceData = VIETNAM_PROVINCES.find(
    (p) => p.name === selectedProvince
  );
  const currentDistrictData = currentProvinceData?.districts.find(
    (d) => d.name === selectedDistrict
  );

  const handleProvinceChange = (prov: string) => {
    setValue("province", prov, { shouldValidate: true });
    const pData = VIETNAM_PROVINCES.find((p) => p.name === prov);
    if (pData && pData.districts.length > 0) {
      const firstD = pData.districts[0];
      setValue("district", firstD.name, { shouldValidate: true });
      setValue("ward", firstD.wards[0] || "", { shouldValidate: true });
    }
  };

  const handleDistrictChange = (dist: string) => {
    setValue("district", dist, { shouldValidate: true });
    const dData = currentProvinceData?.districts.find((d) => d.name === dist);
    if (dData && dData.wards.length > 0) {
      setValue("ward", dData.wards[0], { shouldValidate: true });
    }
  };

  const handleShopNameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setValue("shopName", val, { shouldValidate: true });
    if (!sellerNameEdited) {
      // Auto slugify
      const slug = val
        .toLowerCase()
        .replace(/[^a-z0-9]/g, "-")
        .replace(/-+/g, "-")
        .replace(/^-|-$/g, "");
      setValue("sellerName", slug, { shouldValidate: true });
    }
  };

  const isSellerNameValid = /^[a-z0-9-]+$/.test(currentSellerName || "") && (currentSellerName?.length || 0) >= 3;

  const onSubmit = async (data: SellerRegistrationFormValues) => {
    setErrorMsg(null);

    try {
      await registerSellerMutation.mutateAsync({
        sellerName: data.sellerName,
        shopName: data.shopName,
        shopDescription: data.description,
        bankName: data.bankName,
        bankAccountNumber: data.bankAccountNumber,
        bankAccountHolder: data.bankAccountHolder,
        warehouseContactName: data.contactName,
        warehousePhone: data.contactPhone,
        province: data.province,
        district: data.district,
        ward: data.ward,
        streetAddress: data.streetAddress,
        shippingFee: data.shippingFee,
      });

      setVerificationStatus("PENDING");
      window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (err: unknown) {
      const errObj = err as { response?: { data?: { message?: string } }; message?: string };
      setErrorMsg(
        errObj?.response?.data?.message ||
        errObj?.message ||
        "Failed to register seller profile. Please verify your details."
      );
    }
  };

  const isSubmitting = registerSellerMutation.isPending;

  return (
    <div className="min-h-screen bg-[#f8f9ff] flex flex-col justify-between">
      <SellerHeader />

      <main className="w-full flex-1 max-w-2xl mx-auto px-4 sm:px-6 py-8 sm:py-10">
        {isStatusLoading ? (
          <div className="w-full h-80 flex items-center justify-center bg-white rounded-3xl border border-[#c3c5d9]/30">
            <RefreshCw className="w-8 h-8 animate-spin text-[#0045cc]" />
          </div>
        ) : (
          <>
            {/* State A: Application Pending Review */}
            {verificationStatus === "PENDING" && (
              <div className="bg-white rounded-3xl p-8 sm:p-12 border border-[#c3c5d9]/30 shadow-sm text-center flex flex-col items-center gap-5 animate-in fade-in duration-300">
                <div className="w-16 h-16 rounded-full bg-[#f59e0b]/10 text-[#d97706] flex items-center justify-center">
                  <Clock className="w-8 h-8" />
                </div>

                <div className="max-w-md">
                  <span className="text-xs font-bold text-[#d97706] uppercase tracking-wider bg-[#fef3c7] px-3 py-1 rounded-full">
                    Application Under Review
                  </span>
                  <h1 className="font-heading text-2xl sm:text-3xl font-extrabold text-[#0b1c30] mt-3">
                    Your Merchant Profile Is Being Verified
                  </h1>
                  <p className="text-xs text-[#737688] mt-2 leading-relaxed">
                    Thank you for applying to sell on TechHub Marketplace! Our compliance team is currently reviewing your shop identity (<strong>{currentShopName}</strong>) and banking records. Verification usually takes 24 business hours.
                  </p>
                </div>

                <div className="w-full max-w-md p-4 rounded-2xl bg-[#eff4ff]/60 border border-[#eff4ff] text-left text-xs text-[#434656] space-y-2">
                  <div className="flex justify-between">
                    <span className="text-[#737688]">Shop Handle:</span>
                    <span className="font-mono font-bold text-[#0045cc]">/shop/{currentSellerName}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-[#737688]">Warehouse Hub:</span>
                    <span className="font-medium text-[#0b1c30]">{selectedDistrict}, {selectedProvince}</span>
                  </div>
                </div>

                <div className="pt-2 flex flex-col sm:flex-row items-center gap-3">
                  <Link
                    href="/"
                    className="px-6 py-2.5 rounded-xl bg-[#eff4ff] hover:bg-[#dce9ff] text-xs font-semibold text-[#0b1c30] transition-colors"
                  >
                    Back to Marketplace
                  </Link>
                </div>
              </div>
            )}

            {/* State B: Application Approved */}
            {verificationStatus === "APPROVED" && (
              <div className="bg-white rounded-3xl p-8 sm:p-12 border border-[#c3c5d9]/30 shadow-sm text-center flex flex-col items-center gap-5 animate-in fade-in duration-300">
                <div className="w-16 h-16 rounded-full bg-[#006b5b]/10 text-[#006b5b] flex items-center justify-center">
                  <CheckCircle2 className="w-8 h-8" />
                </div>

                <div className="max-w-md">
                  <span className="text-xs font-bold text-[#006b5b] uppercase tracking-wider bg-[#006b5b]/10 px-3 py-1 rounded-full">
                    Shop Approved &amp; Active
                  </span>
                  <h1 className="font-heading text-2xl sm:text-3xl font-extrabold text-[#0b1c30] mt-3">
                    Welcome to TechHub Seller Centre!
                  </h1>
                  <p className="text-xs text-[#737688] mt-2 leading-relaxed">
                    Your shop <strong>{currentShopName}</strong> has been officially approved. You can now access your dashboard, upload electronic hardware components, and start fulfilling orders.
                  </p>
                </div>

                <Link
                  href="/seller/dashboard"
                  className="mt-2 px-8 py-3 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold shadow-md transition-all flex items-center gap-2 active:scale-[0.99]"
                >
                  <span>Go to Seller Dashboard</span>
                  <ArrowRight className="w-4 h-4" />
                </Link>
              </div>
            )}

            {/* State C: Application Rejected */}
            {verificationStatus === "REJECTED" && (
              <div className="bg-white rounded-3xl p-8 sm:p-12 border border-[#ba1a1a]/20 shadow-sm text-center flex flex-col items-center gap-5 animate-in fade-in duration-300">
                <div className="w-16 h-16 rounded-full bg-[#ba1a1a]/10 text-[#ba1a1a] flex items-center justify-center">
                  <XCircle className="w-8 h-8" />
                </div>

                <div className="max-w-md">
                  <span className="text-xs font-bold text-[#ba1a1a] uppercase tracking-wider bg-[#ba1a1a]/10 px-3 py-1 rounded-full">
                    Application Needs Revision
                  </span>
                  <h1 className="font-heading text-2xl sm:text-3xl font-extrabold text-[#0b1c30] mt-3">
                    Verification Incomplete
                  </h1>
                  <p className="text-xs text-[#737688] mt-2 leading-relaxed">
                    Our compliance team could not approve your application at this time.
                  </p>
                </div>

                {rejectionReason && (
                  <div className="w-full max-w-md p-4 rounded-2xl bg-[#ffdad6]/40 border border-[#ffdad6] text-left">
                    <p className="text-xs font-bold text-[#ba1a1a] mb-1">Reason for Rejection:</p>
                    <p className="text-xs text-[#434656] leading-relaxed">{rejectionReason}</p>
                  </div>
                )}

                <button
                  type="button"
                  onClick={() => setVerificationStatus("NOT_REGISTERED")}
                  className="px-6 py-2.5 rounded-xl bg-[#0045cc] text-white text-xs font-bold hover:bg-[#0d5bff] transition-all flex items-center gap-2"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                  <span>Update and Resubmit Application</span>
                </button>
              </div>
            )}

            {/* State D: Registration Form */}
            {verificationStatus === "NOT_REGISTERED" && (
              <form onSubmit={handleSubmit(onSubmit)} className="space-y-8 animate-in fade-in duration-300">
                <div className="space-y-2 text-center sm:text-left">
                  <span className="text-xs font-bold text-[#0045cc] tracking-wider uppercase bg-[#0045cc]/10 px-3 py-1 rounded-full">
                    Merchant Onboarding
                  </span>
                  <h1 className="font-heading text-2xl sm:text-3xl font-extrabold text-[#0b1c30] tracking-tight">
                    Register Your TechHub Store
                  </h1>
                  <p className="text-xs text-[#737688]">
                    Set up your storefront profile, warehouse dispatch facility, and escrow bank account in one streamlined registration.
                  </p>
                </div>

                {errorMsg && (
                  <div className="p-4 rounded-2xl bg-[#ba1a1a]/10 border border-[#ba1a1a]/20 text-xs text-[#ba1a1a] flex items-center gap-2.5">
                    <AlertCircle className="w-4 h-4 shrink-0" />
                    <span>{errorMsg}</span>
                  </div>
                )}

                {/* 1. Shop Identity Section */}
                <section className="bg-white rounded-3xl p-6 sm:p-8 border border-[#c3c5d9]/30 shadow-xs flex flex-col gap-5">
                  <div className="flex items-center gap-3 border-b border-[#eff4ff] pb-4">
                    <div className="w-9 h-9 rounded-2xl bg-[#0045cc]/10 text-[#0045cc] flex items-center justify-center">
                      <Store className="w-4 h-4" />
                    </div>
                    <div>
                      <h2 className="font-heading text-base font-bold text-[#0b1c30]">
                        1. Shop &amp; Identity Information
                      </h2>
                      <p className="text-xs text-[#737688]">
                        Brand credentials that appear on your public store and product listings.
                      </p>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {/* Shop Name */}
                    <div className="flex flex-col gap-1.5">
                      <label className="text-xs font-bold text-[#0b1c30]">
                        Display Shop Name <span className="text-[#ba1a1a]">*</span>
                      </label>
                      <input
                        type="text"
                        {...register("shopName")}
                        onChange={handleShopNameChange}
                        placeholder="e.g. GearTech Flagship Store"
                        className="w-full text-xs px-3.5 py-2.5 rounded-xl bg-[#eff4ff]/60 border border-[#c3c5d9]/40 focus:outline-none focus:border-[#0045cc] focus:bg-white text-[#0b1c30] transition-all font-medium"
                      />
                      {errors.shopName?.message && (
                        <p className="text-[11px] text-[#ba1a1a] font-medium">{errors.shopName.message}</p>
                      )}
                    </div>

                    {/* Seller Name (URL Slug) */}
                    <div className="flex flex-col gap-1.5">
                      <div className="flex items-center justify-between">
                        <label className="text-xs font-bold text-[#0b1c30]">
                          Shop Handle (seller_name) <span className="text-[#ba1a1a]">*</span>
                        </label>
                        <span
                          className={`text-[10px] font-semibold ${
                            isSellerNameValid ? "text-[#006b5b]" : "text-[#ba1a1a]"
                          }`}
                        >
                          {isSellerNameValid ? "Valid" : "Only a-z, 0-9, -"}
                        </span>
                      </div>
                      <input
                        type="text"
                        {...register("sellerName")}
                        onChange={(e) => {
                          setValue("sellerName", e.target.value.toLowerCase(), { shouldValidate: true });
                          setSellerNameEdited(true);
                        }}
                        placeholder="e.g. geartech-official"
                        className="w-full text-xs px-3.5 py-2.5 rounded-xl bg-[#eff4ff]/60 border border-[#c3c5d9]/40 focus:outline-none focus:border-[#0045cc] focus:bg-white text-[#0b1c30] font-mono transition-all"
                      />
                      {errors.sellerName?.message && (
                        <p className="text-[11px] text-[#ba1a1a] font-medium">{errors.sellerName.message}</p>
                      )}
                      <span className="text-[10px] text-[#737688]">
                        Public URL: <strong className="text-[#0045cc] font-mono">techhub.vn/shop/{currentSellerName || "your-slug"}</strong>
                      </span>
                    </div>
                  </div>

                  {/* Shop Description */}
                  <div className="flex flex-col gap-1.5">
                    <label className="text-xs font-bold text-[#0b1c30]">
                      Shop Description <span className="text-[#ba1a1a]">*</span>
                    </label>
                    <textarea
                      rows={3}
                      {...register("description")}
                      placeholder="Tell customers about your brand, specialty in electronics, and customer support standards..."
                      className="w-full text-xs p-3.5 rounded-xl bg-[#eff4ff]/60 border border-[#c3c5d9]/40 focus:outline-none focus:border-[#0045cc] focus:bg-white text-[#0b1c30] transition-all resize-none leading-relaxed"
                    />
                    {errors.description?.message && (
                      <p className="text-[11px] text-[#ba1a1a] font-medium">{errors.description.message}</p>
                    )}
                  </div>
                </section>

                {/* 2. Warehouse & Logistics Section */}
                <section className="bg-white rounded-3xl p-6 sm:p-8 border border-[#c3c5d9]/30 shadow-xs flex flex-col gap-5">
                  <div className="flex items-center gap-3 border-b border-[#eff4ff] pb-4">
                    <div className="w-9 h-9 rounded-2xl bg-[#0045cc]/10 text-[#0045cc] flex items-center justify-center">
                      <MapPin className="w-4 h-4" />
                    </div>
                    <div>
                      <h2 className="font-heading text-base font-bold text-[#0b1c30]">
                        2. Warehouse Origin &amp; Logistics Hub
                      </h2>
                      <p className="text-xs text-[#737688]">
                        Pickup location for logistics partners (GHN, ViettelPost) and return fulfillment.
                      </p>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {/* Contact Person */}
                    <div className="flex flex-col gap-1.5">
                      <label className="text-xs font-bold text-[#0b1c30]">
                        Warehouse Contact Name <span className="text-[#ba1a1a]">*</span>
                      </label>
                      <input
                        type="text"
                        {...register("contactName")}
                        placeholder="e.g. Alex Morgan"
                        className="w-full text-xs px-3.5 py-2.5 rounded-xl bg-[#eff4ff]/60 border border-[#c3c5d9]/40 focus:outline-none focus:border-[#0045cc] focus:bg-white text-[#0b1c30]"
                      />
                      {errors.contactName?.message && (
                        <p className="text-[11px] text-[#ba1a1a] font-medium">{errors.contactName.message}</p>
                      )}
                    </div>

                    {/* Contact Phone */}
                    <div className="flex flex-col gap-1.5">
                      <label className="text-xs font-bold text-[#0b1c30]">
                        Warehouse Phone Number <span className="text-[#ba1a1a]">*</span>
                      </label>
                      <input
                        type="tel"
                        {...register("contactPhone")}
                        placeholder="e.g. (+84) 912 345 678"
                        className="w-full text-xs px-3.5 py-2.5 rounded-xl bg-[#eff4ff]/60 border border-[#c3c5d9]/40 focus:outline-none focus:border-[#0045cc] focus:bg-white text-[#0b1c30]"
                      />
                      {errors.contactPhone?.message && (
                        <p className="text-[11px] text-[#ba1a1a] font-medium">{errors.contactPhone.message}</p>
                      )}
                    </div>
                  </div>

                  {/* Province, District, Ward Cascading */}
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                    <div className="flex flex-col gap-1.5">
                      <label className="text-xs font-bold text-[#0b1c30]">Province / City *</label>
                      <select
                        value={selectedProvince}
                        onChange={(e) => handleProvinceChange(e.target.value)}
                        className="w-full text-xs px-3 py-2.5 rounded-xl bg-[#eff4ff]/60 border border-[#c3c5d9]/40 focus:outline-none focus:border-[#0045cc] focus:bg-white text-[#0b1c30] cursor-pointer"
                      >
                        {VIETNAM_PROVINCES.map((p) => (
                          <option key={p.name} value={p.name}>
                            {p.name}
                          </option>
                        ))}
                      </select>
                      {errors.province?.message && (
                        <p className="text-[11px] text-[#ba1a1a] font-medium">{errors.province.message}</p>
                      )}
                    </div>

                    <div className="flex flex-col gap-1.5">
                      <label className="text-xs font-bold text-[#0b1c30]">District *</label>
                      <select
                        value={selectedDistrict}
                        onChange={(e) => handleDistrictChange(e.target.value)}
                        className="w-full text-xs px-3 py-2.5 rounded-xl bg-[#eff4ff]/60 border border-[#c3c5d9]/40 focus:outline-none focus:border-[#0045cc] focus:bg-white text-[#0b1c30] cursor-pointer"
                      >
                        {currentProvinceData?.districts.map((d) => (
                          <option key={d.name} value={d.name}>
                            {d.name}
                          </option>
                        ))}
                      </select>
                      {errors.district?.message && (
                        <p className="text-[11px] text-[#ba1a1a] font-medium">{errors.district.message}</p>
                      )}
                    </div>

                    <div className="flex flex-col gap-1.5">
                      <label className="text-xs font-bold text-[#0b1c30]">Ward *</label>
                      <select
                        {...register("ward")}
                        className="w-full text-xs px-3 py-2.5 rounded-xl bg-[#eff4ff]/60 border border-[#c3c5d9]/40 focus:outline-none focus:border-[#0045cc] focus:bg-white text-[#0b1c30] cursor-pointer"
                      >
                        {currentDistrictData?.wards.map((w) => (
                          <option key={w} value={w}>
                            {w}
                          </option>
                        ))}
                      </select>
                      {errors.ward?.message && (
                        <p className="text-[11px] text-[#ba1a1a] font-medium">{errors.ward.message}</p>
                      )}
                    </div>
                  </div>

                  {/* Street Address */}
                  <div className="flex flex-col gap-1.5">
                    <label className="text-xs font-bold text-[#0b1c30]">
                      Street Address &amp; Warehouse Facility <span className="text-[#ba1a1a]">*</span>
                    </label>
                    <input
                      type="text"
                      {...register("streetAddress")}
                      placeholder="e.g. 123 Nguyen Hue Boulevard, Tech Tower, Floor 4"
                      className="w-full text-xs px-3.5 py-2.5 rounded-xl bg-[#eff4ff]/60 border border-[#c3c5d9]/40 focus:outline-none focus:border-[#0045cc] focus:bg-white text-[#0b1c30]"
                    />
                    {errors.streetAddress?.message && (
                      <p className="text-[11px] text-[#ba1a1a] font-medium">{errors.streetAddress.message}</p>
                    )}
                  </div>

                  {/* Flat Shipping Fee */}
                  <div className="flex flex-col gap-1.5">
                    <label className="text-xs font-bold text-[#0b1c30]">
                      Standard Logistics Flat Rate (VND) <span className="text-[#ba1a1a]">*</span>
                    </label>
                    <Controller
                      name="shippingFee"
                      control={control}
                      render={({ field }) => (
                        <input
                          type="text"
                          inputMode="numeric"
                          value={formatCurrencyVND(field.value, true)}
                          onChange={(e) => {
                            const num = parseCurrencyVND(e.target.value);
                            field.onChange(num);
                          }}
                          className="w-full text-xs font-bold font-mono px-3.5 py-2.5 rounded-xl bg-[#eff4ff]/60 border border-[#c3c5d9]/40 focus:outline-none focus:border-[#0045cc] focus:bg-white text-[#0b1c30]"
                        />
                      )}
                    />
                    {errors.shippingFee?.message && (
                      <p className="text-[11px] text-[#ba1a1a] font-medium">{errors.shippingFee.message}</p>
                    )}
                    <span className="text-[10px] text-[#737688]">
                      Default shipping rate applied per checkout order from this shop.
                    </span>
                  </div>
                </section>

                {/* 3. Escrow Bank & Payout Section */}
                <section className="bg-white rounded-3xl p-6 sm:p-8 border border-[#c3c5d9]/30 shadow-xs flex flex-col gap-5">
                  <div className="flex items-center gap-3 border-b border-[#eff4ff] pb-4">
                    <div className="w-9 h-9 rounded-2xl bg-[#0045cc]/10 text-[#0045cc] flex items-center justify-center">
                      <Landmark className="w-4 h-4" />
                    </div>
                    <div>
                      <h2 className="font-heading text-base font-bold text-[#0b1c30]">
                        3. Vietnamese Banking &amp; Payout Escrow
                      </h2>
                      <p className="text-xs text-[#737688]">
                        Revenue from completed customer orders is disbursed automatically to this bank account.
                      </p>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    {/* Bank Selection */}
                    <div className="flex flex-col gap-1.5">
                      <label className="text-xs font-bold text-[#0b1c30]">
                        Bank Institution <span className="text-[#ba1a1a]">*</span>
                      </label>
                      <select
                        {...register("bankName")}
                        className="w-full text-xs px-3 py-2.5 rounded-xl bg-[#eff4ff]/60 border border-[#c3c5d9]/40 focus:outline-none focus:border-[#0045cc] focus:bg-white text-[#0b1c30] cursor-pointer"
                      >
                        {VIETNAMESE_BANKS.map((b) => (
                          <option key={b.code} value={b.name}>
                            {b.name} ({b.code})
                          </option>
                        ))}
                      </select>
                      {errors.bankName?.message && (
                        <p className="text-[11px] text-[#ba1a1a] font-medium">{errors.bankName.message}</p>
                      )}
                    </div>

                    {/* Account Number */}
                    <div className="flex flex-col gap-1.5">
                      <label className="text-xs font-bold text-[#0b1c30]">
                        Bank Account Number <span className="text-[#ba1a1a]">*</span>
                      </label>
                      <input
                        type="text"
                        {...register("bankAccountNumber")}
                        placeholder="e.g. 0071001234567"
                        className="w-full text-xs px-3.5 py-2.5 rounded-xl bg-[#eff4ff]/60 border border-[#c3c5d9]/40 focus:outline-none focus:border-[#0045cc] focus:bg-white text-[#0b1c30] font-mono"
                      />
                      {errors.bankAccountNumber?.message && (
                        <p className="text-[11px] text-[#ba1a1a] font-medium">{errors.bankAccountNumber.message}</p>
                      )}
                    </div>

                    {/* Account Holder Name */}
                    <div className="flex flex-col gap-1.5">
                      <label className="text-xs font-bold text-[#0b1c30]">
                        Account Holder Name (Uppercase) <span className="text-[#ba1a1a]">*</span>
                      </label>
                      <input
                        type="text"
                        {...register("bankAccountHolder")}
                        placeholder="e.g. ALEX MORGAN"
                        className="w-full text-xs px-3.5 py-2.5 rounded-xl bg-[#eff4ff]/60 border border-[#c3c5d9]/40 focus:outline-none focus:border-[#0045cc] focus:bg-white text-[#0b1c30] font-mono font-bold tracking-wider"
                      />
                      {errors.bankAccountHolder?.message && (
                        <p className="text-[11px] text-[#ba1a1a] font-medium">{errors.bankAccountHolder.message}</p>
                      )}
                    </div>
                  </div>

                  {/* Terms Checkbox */}
                  <label className="flex items-start gap-3 p-4 rounded-2xl bg-[#eff4ff]/60 border border-[#eff4ff] cursor-pointer mt-2">
                    <input
                      type="checkbox"
                      {...register("agreeTerms")}
                      className="mt-0.5 accent-[#0045cc] rounded"
                    />
                    <div className="flex flex-col gap-0.5">
                      <span className="text-xs text-[#434656] leading-relaxed">
                        I certify that all provided details and merchant credentials are authentic. I agree to comply with <strong>TechHub Escrow Protection Regulations</strong>, hardware warranty mandates, and the 7-day customer dispute policy.
                      </span>
                      {errors.agreeTerms?.message && (
                        <p className="text-[11px] text-[#ba1a1a] font-medium mt-1">{errors.agreeTerms.message}</p>
                      )}
                    </div>
                  </label>
                </section>

                {/* Submission Actions */}
                <div className="flex flex-col sm:flex-row items-center justify-between gap-4 py-2">
                  <Link
                    href="/"
                    className="text-xs font-semibold text-[#737688] hover:text-[#0b1c30] transition-colors"
                  >
                    Cancel and Return to Marketplace
                  </Link>

                  <button
                    type="submit"
                    disabled={isSubmitting}
                    className="w-full sm:w-auto px-8 py-3.5 rounded-2xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold shadow-md hover:shadow-lg transition-all flex items-center justify-center gap-2 cursor-pointer active:scale-[0.99] disabled:opacity-50"
                  >
                    {isSubmitting ? (
                      <>
                        <RefreshCw className="w-4 h-4 animate-spin" />
                        <span>Submitting Application...</span>
                      </>
                    ) : (
                      <>
                        <ShieldCheck className="w-4 h-4" />
                        <span>Submit Seller Registration</span>
                      </>
                    )}
                  </button>
                </div>
              </form>
            )}
          </>
        )}
      </main>

      <footer className="border-t border-[#c3c5d9]/30 bg-white py-6">
        <div className="max-w-6xl mx-auto px-4 text-center text-xs text-[#737688]">
          © 2026 TechHub Electronics Marketplace • Merchant Verification &amp; Compliance Center
        </div>
      </footer>
    </div>
  );
}
