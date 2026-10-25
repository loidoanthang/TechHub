export type OrderStatus =
  | "ALL"
  | "PENDING_PAYMENT"
  | "PROCESSING"
  | "CONFIRMED"
  | "SHIPPED"
  | "DELIVERED"
  | "COMPLETED"
  | "CANCELLED"
  | "RETURNED";

export interface OrderItemDTO {
  id: string;
  productName: string;
  variantName: string;
  sku: string;
  price: number;
  originalPrice?: number;
  quantity: number;
  imageUrl: string;
  hasReviewed?: boolean;
}

export interface TrackingStep {
  status: string;
  title: string;
  description?: string;
  timestamp: string;
  completed: boolean;
  active?: boolean;
}

export interface CancellationRequestDTO {
  id: string;
  orderId: string;
  reason: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  createdAt: string;
}

export interface ReturnRefundRequestDTO {
  id: string;
  orderId: string;
  reason: string;
  description?: string;
  status: "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED" | "IN_PROGRESS" | "COMPLETED";
  mediaUrls: string[];
  createdAt: string;
}

export interface ReviewDTO {
  id: string;
  orderItemId: string;
  rating: number; // 1-5
  comment?: string;
  mediaUrls?: string[];
  sellerResponse?: string;
  sellerResponseAt?: string;
  createdAt: string;
}

export interface OrderDTO {
  id: string; // UUID or Order Number
  orderNumber: string;
  shopName: string;
  createdAt: string;
  status: OrderStatus;
  statusLabel: string;
  items: OrderItemDTO[];
  totalProductAmount: number;
  shippingFee: number;
  shopDiscountAmount: number;
  platformDiscountAmount: number;
  totalAmount: number;
  // Delivery & Tracking Details:
  recipientName: string;
  recipientPhone: string;
  deliveryAddress: string;
  shippingCarrier: string;
  trackingCode: string;
  carrierHotline: string;
  paymentMethod: string;
  paymentDetails: string;
  trackingTimeline: TrackingStep[];
  canReturn?: boolean;
  cancellationRequest?: CancellationRequestDTO;
  returnRefundRequest?: ReturnRefundRequestDTO;
}

