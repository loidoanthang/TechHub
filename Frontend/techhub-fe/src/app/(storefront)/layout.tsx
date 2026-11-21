import { Header, Footer } from "@/components/shared";
import { CartProvider } from "@/features/cart/context/CartContext";
import { CartDrawer } from "@/features/cart/components";
import { ChatProvider } from "@/features/chat";

export default function StorefrontLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <CartProvider>
      <ChatProvider>
        <div className="min-h-screen flex flex-col bg-[#f8f9ff] overflow-x-hidden w-full max-w-full">
          <Header />
          <main className="flex-1 flex flex-col pt-32 sm:pt-28 pb-12 sm:pb-0">{children}</main>
          <Footer />
          <CartDrawer />
        </div>
      </ChatProvider>
    </CartProvider>
  );
}
