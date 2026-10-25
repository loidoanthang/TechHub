import type { Product } from "@/types/product";

export interface CategoryConfig {
  id: string;
  name: string;
  subcategories: { id: string; name: string }[];
  attributes: {
    key: string;
    label: string;
    options: string[];
  }[];
}

export const MOCK_CATEGORIES_DATA: CategoryConfig[] = [
  {
    id: "laptops",
    name: "Laptops & Ultrabooks",
    subcategories: [
      { id: "laptop-gaming", name: "Gaming Laptops" },
      { id: "laptop-ultrabook", name: "Slim Ultrabooks" },
      { id: "laptop-workstation", name: "Workstations & Creator" },
      { id: "macbook", name: "Apple MacBook" },
    ],
    attributes: [
      {
        key: "ram",
        label: "Memory (RAM)",
        options: ["8GB", "16GB", "32GB", "64GB+"],
      },
      {
        key: "cpu",
        label: "Processor (CPU)",
        options: [
          "Intel Core i5 / Ultra 5",
          "Intel Core i7 / Ultra 7",
          "Intel Core i9 / Ultra 9",
          "AMD Ryzen 7",
          "AMD Ryzen 9",
          "Apple M-Series",
        ],
      },
      {
        key: "gpu",
        label: "Graphics (GPU)",
        options: ["RTX 4060", "RTX 4070", "RTX 4080", "RTX 4090", "Intel Arc / Integrated"],
      },
      {
        key: "storage",
        label: "Storage Capacity",
        options: ["512GB SSD", "1TB SSD", "2TB SSD+"],
      },
    ],
  },
  {
    id: "phones",
    name: "Smartphones & Tablets",
    subcategories: [
      { id: "flagship-phones", name: "Flagship Phones" },
      { id: "gaming-phones", name: "Gaming Phones" },
      { id: "tablets", name: "Tablets & iPads" },
    ],
    attributes: [
      {
        key: "ram",
        label: "RAM Capacity",
        options: ["8GB", "12GB", "16GB", "24GB"],
      },
      {
        key: "storage",
        label: "Internal Storage",
        options: ["128GB", "256GB", "512GB", "1TB"],
      },
      {
        key: "screenSize",
        label: "Display Size",
        options: ["Under 6.1\"", "6.1\" - 6.5\"", "6.6\" - 6.8\"", "Foldable Display"],
      },
    ],
  },
  {
    id: "keyboards",
    name: "Custom Keyboards & Keycaps",
    subcategories: [
      { id: "custom-keyboard", name: "Pre-built Custom Keyboards" },
      { id: "keyboard-kit", name: "Barebone DIY Kits" },
      { id: "keycaps", name: "Keycap Sets (PBT/ABS)" },
      { id: "switches", name: "Mechanical Switches" },
    ],
    attributes: [
      {
        key: "layout",
        label: "Keyboard Layout",
        options: ["60% / 65%", "75%", "TKL (87-key)", "Fullsize (104+ key)"],
      },
      {
        key: "switchType",
        label: "Switch Type",
        options: ["Linear", "Tactile", "Clicky", "Magnetic / Hall Effect"],
      },
      {
        key: "connectivity",
        label: "Connectivity",
        options: ["Wired Type-C", "Tri-Mode (2.4G/BT/Wired)", "Bluetooth 5.3"],
      },
    ],
  },
  {
    id: "components",
    name: "PC Components & GPUs",
    subcategories: [
      { id: "gpu", name: "Graphics Cards (GPU)" },
      { id: "cpu", name: "Desktop Processors (CPU)" },
      { id: "motherboard", name: "Motherboards" },
      { id: "psu-case", name: "Power Supplies & Cases" },
    ],
    attributes: [
      {
        key: "vram",
        label: "VRAM Memory",
        options: ["8GB GDDR6", "12GB GDDR6X", "16GB GDDR6X", "24GB GDDR6X"],
      },
      {
        key: "socket",
        label: "Socket Type",
        options: ["LGA1700 (Intel)", "LGA1851 (Intel)", "AM5 (AMD)", "AM4 (AMD)"],
      },
    ],
  },
];

export const MOCK_BRANDS_DATA = [
  { id: "asus", name: "ASUS Republic of Gamers", count: 48 },
  { id: "razer", name: "Razer", count: 35 },
  { id: "keychron", name: "Keychron", count: 29 },
  { id: "wooting", name: "Wooting", count: 12 },
  { id: "logitech", name: "Logitech G", count: 41 },
  { id: "nuphy", name: "NuPhy", count: 18 },
  { id: "apple", name: "Apple", count: 24 },
  { id: "lenovo", name: "Lenovo Legion", count: 31 },
];

