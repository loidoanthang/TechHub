"use client";

import { useState, useEffect, useMemo } from "react";
import Link from "next/link";
import Image from "next/image";
import {
  Store,
  Trash2,
  Plus,
  Minus,
  Tag,
  MapPin,
  Truck,
  CreditCard,
  ShieldCheck,
  CheckCircle2,
  ArrowRight,
  Loader2,
  AlertCircle,
  PlusCircle,
  ChevronRight,
  Sparkles,
} from "lucide-react";
import { useCart } from "@/features/cart/context/CartContext";
import { useAddressesQuery } from "@/features/account/hooks/useAccountQueries";
import { usePlatformVouchersQuery } from "@/features/checkout/hooks/useVouchers";
import { useCheckoutPreview } from "@/features/checkout/hooks/useCheckoutPreview";
import { VoucherSelectModal } from "@/features/checkout/components/VoucherSelectModal";
import { checkoutApi } from "@/features/checkout/api/checkoutApi";
import { resolveSellerSlug } from "@/features/shops";
import { ROUTES } from "@/lib/constants";
import type { PaymentMethod } from "@/types/enums";
import type { PlaceOrderResponse, ShopOrderRequest } from "@/types/checkout";
import type { CartPlatformVoucherResponse } from "@/types/voucher";

const isUuid = (val: string) =>
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(val);