export const MOCK_ORDERS: OrderDTO[] = [
  {
    id: "TH-88294012",
    orderNumber: "TH-88294012",
    shopName: "GearTech Flagship Store",
    createdAt: "Mar 28, 2026 • 14:22",
    status: "SHIPPED",
    statusLabel: "In Transit",
    items: [
      {
        id: "item-1",
        productName:
          "Card màn hình ASUS ROG Strix GeForce RTX 4080 SUPER 16GB GDDR6X OC Edition",
        variantName: "ROG Strix RTX 4080 SUPER OC - Obsidian Black",
        sku: "ROG-RTX4080S-O16G-G",
        price: 33990000,
        originalPrice: 35500000,
        quantity: 1,
        imageUrl:
          "https://lh3.googleusercontent.com/aida-public/AB6AXuBX2vOyANB8LpogV-7Js-24zU_C2KExXS8NW81wAapr4dZUgvYSI9oWfQPcn-3R7V_aqHg2iRsxauhZ1OcyW7ttqulnIlM-Wa4LOROavaCmf9NWMw36ON4CPXAbLQdq4qeWaN1ID2xSgF6dguBR4YRRdueSMNYiMFVcQeMFSmDd6jujrMWXJUWEBI1e_tqm24nIjyVUVrAc881mvc_dHSX3hWyXZJd69m6qGCqpFZeDEVHQLQgWSpytRA",
      },
    ],
    totalProductAmount: 35500000,
    shippingFee: 70000,
    shopDiscountAmount: 1510000,
    platformDiscountAmount: 570000,
    totalAmount: 33490000,
    recipientName: "Alex Morgan",
    recipientPhone: "(+84) 912 345 678",
    deliveryAddress:
      "TechHub Tower, 123 Dien Bien Phu Street, Floor 12, Da Kao Ward, District 1, Ho Chi Minh City",
    shippingCarrier: "VNPost Express / TechHub Priority",
    trackingCode: "THEXP-VN-994820194",
    carrierHotline: "1900-5454-88",
    paymentMethod: "Visa Platinum",
    paymentDetails: "Visa ending in •••• 4289 (Escrow Protection Active)",
    trackingTimeline: [
      {
        status: "PENDING_PAYMENT",
        title: "Order Placed",
        description: "Order initialized and payment authorized",
        timestamp: "Mar 28, 14:20",
        completed: true,
      },
      {
        status: "CONFIRMED",
        title: "Confirmed",
        description: "Merchant accepted order",
        timestamp: "Mar 28, 14:35",
        completed: true,
      },
      {
        status: "PROCESSING",
        title: "Packed & Labelled",
        description: "Package ready for carrier pickup",
        timestamp: "Mar 28, 16:10",
        completed: true,
      },
      {
        status: "SHIPPED",
        title: "In Transit",
        description: "Departed sorting facility",
        timestamp: "Mar 29, 08:30",
        completed: true,
        active: true,
      },
      {
        status: "DELIVERED",
        title: "Out for Delivery",
        description: "Courier assigned",
        timestamp: "Pending",
        completed: false,
      },
      {
        status: "COMPLETED",
        title: "Completed",
        description: "Awaiting delivery confirmation",
        timestamp: "Pending",
        completed: false,
      },
    ],
  },
  {
    id: "TH-88294025",
    orderNumber: "TH-88294025",
    shopName: "AMD Official Retail",
    createdAt: "Mar 29, 2026 • 09:15",
    status: "PROCESSING",
    statusLabel: "Preparing Hardware",
    items: [
      {
        id: "item-2",
        productName:
          "Bộ vi xử lý AMD Ryzen 9 9950X (16 Nhân / 32 Luồng, Up to 5.7GHz, 80MB Cache, AM5)",
        variantName: "Official Authentic Box (36-Mo Warranty)",
        sku: "AMD-RYZ-9950X-AM5",
        price: 16890000,
        originalPrice: 17200000,
        quantity: 1,
        imageUrl:
          "https://lh3.googleusercontent.com/aida-public/AB6AXuDz_YreJ9FAhab_3sLdrm3ETQqvKFcZqmKG5L2NYqP12BdCjaz44KnDL5W2CurNp57XL0r7DvuezQUB2n_M-bb4dGBwtD-MgYeOGiZrsbPqvOV8MIoFViCnNf7HEEphYnmvNv7f3D_SUpDd58zVW-NGZwK-3KBDQJ2eebS4mogmp5a198y8izL7Y0Rhe9l8SmzfWdMypp8HVlqbJCbHzQDLDbCoGq19gYcNP3kLsxiRVhz70dqG90x01A",
      },
    ],
    totalProductAmount: 17200000,
    shippingFee: 50000,
    shopDiscountAmount: 310000,
    platformDiscountAmount: 50000,
    totalAmount: 16890000,
    recipientName: "Alex Morgan",
    recipientPhone: "(+84) 912 345 678",
    deliveryAddress:
      "TechHub Tower, 123 Dien Bien Phu Street, Floor 12, Da Kao Ward, District 1, Ho Chi Minh City",
    shippingCarrier: "Standard Ground Delivery",
    trackingCode: "THEXP-VN-994820251",
    carrierHotline: "1900-5454-88",
    paymentMethod: "VNPay QR",
    paymentDetails: "VNPay instant bank authorization",
    trackingTimeline: [
      {
        status: "PENDING_PAYMENT",
        title: "Order Placed",
        description: "Payment authorized via VNPay",
        timestamp: "Mar 29, 09:15",
        completed: true,
      },
      {
        status: "CONFIRMED",
        title: "Order Confirmed",
        description: "Merchant accepted order",
        timestamp: "Mar 29, 09:30",
        completed: true,
      },
      {
        status: "PROCESSING",
        title: "Preparing Hardware",
        description: "Packaging Ryzen 9 9950X sealed box",
        timestamp: "Today, 10:00",
        completed: true,
        active: true,
      },
      {
        status: "SHIPPED",
        title: "In Transit",
        description: "Handing over to carrier",
        timestamp: "Est. Tomorrow",
        completed: false,
      },
    ],
  },
  {
    id: "TH-88294030",
    orderNumber: "TH-88294030",
    shopName: "Logitech G Official Store",
    createdAt: "Today • 11:30",
    status: "PENDING_PAYMENT",
    statusLabel: "To Pay",
    items: [
      {
        id: "item-pending",
        productName: "Chuột không dây Logitech G Pro X Superlight 2 Lightspeed - White",
        variantName: "White Edition • Hero 2 Sensor",
        sku: "LOGI-GPX-SL2-W",
        price: 3490000,
        originalPrice: 3890000,
        quantity: 1,
        imageUrl:
          "https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?auto=format&fit=crop&w=400&q=80",
      },
    ],
    totalProductAmount: 3890000,
    shippingFee: 30000,
    shopDiscountAmount: 400000,
    platformDiscountAmount: 30000,
    totalAmount: 3490000,
    recipientName: "Alex Morgan",
    recipientPhone: "(+84) 912 345 678",
    deliveryAddress:
      "TechHub Tower, 123 Dien Bien Phu Street, Floor 12, Da Kao Ward, District 1, Ho Chi Minh City",
    shippingCarrier: "Standard Express",
    trackingCode: "THEXP-VN-994820300",
    carrierHotline: "1900-5454-88",
    paymentMethod: "Pending Selection",
    paymentDetails: "Unpaid (Order will expire in 15 minutes)",
    trackingTimeline: [
      {
        status: "PENDING_PAYMENT",
        title: "Order Placed",
        description: "Waiting for payment authorization",
        timestamp: "Today, 11:30",
        completed: true,
        active: true,
      },
    ],
  },
  {
    id: "TH-88294028",
    orderNumber: "TH-88294028",
    shopName: "Razer Gaming Zone",
    createdAt: "Yesterday • 14:10",
    status: "DELIVERED",
    statusLabel: "Delivered",
    canReturn: true,
    items: [
      {
        id: "item-delivered",
        productName: "Tai nghe chơi game không dây Razer BlackShark V2 Pro 2024",
        variantName: "Black • HyperSpeed Wireless",
        sku: "RZ-BS-V2P-BK",
        price: 4990000,
        originalPrice: 5490000,
        quantity: 1,
        imageUrl:
          "https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=400&q=80",
      },
    ],
    totalProductAmount: 5490000,
    shippingFee: 40000,
    shopDiscountAmount: 500000,
    platformDiscountAmount: 40000,
    totalAmount: 4990000,
    recipientName: "Alex Morgan",
    recipientPhone: "(+84) 912 345 678",
    deliveryAddress:
      "TechHub Tower, 123 Dien Bien Phu Street, Floor 12, Da Kao Ward, District 1, Ho Chi Minh City",
    shippingCarrier: "TechHub Express Fast",
    trackingCode: "THEXP-VN-994820288",
    carrierHotline: "1900-5454-88",
    paymentMethod: "VNPay QR",
    paymentDetails: "Paid via VNPay QR",
    trackingTimeline: [
      {
        status: "PENDING_PAYMENT",
        title: "Order Placed",
        description: "Paid via VNPay",
        timestamp: "Yesterday, 14:10",
        completed: true,
      },
      {
        status: "CONFIRMED",
        title: "Order Confirmed",
        description: "Merchant accepted",
        timestamp: "Yesterday, 14:25",
        completed: true,
      },
      {
        status: "PROCESSING",
        title: "Packed",
        description: "Prepared and handed over",
        timestamp: "Yesterday, 16:00",
        completed: true,
      },
      {
        status: "SHIPPED",
        title: "In Transit",
        description: "Arrived at destination hub",
        timestamp: "Today, 08:00",
        completed: true,
      },
      {
        status: "DELIVERED",
        title: "Delivered",
        description: "Package received by Alex Morgan",
        timestamp: "Today, 10:45",
        completed: true,
        active: true,
      },
    ],
  },
  {
    id: "TH-88293998",
    orderNumber: "TH-88293998",
    shopName: "KeyChron Certified Store",
    createdAt: "Mar 15, 2026 • 11:00",
    status: "COMPLETED",
    statusLabel: "Completed",
    canReturn: true,
    items: [
      {
        id: "item-3",
        productName:
          "Bàn phím cơ không dây Keychron Q1 Pro QMK/VIA Custom Mechanical Keyboard",
        variantName: "Carbon Black • Red Switch • RGB",
        sku: "KC-Q1P-CB-RED",
        price: 4690000,
        originalPrice: 4990000,
        quantity: 1,
        imageUrl:
          "https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=400&q=80",
        hasReviewed: false,
      },
    ],
    totalProductAmount: 4990000,
    shippingFee: 30000,
    shopDiscountAmount: 300000,
    platformDiscountAmount: 30000,
    totalAmount: 4690000,
    recipientName: "Alex Morgan",
    recipientPhone: "(+84) 912 345 678",
    deliveryAddress:
      "TechHub Tower, 123 Dien Bien Phu Street, Floor 12, Da Kao Ward, District 1, Ho Chi Minh City",
    shippingCarrier: "TechHub Express",
    trackingCode: "THEXP-VN-994819920",
    carrierHotline: "1900-5454-88",
    paymentMethod: "Credit Card",
    paymentDetails: "Mastercard ending in •••• 8812",
    trackingTimeline: [
      {
        status: "COMPLETED",
        title: "Delivered & Confirmed",
        description: "Order completed with escrow disbursed to seller",
        timestamp: "Mar 17, 16:30",
        completed: true,
      },
    ],
  },
];