export const MOCK_SEARCH_PRODUCTS: Product[] = [
  {
    id: "search-1",
    title: "Keychron Q1 Pro Wireless Custom Mechanical Keyboard (QMK/VIA Barebone)",
    price: 199.0,
    rating: 4.9,
    reviewsCount: 142,
    vendorName: "Keychron Official",
    isVerifiedVendor: true,
    imageUrl: "https://images.unsplash.com/photo-1587829741301-dc798b83add3?q=80&w=600&auto=format&fit=crop",
    tags: ["75% Layout", "CNC Aluminum", "Wireless"],
  },
  {
    id: "search-2",
    title: "ASUS ROG Zephyrus G16 OLED Gaming Laptop (Intel Core Ultra 9, RTX 4080)",
    price: 2499.0,
    originalPrice: 2799.0,
    discountPercentage: 11,
    rating: 4.8,
    reviewsCount: 88,
    vendorName: "ASUS Republic Store",
    isVerifiedVendor: true,
    imageUrl: "https://images.unsplash.com/photo-1593642632823-8f785ba67e45?q=80&w=600&auto=format&fit=crop",
    tags: ["240Hz OLED", "RTX 4080", "32GB RAM"],
  },
  {
    id: "search-3",
    title: "NuPhy Air75 V2 Low-Profile Wireless Mechanical Keyboard",
    price: 119.0,
    rating: 4.8,
    reviewsCount: 210,
    vendorName: "NuPhy Studio",
    isVerifiedVendor: true,
    imageUrl: "https://images.unsplash.com/photo-1587829741301-dc798b83add3?q=80&w=600&auto=format&fit=crop",
    tags: ["Low-Profile", "1000Hz", "Gateron"],
  },
  {
    id: "search-4",
    title: "Wooting 60HE+ Analog Mechanical Keyboard Hall Effect",
    price: 175.0,
    rating: 5.0,
    reviewsCount: 520,
    vendorName: "Wooting Direct",
    isVerifiedVendor: true,
    imageUrl: "https://images.unsplash.com/photo-1595225476474-87563907a212?q=80&w=600&auto=format&fit=crop",
    tags: ["Rapid Trigger", "Lekker Switch"],
  },
  {
    id: "search-5",
    title: "Razer Blade 14 Gaming Laptop (AMD Ryzen 9 8945HS, RTX 4070)",
    price: 2199.0,
    rating: 4.7,
    reviewsCount: 64,
    vendorName: "Razer Store",
    isVerifiedVendor: true,
    imageUrl: "https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?q=80&w=600&auto=format&fit=crop",
    tags: ["QHD+ 240Hz", "CNC Aluminum"],
  },
  {
    id: "search-6",
    title: "Logitech G PRO X TKL LIGHTSPEED Wireless Gaming Keyboard",
    price: 199.0,
    originalPrice: 229.0,
    discountPercentage: 13,
    rating: 4.8,
    reviewsCount: 178,
    vendorName: "Logitech G Official",
    isVerifiedVendor: true,
    imageUrl: "https://images.unsplash.com/photo-1595225476474-87563907a212?q=80&w=600&auto=format&fit=crop",
    tags: ["LIGHTSPEED", "GX Brown", "RGB"],
  },
  {
    id: "search-7",
    title: "Corsair K70 MAX RGB Magnetic-Mechanical Gaming Keyboard",
    price: 229.0,
    rating: 4.6,
    reviewsCount: 92,
    vendorName: "Corsair Authorized",
    isVerifiedVendor: true,
    imageUrl: "https://images.unsplash.com/photo-1587829741301-dc798b83add3?q=80&w=600&auto=format&fit=crop",
    tags: ["MGX Switches", "Rapid Trigger"],
  },
  {
    id: "search-8",
    title: "Lenovo Legion Pro 7i Gen 9 (Intel Core i9-14900HX, RTX 4090)",
    price: 2999.0,
    rating: 4.9,
    reviewsCount: 110,
    vendorName: "Lenovo Enterprise Hub",
    isVerifiedVendor: true,
    imageUrl: "https://images.unsplash.com/photo-1593642632823-8f785ba67e45?q=80&w=600&auto=format&fit=crop",
    tags: ["RTX 4090 175W", "240Hz IPS", "Coldfront 5.0"],
  },
  {
    id: "search-9",
    title: "NuPhy Halo75 Wireless Mechanical Keyboard (Night Breeze)",
    price: 139.0,
    rating: 4.8,
    reviewsCount: 154,
    vendorName: "NuPhy Studio",
    isVerifiedVendor: true,
    imageUrl: "https://images.unsplash.com/photo-1587829741301-dc798b83add3?q=80&w=600&auto=format&fit=crop",
    tags: ["Halo Band", "Gasket Mount"],
  },
  {
    id: "search-10",
    title: "Keychron Q3 Max QMK/VIA Wireless Custom Mechanical Keyboard",
    price: 214.0,
    rating: 4.9,
    reviewsCount: 88,
    vendorName: "Keychron Official",
    isVerifiedVendor: true,
    imageUrl: "https://images.unsplash.com/photo-1595225476474-87563907a212?q=80&w=600&auto=format&fit=crop",
    tags: ["Acoustic Foam", "Double-Gasket"],
  },
  {
    id: "search-11",
    title: "ASUS ROG Strix SCAR 16 (Intel Core i9-14900HX, Mini LED)",
    price: 2899.0,
    rating: 4.9,
    reviewsCount: 96,
    vendorName: "ASUS Republic Store",
    isVerifiedVendor: true,
    imageUrl: "https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?q=80&w=600&auto=format&fit=crop",
    tags: ["ROG Nebula HDR", "RTX 4080"],
  },
  {
    id: "search-12",
    title: "Razer BlackWidow V4 Pro Mechanical Gaming Keyboard with Command Dial",
    price: 229.0,
    rating: 4.7,
    reviewsCount: 230,
    vendorName: "Razer Store",
    isVerifiedVendor: true,
    imageUrl: "https://images.unsplash.com/photo-1587829741301-dc798b83add3?q=80&w=600&auto=format&fit=crop",
    tags: ["Yellow Switch", "8 Dedicated Macro"],
  },
];
