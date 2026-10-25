export interface CartItem {
  id: string;
  storeId?: string;
  storeName?: string;
  title: string;
  specs: string;
  price: number;
  quantity: number;
  imageUrl: string;
}

export interface Voucher {
  id: string;
  code: string;
  title: string;
  description: string;
  discountAmount: number;
  minOrderValue: number;
}

export const MOCK_AVAILABLE_VOUCHERS: Voucher[] = [
  {
    id: "vouch-1",
    code: "TECHMEGA500",
    title: "Mega Tech Festival",
    description: "500K discount for orders over 10M",
    discountAmount: 500000,
    minOrderValue: 10000000,
  },
  {
    id: "vouch-2",
    code: "FREESHIPALL",
    title: "Sitewide Free Shipping",
    description: "Free sitewide shipping on TechHub (Max 70K)",
    discountAmount: 70000,
    minOrderValue: 500000,
  },
];

export const MOCK_INITIAL_CART_ITEMS: CartItem[] = [
  {
    id: "cart-item-1",
    storeId: "store-geartech",
    storeName: "GearTech Flagship Store",
    title: "Card màn hình ASUS ROG Strix GeForce RTX 4080 SUPER 16GB GDDR6X OC Edition",
    specs: "ROG Strix RTX 4080 SUPER OC - Obsidian Black",
    price: 33990000,
    quantity: 1,
    imageUrl:
      "https://lh3.googleusercontent.com/aida-public/AB6AXuBmEc9AcPT1J1-Ca_lmY2HH0wRGqzXVLSV36iUoP-0Go6G-x4WE10TxFvyI4yg2SIHRmOM_0PY7-epJkeX0uEig-_g_Nnte9H8yVBvgFkrjWbN3qL2wlj-fOJ_zZd1Zzgy9rlF5gs6yUiyEhqmdoVJw-lDDWFzc88RyG38OaC1f2aSOR94I7c97hZCJg8cv6Xuoqm3xFp9nJ-BVDkxS5sBBir_erfeVjtGHqRaPACGMQWffPdphNIvRKQ",
  },
  {
    id: "cart-item-2",
    storeId: "store-silicon",
    storeName: "Silicon Zone Vietnam",
    title: "Bộ vi xử lý AMD Ryzen 9 9950X (16 Nhân / 32 Luồng, Up to 5.7GHz, 80MB Cache, AM5)",
    specs: "Ryzen 9 9950X - Official Authentic Box (36-Mo Warranty)",
    price: 16890000,
    quantity: 1,
    imageUrl:
      "https://lh3.googleusercontent.com/aida-public/AB6AXuCbJ8lxKTHvK0RhfnySS2VYm84WNcmJMjGLX4BuqsCNSswYE9tdw0re5lHQIlhv8mUZb88Nz9FX3K5PCqzfQlRXGuPJbBTZQQw4vtreYZvFg5xoZLmKLDNnPst_0W6YiYtYUhM2tksN3qS8BXx-R8D7cGcLtn2HslKx26X4e-mdHdePwIHoHksuGtNtZJ2-fKHwxyfmapZa5tUDyZSfQ1-Su7dFicmOlAa1f05bkuNoZdD1GmsbknWxzg",
  },
];
