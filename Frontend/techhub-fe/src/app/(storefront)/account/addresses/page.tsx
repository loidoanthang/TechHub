import { AddressesView } from "@/features/account/components/AddressesView";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Shipping Addresses | TechHub Marketplace",
  description: "Manage your delivery addresses and set your default destination for tech gear.",
};

export default function AddressesPage() {
  return <AddressesView />;
}
