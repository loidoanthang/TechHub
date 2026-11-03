import { AddressesView } from "@/features/account/components/AddressesView";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Sổ địa chỉ nhận hàng | TechHub Marketplace",
  description: "Quản lý danh sách địa chỉ nhận hàng của bạn trên TechHub Marketplace.",
};

export default function AddressesPage() {
  return <AddressesView />;
}
