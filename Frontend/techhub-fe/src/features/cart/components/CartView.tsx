"use client";

import { OnePageCartCheckoutView } from "./OnePageCartCheckoutView";

/**
 * CartView acts as a canonical view component for the unified Shopping Cart & Checkout flow.
 * In TechHub's modern architecture, Cart and Checkout are consolidated into a single seamless
 * responsive view (OnePageCartCheckoutView) connected to real backend APIs.
 */
export function CartView() {
  return <OnePageCartCheckoutView />;
}