export function OnePageCartCheckoutView() {
  const { items, updateQuantity, removeItem, totalCount, refetchCart } = useCart();
  const { data: addresses = [], isLoading: isLoadingAddresses } = useAddressesQuery();

  // Selection state
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [hasInitializedSelection, setHasInitializedSelection] = useState(false);

  // Address selection state
  const [selectedAddressId, setSelectedAddressId] = useState<string>("");

  // Voucher state
  const [isVoucherModalOpen, setIsVoucherModalOpen] = useState(false);
  const [selectedVoucher, setSelectedVoucher] = useState<CartPlatformVoucherResponse | null>(null);

  // Payment state
  const [paymentMethod, setPaymentMethod] = useState<"vnpay" | "cod">("vnpay");

  // Submission state
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [confirmedOrder, setConfirmedOrder] = useState<PlaceOrderResponse | null>(null);
  const [isPlaced, setIsPlaced] = useState(false);

  // Auto select default address
  useEffect(() => {
    if (addresses.length > 0 && !selectedAddressId) {
      const defaultAddr = addresses.find((a) => a.isDefault) || addresses[0];
      if (defaultAddr) {
        setSelectedAddressId(defaultAddr.id);
      }
    }
  }, [addresses, selectedAddressId]);

  // Auto select all items on initial cart load
  useEffect(() => {
    if (!hasInitializedSelection && items.length > 0) {
      setSelectedIds(items.map((item) => item.id));
      setHasInitializedSelection(true);
    }
  }, [items, hasInitializedSelection]);

  const formatPrice = (val: number) => {
    return new Intl.NumberFormat("vi-VN").format(val) + "₫";
  };

  const isAllSelected = items.length > 0 && selectedIds.length === items.length;

  const toggleSelectAll = () => {
    if (isAllSelected) {
      setSelectedIds([]);
    } else {
      setSelectedIds(items.map((i) => i.id));
    }
  };

  const toggleSelectItem = (id: string) => {
    if (selectedIds.includes(id)) {
      setSelectedIds(selectedIds.filter((item) => item !== id));
    } else {
      setSelectedIds([...selectedIds, id]);
    }
  };

  const selectedItems = items.filter((item) => selectedIds.includes(item.id));
  const selectedSubtotal = selectedItems.reduce(
    (sum, item) => sum + item.price * item.quantity,
    0
  );

  const isShippingVoucher = selectedVoucher?.appliesTo === "SHIPPING_FEE";
  const validCartItemIds = selectedItems.map((i) => i.id).filter(isUuid);

  // Group items by store for selected items only
  const selectedGroupedStores = selectedItems.reduce<Record<string, typeof selectedItems>>((acc, item) => {
    const store = item.storeName || "TechHub Verified Store";
    if (!acc[store]) acc[store] = [];
    acc[store].push(item);
    return acc;
  }, {});

  // Group all items by store for cart items list
  const groupedStores = items.reduce<Record<string, typeof items>>((acc, item) => {
    const store = item.storeName || "TechHub Verified Store";
    if (!acc[store]) acc[store] = [];
    acc[store].push(item);
    return acc;
  }, {});

  // Dynamic preview calculation from Backend API (BR-17)
  const previewParams = useMemo(() => ({
    selectedCartItemIds: validCartItemIds,
    addressId: selectedAddressId && isUuid(selectedAddressId) ? selectedAddressId : undefined,
    platformVoucherId: selectedVoucher && !isShippingVoucher ? selectedVoucher.id : undefined,
    platformShippingVoucherId: selectedVoucher && isShippingVoucher ? selectedVoucher.id : undefined,
    shopVoucherIds: [],
  }), [validCartItemIds, selectedAddressId, selectedVoucher, isShippingVoucher]);

  const { data: previewData, isLoading: isLoadingPreview } = useCheckoutPreview(
    previewParams,
    validCartItemIds.length > 0
  );

  // Multi-shop shipping fallback (30.000₫ per shop)
  const selectedShopsCount = Object.keys(selectedGroupedStores).length || 1;
  const fallbackShippingFee = selectedItems.length > 0 ? selectedShopsCount * 30000 : 0;

  const totalShippingFee = previewData?.totalShippingFee !== undefined
    ? previewData.totalShippingFee
    : fallbackShippingFee;

  const finalShippingFee = previewData?.finalShippingFee !== undefined
    ? previewData.finalShippingFee
    : totalShippingFee;

  const shippingDiscount = Math.max(0, totalShippingFee - finalShippingFee);

  // Query platform vouchers from Backend
  const { data: platformVouchers = [], isLoading: isLoadingVouchers } = usePlatformVouchersQuery({
    orderAmount: selectedSubtotal,
    shippingFee: totalShippingFee,
  });

  // Tự động kiểm tra tính hợp lệ khi giỏ hàng thay đổi số lượng / tổng tiền
  useEffect(() => {
    if (selectedVoucher && platformVouchers.length > 0) {
      const updated = platformVouchers.find((v) => v.id === selectedVoucher.id);
      if (updated) {
        if (!updated.isEligible) {
          setSelectedVoucher(null);
        } else {
          setSelectedVoucher(updated);
        }
      }
    }
  }, [platformVouchers, selectedVoucher]);

  const productDiscount = (previewData?.totalPlatformDiscountAmount ?? 0) + (previewData?.totalShopDiscountAmount ?? 0);
  const discountAmount = selectedVoucher
    ? (selectedVoucher.appliesTo === "SHIPPING_FEE" ? shippingDiscount : selectedVoucher.estimatedDiscount)
    : productDiscount;

  const grandTotal = previewData?.finalTotalAmount !== undefined
    ? previewData.finalTotalAmount
    : Math.max(0, selectedSubtotal + finalShippingFee - discountAmount);

  // Build shop delivery packages to render
  const shopsToRender = previewData?.shops && previewData.shops.length > 0
    ? previewData.shops
    : Object.entries(selectedGroupedStores).map(([storeName, storeItems]) => ({
        shopId: storeItems[0]?.storeId || storeName,
        shopName: storeName,
        shippingFee: 30000,
        finalShippingFee: 30000,
        shopShippingDiscount: 0,
        allocatedPlatformShippingDiscount: 0,
      }));

  const handlePlaceOrder = async (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedItems.length === 0) return;
    setSubmitError(null);

    if (!selectedAddressId && addresses.length > 0) {
      setSubmitError("Vui lòng chọn địa chỉ nhận hàng.");
      return;
    }

    if (addresses.length === 0) {
      setSubmitError("Bạn chưa có địa chỉ nhận hàng. Vui lòng thêm địa chỉ nhận hàng trước.");
      return;
    }

    setIsSubmitting(true);

    try {
      const mappedPaymentMethod: PaymentMethod = paymentMethod === "cod" ? "COD" : "VNPAY";

      // Group shop orders strictly from selected items
      const shopOrders: ShopOrderRequest[] = Object.entries(selectedGroupedStores).map(([_, storeItems]) => ({
        shopId:
          storeItems[0]?.storeId && isUuid(storeItems[0].storeId)
            ? storeItems[0].storeId
            : "00000000-0000-0000-0000-000000000000",
        note: "",
      }));

      // Call Backend Checkout API
      if (validCartItemIds.length > 0 && selectedAddressId) {
        const res = await checkoutApi.placeOrder({
          addressId: selectedAddressId,
          paymentMethod: mappedPaymentMethod,
          selectedCartItemIds: validCartItemIds,
          shopOrders,
          platformVoucherId: selectedVoucher && !isShippingVoucher ? selectedVoucher.id : null,
          platformShippingVoucherId: selectedVoucher && isShippingVoucher ? selectedVoucher.id : null,
        });

        // VNPay Payment redirect
        if (mappedPaymentMethod === "VNPAY" && res.data.paymentUrl) {
          window.location.href = res.data.paymentUrl;
          return;
        }

        // COD success
        setConfirmedOrder(res.data);
        setIsPlaced(true);
        refetchCart();
        return;
      }

      // Fallback for mock items during development
      setIsPlaced(true);
      refetchCart();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      const msg =
        axiosErr?.response?.data?.message ||
        "Đặt hàng thất bại. Vui lòng kiểm tra lại thông tin hoặc thử lại sau.";
      setSubmitError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const selectedAddress = addresses.find((a) => a.id === selectedAddressId) || addresses[0];

  if (isPlaced) {
    const orderNumbers = confirmedOrder?.orders?.map((o) => o.orderNumber).join(", ") || "#TH-94812";
    const finalPaid = confirmedOrder?.finalTotalAmount ?? grandTotal;

    return (
      <div className="w-full bg-[#f8f9ff] min-h-[calc(100vh-160px)] py-12 flex items-center justify-center overflow-hidden">
        <div className="max-w-md w-full mx-4 bg-white rounded-2xl shadow-lg border border-[#c3c5d9]/30 p-8 text-center flex flex-col items-center gap-4">
          <div className="w-16 h-16 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center">
            <CheckCircle2 className="w-10 h-10" />
          </div>
          <h1 className="font-heading text-2xl font-bold text-[#0b1c30]">
            Đặt hàng thành công!
          </h1>
          <p className="text-sm text-[#434656] leading-relaxed">
            Cảm ơn bạn đã mua sắm tại TechHub. Mã đơn hàng của bạn là{" "}
            <span className="font-mono font-bold text-[#0b1c30]">{orderNumbers}</span>.
          </p>
          <div className="w-full bg-[#eff4ff] rounded-xl p-4 text-xs flex flex-col gap-2 my-2 text-left">
            <div className="flex justify-between">
              <span className="text-[#737688]">Tổng thanh toán:</span>
              <span className="font-bold text-[#0045cc] font-mono">
                {formatPrice(finalPaid)}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-[#737688]">Phương thức:</span>
              <span className="font-semibold text-emerald-600 uppercase">
                {paymentMethod === "vnpay" ? "VNPay QR" : "Thanh toán khi nhận hàng (COD)"}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-[#737688]">Dự kiến giao hàng:</span>
              <span className="font-semibold text-[#0b1c30]">
                2-3 ngày làm việc (Giao tiêu chuẩn)
              </span>
            </div>
          </div>
          <div className="flex flex-col gap-2.5 w-full mt-2">
            <Link
              href={ROUTES.ACCOUNT.ORDERS}
              className="w-full py-3 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-sm font-semibold transition-colors flex items-center justify-center gap-2"
            >
              <span>Xem đơn mua của tôi</span>
            </Link>
            <Link
              href={ROUTES.HOME}
              className="w-full py-3 rounded-xl bg-[#eff4ff] hover:bg-[#dce9ff] text-[#0045cc] text-sm font-semibold transition-colors flex items-center justify-center gap-2"
            >
              <span>Tiếp tục mua sắm</span>
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full bg-[#f8f9ff] min-h-[calc(100vh-160px)] py-8">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-12 flex flex-col gap-6">
        {/* Navigation Breadcrumb */}
        <div className="flex flex-col gap-2">
          <div className="flex items-center text-xs text-[#737688] gap-1.5">
            <Link href="/" className="hover:text-[#0045cc] transition-colors">
              Trang chủ
            </Link>
            <span>/</span>
            <span className="text-[#0b1c30] font-medium">Giỏ hàng &amp; Thanh toán</span>
          </div>
          <div className="flex flex-wrap items-center justify-between gap-4">
            <h1 className="font-heading text-2xl sm:text-3xl font-bold text-[#0b1c30] tracking-tight">
              Giỏ hàng &amp; Thanh toán
            </h1>
            <Link
              href="/search"
              className="text-xs sm:text-sm font-bold text-[#0045cc] hover:underline inline-flex items-center gap-1"
            >
              Tiếp tục chọn hàng <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>

        {items.length === 0 ? (
          <div className="bg-white rounded-2xl border border-[#c3c5d9]/30 shadow-sm p-12 flex flex-col items-center justify-center text-center">
            <div className="w-16 h-16 rounded-full bg-[#eff4ff] text-[#0045cc] flex items-center justify-center mb-4">
              <Store className="w-8 h-8" />
            </div>
            <h2 className="font-heading text-xl font-bold text-[#0b1c30] mb-2">
              Giỏ hàng của bạn đang trống
            </h2>
            <p className="text-sm text-[#434656] max-w-sm mb-6">
              Bạn chưa thêm sản phẩm công nghệ nào vào giỏ. Khám phá các thiết bị phần cứng chính hãng ngay hôm nay!
            </p>
            <Link
              href="/search"
              className="px-6 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-sm font-semibold transition-colors shadow-sm"
            >
              Khám phá sản phẩm
            </Link>
          </div>
        ) : (
          <form onSubmit={handlePlaceOrder} className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
            {/* Left Column (8 cols): Cart Items + Shipping Address + Shipping Service + Payment */}
            <div className="lg:col-span-8 flex flex-col gap-5">
              {/* Select All Header */}
              <div className="bg-white rounded-xl p-4 px-6 border border-[#c3c5d9]/30 shadow-sm flex items-center justify-between">
                <label className="flex items-center gap-3 cursor-pointer select-none text-xs sm:text-sm font-semibold text-[#0b1c30]">
                  <input
                    type="checkbox"
                    checked={isAllSelected}
                    onChange={toggleSelectAll}
                    className="w-4 h-4 rounded text-[#0d5bff] accent-[#0d5bff] cursor-pointer"
                  />
                  <span>Chọn tất cả ({totalCount} sản phẩm)</span>
                </label>
                <span className="text-xs text-[#737688]">
                  Đã chọn:{" "}
                  <strong className="text-[#0045cc] font-bold">
                    {selectedItems.length}
                  </strong>
                </span>
              </div>

              {/* Cart Items by Store */}
              {Object.entries(groupedStores).map(([storeName, storeItems]) => {
                const storeAllSelected = storeItems.every((i) =>
                  selectedIds.includes(i.id)
                );

                const toggleStore = () => {
                  if (storeAllSelected) {
                    setSelectedIds(
                      selectedIds.filter(
                        (id) => !storeItems.some((si) => si.id === id)
                      )
                    );
                  } else {
                    const newIds = [
                      ...selectedIds,
                      ...storeItems
                        .map((si) => si.id)
                        .filter((id) => !selectedIds.includes(id)),
                    ];
                    setSelectedIds(newIds);
                  }
                };

                return (
                  <div
                    key={storeName}
                    className="bg-white rounded-xl border border-[#c3c5d9]/30 shadow-sm overflow-hidden flex flex-col"
                  >
                    {/* Store Header */}
                    <div className="px-6 py-3.5 border-b border-[#c3c5d9]/20 flex items-center justify-between bg-white">
                      <div className="flex items-center gap-3">
                        <input
                          type="checkbox"
                          checked={storeAllSelected}
                          onChange={toggleStore}
                          className="w-4 h-4 rounded text-[#0d5bff] accent-[#0d5bff] cursor-pointer"
                        />
                        <div className="flex items-center gap-2">
                          <Store className="w-4 h-4 text-[#0045cc]" />
                          <Link
                            href={`/shops/${resolveSellerSlug(storeName)}`}
                            className="text-sm font-bold text-[#0b1c30] hover:text-[#0045cc] transition-colors hover:underline"
                          >
                            {storeName}
                          </Link>
                        </div>
                      </div>
                    </div>

                    {/* Store Items List */}
                    <div className="divide-y divide-[#c3c5d9]/20">
                      {storeItems.map((item) => (
                        <div
                          key={item.id}
                          className="p-6 flex flex-col sm:flex-row items-start sm:items-center gap-4"
                        >
                          <div className="flex items-center gap-3">
                            <input
                              type="checkbox"
                              checked={selectedIds.includes(item.id)}
                              onChange={() => toggleSelectItem(item.id)}
                              className="w-4 h-4 rounded text-[#0d5bff] accent-[#0d5bff] cursor-pointer flex-shrink-0"
                            />
                            <div className="w-20 h-20 rounded-xl overflow-hidden bg-[#eff4ff] flex-shrink-0 border border-[#c3c5d9]/30 relative">
                              <Image
                                src={item.imageUrl}
                                alt={item.title}
                                fill
                                className="object-cover"
                                unoptimized
                              />
                            </div>
                          </div>

                          <div className="flex-1 min-w-0 flex flex-col gap-1">
                            <h3 className="text-sm font-bold text-[#0b1c30] line-clamp-2 leading-snug hover:text-[#0045cc] transition-colors cursor-pointer">
                              {item.title}
                            </h3>
                            <div className="text-xs text-[#737688]">
                              Phân loại:{" "}
                              <span className="text-[#434656] font-medium">
                                {item.specs}
                              </span>
                            </div>
                            <div className="mt-1.5 text-[#0045cc] font-bold text-base">
                              {formatPrice(item.price)}
                            </div>
                          </div>

                          <div className="flex items-center justify-between sm:justify-end gap-4 w-full sm:w-auto mt-2 sm:mt-0 flex-shrink-0">
                            {/* Quantity Controls */}
                            <div className="flex items-center border border-[#c3c5d9]/40 rounded-lg overflow-hidden bg-white">
                              <button
                                type="button"
                                onClick={() => updateQuantity(item.id, -1)}
                                className="w-8 h-8 flex items-center justify-center text-[#737688] hover:text-[#0b1c30] hover:bg-[#eff4ff] transition-colors cursor-pointer"
                              >
                                <Minus className="w-3.5 h-3.5" />
                              </button>
                              <span className="w-8 text-center text-xs font-bold text-[#0b1c30]">
                                {item.quantity}
                              </span>
                              <button
                                type="button"
                                onClick={() => updateQuantity(item.id, 1)}
                                className="w-8 h-8 flex items-center justify-center text-[#737688] hover:text-[#0b1c30] hover:bg-[#eff4ff] transition-colors cursor-pointer"
                              >
                                <Plus className="w-3.5 h-3.5" />
                              </button>
                            </div>

                            {/* Delete Item */}
                            <button
                              type="button"
                              onClick={() => removeItem(item.id)}
                              className="text-[#737688] hover:text-[#ba1a1a] transition-colors p-1.5 rounded-lg hover:bg-[#eff4ff] cursor-pointer"
                              title="Xóa sản phẩm"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                );
              })}

              {/* Delivery Address Section (Connected to Real Backend API) */}
              <div className="bg-white rounded-xl border border-[#c3c5d9]/30 shadow-sm p-6 flex flex-col gap-4">
                <div className="flex items-center justify-between border-b border-[#c3c5d9]/20 pb-3">
                  <div className="flex items-center gap-2 text-sm font-bold text-[#0b1c30]">
                    <MapPin className="w-4 h-4 text-[#0045cc]" />
                    <span>Địa chỉ nhận hàng</span>
                  </div>
                  <Link
                    href={ROUTES.ACCOUNT.ADDRESSES}
                    className="text-xs font-semibold text-[#0045cc] hover:underline cursor-pointer flex items-center gap-1"
                  >
                    <span>Quản lý địa chỉ</span>
                  </Link>
                </div>

                {isLoadingAddresses ? (
                  <div className="py-4 flex items-center gap-2 text-xs text-[#737688]">
                    <Loader2 className="w-4 h-4 animate-spin text-[#0045cc]" />
                    <span>Đang tải địa chỉ nhận hàng...</span>
                  </div>
                ) : addresses.length === 0 ? (
                  <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-xs flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div className="flex items-center gap-2">
                      <AlertCircle className="w-4 h-4 text-amber-600 shrink-0" />
                      <span>Bạn chưa có địa chỉ nhận hàng nào trong hệ thống.</span>
                    </div>
                    <Link
                      href={ROUTES.ACCOUNT.ADDRESSES}
                      className="px-3 py-1.5 bg-[#0045cc] hover:bg-[#0d5bff] text-white rounded-lg font-semibold inline-flex items-center gap-1 shrink-0 text-center"
                    >
                      <PlusCircle className="w-3.5 h-3.5" />
                      <span>Thêm địa chỉ ngay</span>
                    </Link>
                  </div>
                ) : (
                  <div className="flex flex-col gap-2">
                    {addresses.map((addr) => (
                      <label
                        key={addr.id}
                        onClick={() => setSelectedAddressId(addr.id)}
                        className={`p-3.5 rounded-xl border cursor-pointer transition-all flex items-start gap-3 ${
                          selectedAddressId === addr.id
                            ? "border-[#0045cc] bg-[#eff4ff]/60 ring-2 ring-[#0045cc]/15"
                            : "border-slate-200 hover:bg-slate-50"
                        }`}
                      >
                        <input
                          type="radio"
                          name="delivery_address"
                          checked={selectedAddressId === addr.id}
                          onChange={() => setSelectedAddressId(addr.id)}
                          className="mt-1 accent-[#0045cc]"
                        />
                        <div className="flex flex-col gap-0.5 text-sm flex-1">
                          <div className="flex items-center gap-2 font-bold text-[#0b1c30]">
                            <span>{addr.recipientName}</span>
                            <span className="text-xs font-normal text-[#737688]">
                              ({addr.phone})
                            </span>
                            {addr.isDefault && (
                              <span className="bg-[#eff4ff] text-[#0045cc] text-[10px] font-bold px-2 py-0.5 rounded uppercase">
                                Mặc định
                              </span>
                            )}
                          </div>
                          <p className="text-xs text-[#434656] leading-relaxed">
                            {addr.fullAddress || `${addr.streetAddress}, ${addr.ward}, ${addr.district}, ${addr.province}`}
                          </p>
                        </div>
                      </label>
                    ))}
                  </div>
                )}
              </div>

              {/* Shipping Method Section */}
              <div className="bg-white rounded-xl border border-[#c3c5d9]/30 shadow-sm p-6 flex flex-col gap-4">
                <div className="flex items-center justify-between border-b border-[#c3c5d9]/20 pb-3">
                  <div className="flex items-center gap-2 text-sm font-bold text-[#0b1c30]">
                    <Truck className="w-4 h-4 text-[#0045cc]" />
                    <span>Phương thức vận chuyển ({shopsToRender.length} kiện hàng)</span>
                  </div>
                  {isLoadingPreview && (
                    <div className="flex items-center gap-1.5 text-xs text-[#0045cc]">
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      <span>Đang cập nhật cước...</span>
                    </div>
                  )}
                </div>

                {/* Backend warnings banner */}
                {previewData?.warnings && previewData.warnings.length > 0 && (
                  <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-xs text-amber-800 flex items-start gap-2">
                    <AlertCircle className="w-4 h-4 shrink-0 text-amber-600 mt-0.5" />
                    <div className="flex flex-col gap-0.5">
                      {previewData.warnings.map((warn, idx) => (
                        <p key={idx}>{warn}</p>
                      ))}
                    </div>
                  </div>
                )}

                {/* Per-Shop Shipping Packages */}
                <div className="flex flex-col gap-3">
                  {shopsToRender.map((shop, idx) => {
                    const shopOrigFee = shop.shippingFee ?? 30000;
                    const shopDiscount =
                      (shop.shopShippingDiscount ?? 0) +
                      (shop.allocatedPlatformShippingDiscount ?? 0);
                    const shopFinalFee =
                      shop.finalShippingFee ?? Math.max(0, shopOrigFee - shopDiscount);

                    return (
                      <div
                        key={shop.shopId || idx}
                        className="p-4 rounded-xl border border-[#0045cc]/20 bg-[#eff4ff]/30 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                      >
                        <div className="flex items-start gap-3">
                          <div className="w-9 h-9 rounded-xl bg-white border border-[#c3c5d9]/40 flex items-center justify-center text-[#0045cc] shrink-0 mt-0.5 shadow-xs">
                            <Store className="w-4 h-4" />
                          </div>
                          <div className="flex flex-col gap-0.5">
                            <div className="flex items-center gap-2">
                              <span className="text-xs font-bold text-[#0b1c30]">
                                Kiện {idx + 1}: {shop.shopName || "Gian hàng TechHub"}
                              </span>
                              <span className="text-[10px] font-semibold bg-emerald-100 text-emerald-700 px-1.5 py-0.5 rounded">
                                Giao tiêu chuẩn
                              </span>
                            </div>
                            <span className="text-[11px] text-[#737688]">
                              Giao từ 2 - 3 ngày làm việc • Đóng gói niêm phong chống sốc phần cứng
                            </span>
                          </div>
                        </div>

                        <div className="flex sm:flex-col items-end justify-between sm:justify-center border-t sm:border-t-0 pt-2 sm:pt-0 border-slate-200/60">
                          {shopDiscount > 0 ? (
                            <div className="flex items-center gap-1.5">
                              <span className="text-[11px] line-through text-[#737688]">
                                {formatPrice(shopOrigFee)}
                              </span>
                              <span className="text-xs font-mono font-bold text-emerald-600">
                                {shopFinalFee === 0 ? "Miễn phí" : formatPrice(shopFinalFee)}
                              </span>
                            </div>
                          ) : (
                            <span className="text-xs font-mono font-bold text-[#0045cc]">
                              {formatPrice(shopOrigFee)}
                            </span>
                          )}
                          <span className="text-[10px] text-[#737688]">
                            {shopDiscount > 0 ? "Đã giảm phí ship sàn" : "Phí vận chuyển kiện"}
                          </span>
                        </div>
                      </div>
                    );
                  })}
                </div>

                {/* Shipping Voucher Savings Highlight */}
                {shippingDiscount > 0 && (
                  <div className="flex items-center justify-between p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-800">
                    <span className="font-medium flex items-center gap-1.5">
                      <Sparkles className="w-3.5 h-3.5 text-emerald-600" />
                      Ưu đãi Voucher Freeship Sàn đã áp dụng
                    </span>
                    <span className="font-bold font-mono text-emerald-700">
                      -{formatPrice(shippingDiscount)}
                    </span>
                  </div>
                )}
              </div>

              {/* Payment Methods Section */}
              <div className="bg-white rounded-xl border border-[#c3c5d9]/30 shadow-sm p-6 flex flex-col gap-4">
                <div className="flex items-center gap-2 text-sm font-bold text-[#0b1c30] border-b border-[#c3c5d9]/20 pb-3">
                  <CreditCard className="w-4 h-4 text-[#0045cc]" />
                  <span>Phương thức thanh toán</span>
                </div>

                <div className="flex flex-col gap-2.5">
                  {/* VNPay */}
                  <label
                    onClick={() => setPaymentMethod("vnpay")}
                    className={`border rounded-xl p-4 flex items-center justify-between cursor-pointer transition-all ${
                      paymentMethod === "vnpay"
                        ? "border-[#0045cc] bg-[#eff4ff]/60 ring-2 ring-[#0045cc]/15"
                        : "border-[#c3c5d9]/30 bg-white hover:bg-neutral-50"
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <input
                        type="radio"
                        name="payment"
                        checked={paymentMethod === "vnpay"}
                        onChange={() => setPaymentMethod("vnpay")}
                        className="accent-[#0045cc]"
                      />
                      <div className="flex flex-col">
                        <span className="text-xs font-bold text-[#0b1c30]">
                          Cổng VNPay (Thẻ ATM / QR Pay / Visa / Mastercard)
                        </span>
                        <span className="text-[11px] text-[#737688]">
                          Thanh toán bảo mật trực tuyến qua VNPay Sandbox
                        </span>
                      </div>
                    </div>
                    <span className="text-[10px] font-bold text-[#0045cc] bg-[#eff4ff] px-2 py-0.5 rounded">
                      Khuyên dùng
                    </span>
                  </label>

                  {/* COD */}
                  <label
                    onClick={() => setPaymentMethod("cod")}
                    className={`border rounded-xl p-4 flex items-center justify-between cursor-pointer transition-all ${
                      paymentMethod === "cod"
                        ? "border-[#0045cc] bg-[#eff4ff]/60 ring-2 ring-[#0045cc]/15"
                        : "border-[#c3c5d9]/30 bg-white hover:bg-neutral-50"
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <input
                        type="radio"
                        name="payment"
                        checked={paymentMethod === "cod"}
                        onChange={() => setPaymentMethod("cod")}
                        className="accent-[#0045cc]"
                      />
                      <div className="flex flex-col">
                        <span className="text-xs font-bold text-[#0b1c30]">
                          Thanh toán khi nhận hàng (COD)
                        </span>
                        <span className="text-[11px] text-[#737688]">
                          Được kiểm tra tem niêm phong trước khi thanh toán
                        </span>
                      </div>
                    </div>
                  </label>
                </div>
              </div>
            </div>

            {/* Right Column (4 cols Sticky): Vouchers & Final Checkout Order Summary */}
            <div className="lg:col-span-4 flex flex-col gap-4 lg:sticky lg:top-28">
              {/* Vouchers & Promos */}
              <div className="bg-white rounded-xl border border-[#c3c5d9]/30 shadow-sm p-5 flex flex-col gap-3.5">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 text-sm font-bold text-[#0b1c30]">
                    <Tag className="w-4 h-4 text-[#0045cc]" />
                    <span>TechHub Voucher</span>
                  </div>
                  {selectedVoucher && (
                    <button
                      type="button"
                      onClick={() => setIsVoucherModalOpen(true)}
                      className="text-xs font-semibold text-[#0045cc] hover:underline cursor-pointer"
                    >
                      Đổi mã
                    </button>
                  )}
                </div>

                {selectedVoucher ? (
                  <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-800 flex items-center justify-between gap-2">
                    <div className="flex flex-col gap-0.5 min-w-0">
                      <div className="flex items-center gap-1.5 font-bold">
                        <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                        <span className="font-mono">{selectedVoucher.code}</span>
                      </div>
                      <span className="text-[11px] text-emerald-700 truncate">
                        {selectedVoucher.name} (-{formatPrice(discountAmount)})
                      </span>
                    </div>
                    <button
                      type="button"
                      onClick={() => setSelectedVoucher(null)}
                      className="text-xs font-semibold text-rose-600 hover:text-rose-800 hover:underline cursor-pointer shrink-0"
                    >
                      Bỏ chọn
                    </button>
                  </div>
                ) : (
                  <button
                    type="button"
                    onClick={() => setIsVoucherModalOpen(true)}
                    className="w-full p-3 rounded-xl border border-dashed border-[#0045cc]/40 bg-[#eff4ff]/40 hover:bg-[#eff4ff] transition-all flex items-center justify-between text-left cursor-pointer group"
                  >
                    <div className="flex items-center gap-2">
                      <Sparkles className="w-4 h-4 text-[#0045cc]" />
                      <span className="text-xs font-semibold text-[#0045cc]">
                        {platformVouchers.filter((v) => v.isEligible).length > 0
                          ? `Chọn voucher (${
                              platformVouchers.filter((v) => v.isEligible).length
                            } mã khả dụng)`
                          : "Chọn hoặc nhập mã giảm giá"}
                      </span>
                    </div>
                    <ChevronRight className="w-4 h-4 text-[#0045cc] group-hover:translate-x-0.5 transition-transform shrink-0" />
                  </button>
                )}
              </div>

              {/* Order Summary Card */}
              <div className="bg-white rounded-xl border border-[#c3c5d9]/30 shadow-sm p-6 flex flex-col gap-4">
                <h3 className="font-bold text-base text-[#0b1c30] border-b border-[#c3c5d9]/20 pb-3">
                  Tóm tắt đơn hàng
                </h3>

                {submitError && (
                  <div className="p-3 rounded-xl bg-red-50 border border-red-200 text-red-600 text-xs flex items-center gap-2">
                    <AlertCircle className="w-4 h-4 shrink-0" />
                    <span>{submitError}</span>
                  </div>
                )}

                <div className="flex flex-col gap-2.5 text-xs">
                  <div className="flex items-center justify-between text-[#434656]">
                    <span>Tạm tính ({selectedItems.length} sản phẩm):</span>
                    <span className="font-mono font-bold text-[#0b1c30]">
                      {formatPrice(selectedSubtotal)}
                    </span>
                  </div>

                  <div className="flex items-center justify-between text-[#434656]">
                    <span>Phí vận chuyển ({shopsToRender.length} kiện):</span>
                    <span className="font-mono font-bold text-[#0b1c30]">
                      {selectedItems.length > 0 ? formatPrice(totalShippingFee) : "0₫"}
                    </span>
                  </div>
                  {shippingDiscount > 0 && (
                    <div className="flex items-center justify-between text-emerald-600">
                      <span>Giảm giá Freeship:</span>
                      <span className="font-mono font-bold">
                        -{formatPrice(shippingDiscount)}
                      </span>
                    </div>
                  )}
                  {discountAmount > 0 && (
                    <div className="flex items-center justify-between text-[#006b5b]">
                      <span>Giảm giá Voucher:</span>
                      <span className="font-mono font-bold">
                        -{formatPrice(discountAmount)}
                      </span>
                    </div>
                  )}
                </div>

                <div className="h-px bg-[#e5eeff] w-full"></div>

                <div className="flex flex-col gap-1">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-medium text-[#434656]">
                      Tổng tiền thanh toán:
                    </span>
                    <span className="text-[11px] text-[#006b5b] font-semibold">
                      (Đã gồm VAT)
                    </span>
                  </div>
                  <div className="flex items-center gap-2 font-heading text-2xl sm:text-3xl font-bold text-[#0045cc] tracking-tight">
                    <span>{formatPrice(grandTotal)}</span>
                    {isLoadingPreview && (
                      <Loader2 className="w-4 h-4 animate-spin text-[#0045cc]" />
                    )}
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={isSubmitting || selectedItems.length === 0}
                  className="w-full py-3.5 px-4 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-sm font-bold transition-all shadow-md flex items-center justify-center gap-2 mt-2 active:scale-[0.99] disabled:opacity-60 cursor-pointer"
                >
                  {isSubmitting ? (
                    <>
                      <Loader2 className="w-5 h-5 animate-spin" />
                      <span>Đang xử lý đơn hàng...</span>
                    </>
                  ) : (
                    <>
                      <span>Đặt hàng ({formatPrice(grandTotal)})</span>
                      <ArrowRight className="w-4 h-4" />
                    </>
                  )}
                </button>

                <div className="flex items-center justify-center gap-1.5 text-[11px] text-[#737688] pt-1">
                  <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                  <span>Bảo vệ quyền lợi người mua TechHub 30 ngày</span>
                </div>
              </div>
            </div>
          </form>
        )}
      </div>

      {/* Voucher Selection Modal */}
      <VoucherSelectModal
        isOpen={isVoucherModalOpen}
        onClose={() => setIsVoucherModalOpen(false)}
        vouchers={platformVouchers}
        isLoading={isLoadingVouchers}
        selectedVoucher={selectedVoucher}
        onApplyVoucher={(v) => setSelectedVoucher(v)}
        orderAmount={selectedSubtotal}
      />
    </div>
  );
}
