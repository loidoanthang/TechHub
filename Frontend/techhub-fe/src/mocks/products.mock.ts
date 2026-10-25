import type { ProductDetailData } from "@/types/productDetail";

export const MOCK_PRODUCT_DETAILS: Record<string, ProductDetailData> = {
  "default": {
    id: "keychron-q1-max",
    slug: "keychron-q1-max-wireless-k8f2a1",
    title: "Keychron Q1 Max Full-Metal Wireless Mechanical Keyboard (Gateron Jupiter)",
    brandName: "Keychron",
    categoryName: "Custom Keyboards",
    categorySlug: "keyboards",
    flagshipBadge: "FLAGSHIP GEAR",
    rating: 4.9,
    reviewsCount: 142,
    soldCount: 620,
    images: [
      "https://images.unsplash.com/photo-1587829741301-dc798b83add3?q=80&w=1200&auto=format&fit=crop",
      "https://images.unsplash.com/photo-1595225476474-87563907a212?q=80&w=1200&auto=format&fit=crop",
      "https://images.unsplash.com/photo-1593642632823-8f785ba67e45?q=80&w=1200&auto=format&fit=crop",
    ],
    attributeGroups: [
      {
        name: "Colorway",
        values: ["Carbon Black", "Shell White", "Retro Gray"],
      },
      {
        name: "Switch Type",
        values: ["Jupiter Red (Linear)", "Jupiter Brown (Tactile)", "Jupiter Banana (Early Bump)"],
      },
    ],
    variants: [
      {
        id: "v-cb-red",
        sku: "KC-Q1M-CB-RED",
        name: "Carbon Black • Jupiter Red",
        attributes: {
          "Colorway": "Carbon Black",
          "Switch Type": "Jupiter Red (Linear)",
        },
        price: 4690000,
        originalPrice: 4990000,
        stock: 18,
        imageUrl:
          "https://images.unsplash.com/photo-1587829741301-dc798b83add3?q=80&w=1200&auto=format&fit=crop",
        status: "ACTIVE",
      },
      {
        id: "v-cb-brown",
        sku: "KC-Q1M-CB-BRN",
        name: "Carbon Black • Jupiter Brown",
        attributes: {
          "Colorway": "Carbon Black",
          "Switch Type": "Jupiter Brown (Tactile)",
        },
        price: 4690000,
        originalPrice: 4990000,
        stock: 12,
        imageUrl:
          "https://images.unsplash.com/photo-1587829741301-dc798b83add3?q=80&w=1200&auto=format&fit=crop",
        status: "ACTIVE",
      },
      {
        id: "v-cb-banana",
        sku: "KC-Q1M-CB-BAN",
        name: "Carbon Black • Jupiter Banana",
        attributes: {
          "Colorway": "Carbon Black",
          "Switch Type": "Jupiter Banana (Early Bump)",
        },
        price: 4890000,
        originalPrice: 5190000,
        stock: 8,
        imageUrl:
          "https://images.unsplash.com/photo-1587829741301-dc798b83add3?q=80&w=1200&auto=format&fit=crop",
        status: "ACTIVE",
      },
      {
        id: "v-sw-red",
        sku: "KC-Q1M-SW-RED",
        name: "Shell White • Jupiter Red",
        attributes: {
          "Colorway": "Shell White",
          "Switch Type": "Jupiter Red (Linear)",
        },
        price: 4890000,
        originalPrice: 5290000,
        stock: 5,
        imageUrl:
          "https://images.unsplash.com/photo-1595225476474-87563907a212?q=80&w=1200&auto=format&fit=crop",
        status: "ACTIVE",
      },
      {
        id: "v-sw-banana",
        sku: "KC-Q1M-SW-BAN",
        name: "Shell White • Jupiter Banana",
        attributes: {
          "Colorway": "Shell White",
          "Switch Type": "Jupiter Banana (Early Bump)",
        },
        price: 4990000,
        originalPrice: 5390000,
        stock: 0, // Out of stock to test cross-attribute disabled behavior!
        imageUrl:
          "https://images.unsplash.com/photo-1595225476474-87563907a212?q=80&w=1200&auto=format&fit=crop",
        status: "ACTIVE",
      },
      {
        id: "v-rg-brown",
        sku: "KC-Q1M-RG-BRN",
        name: "Retro Gray • Jupiter Brown",
        attributes: {
          "Colorway": "Retro Gray",
          "Switch Type": "Jupiter Brown (Tactile)",
        },
        price: 4790000,
        originalPrice: 5090000,
        stock: 14,
        imageUrl:
          "https://images.unsplash.com/photo-1593642632823-8f785ba67e45?q=80&w=1200&auto=format&fit=crop",
        status: "ACTIVE",
      },
    ],
    specs: [
      { label: "Chassis Material", value: "CNC Machined 6063 Aluminum" },
      { label: "Plate Material", value: "Acoustic KTT Polycarbonate (PC)" },
      { label: "Keycaps Included", value: "KSA Double-Shot PBT (Non-shine)" },
      { label: "Switch Mount", value: "Hot-swappable 3-pin & 5-pin" },
      { label: "Polling Rate", value: "1000 Hz (2.4G & Wired) / 90 Hz (BT)" },
      { label: "Battery Capacity", value: "4000mAh Rechargeable Li-Polymer" },
      { label: "Total Weight", value: "1,820 g ± 10 g (Fully Assembled)" },
      { label: "OS Compatibility", value: "macOS / Windows 11 / Linux / iOS / Android" },
      { label: "Stabilizers", value: "PCB-Mounted Screw-in Stabilizers" },
      { label: "Backlight", value: "South-Facing RGB (22 dynamic modes)" },
    ],
    merchant: {
      id: "m-1",
      name: "Keychron Direct Official Store",
      sellerName: "keychron-official",
      avatarText: "KD",
      joinedText: "Joined TechHub 3 years ago • Official Mechanical Keyboard Partner",
      rating: 4.9,
      positiveFeedback: "98.9%",
      totalProducts: 64,
      verified: true,
      status: "ACTIVE",
    },
    description: {
      intro: [
        "The Keychron Q1 Max is a flagship full-metal 75% layout wireless custom mechanical keyboard engineered for mechanical keyboard enthusiasts and demanding professionals. Featuring an upgraded 2.4 GHz wireless connection alongside Bluetooth 5.1 and wired Type-C connectivity, the Q1 Max offers zero-latency performance across macOS, Windows, and Linux environments.",
        "Crafted from precision CNC-machined 6063 aluminum, every keyboard chassis undergoes 24 rigorous manufacturing stages—including sandblasting, anodization, and surface polishing. An advanced double-gasket acoustic architecture pairs with a six-tier dampening system (IXPE switch pads, acoustic PET film, latex dampening foam, and Poron case base) to produce the signature deep marble \"thock\" bottom-out sound profile right out of the box without any DIY modding required.",
      ],
      features: [
        "Tri-Mode Connectivity: Seamlessly switch between lag-free 2.4 GHz wireless with 1000 Hz polling rate, Bluetooth 5.1 (up to 3 simultaneous devices), and robust USB Type-C wired mode.",
        "Full QMK / VIA Programmability: Remap every single key, dial rotary encoder gestures, and author custom macro layers directly inside your web browser via Keychron Web Launcher without third-party bloatware.",
        "Hot-Swappable PCB: Compatible with almost all standard 3-pin and 5-pin MX-style mechanical switches (including Cherry, Gateron, Kailh, and Panda) with zero soldering required.",
        "Factory-Lubed Gateron Jupiter Switches: Custom-engineered molds with high-precision factory Krytox lubrication for silk-smooth linear actuation and tactile response.",
        "South-Facing RGB Backlight: 22 dynamic RGB backlighting modes designed to shine brilliantly toward the user even with thick non-shine-through PBT keycaps.",
        "Screw-in PCB Stabilizers: Gold-plated screw-in design eliminates wobble on large keys (Spacebar, Shift, Enter, Backspace) for balanced key travel.",
      ],
      materials: [
        "Chassis: 6063 full aluminum alloy, anodized finish.",
        "Plate: Acoustic polycarbonate (PC) flex plate.",
        "Keycaps: Double-shot KSA profile PBT (oil-resistant, non-shine).",
        "Dampening Layers: IXPE foam + PET sound film + Latex sound-absorbing pad + Poron base foam.",
      ],
      inTheBox: [
        "1x Keychron Q1 Max fully assembled keyboard (switches and keycaps installed)",
        "1x Type-C to Type-C Braided Cable with Type-A to Type-C USB Adapter",
        "1x 2.4 GHz Type-A Wireless Receiver & Extension Adapter",
        "1x 2-in-1 Premium Switch & Keycap Puller Tool",
        "1x Hex Key & Screwdriver Set with extra replacement screws and rubber feet",
        "1x Quick Start Guide & Keymap Reference Sheet",
      ],
    },
    reviewStats: {
      average: 4.9,
      totalReviews: 142,
      breakdown: [
        { star: 5, percentage: 88 },
        { star: 4, percentage: 9 },
        { star: 3, percentage: 2 },
        { star: 2, percentage: 1 },
        { star: 1, percentage: 0 },
      ],
    },
    reviews: [
      {
        id: "rev-1",
        userName: "Marcus R.",
        userAvatarText: "MR",
        avatarColor: "bg-[#dce9ff] text-[#0045cc]",
        date: "2 days ago",
        rating: 5,
        comment:
          'The weight of this keyboard is sensational—almost 2 kilos of pure aluminum. The Jupiter Banana switches combined with the acoustic foam dampening create that deep, satisfying "rain on glass" marble sound straight out of the box without any tape modding needed. 2.4GHz wireless has zero noticeable latency for competitive CS2 play.',
        helpfulCount: 48,
      },
      {
        id: "rev-2",
        userName: "Elena Lindqvist",
        userAvatarText: "EL",
        avatarColor: "bg-emerald-100 text-emerald-700",
        date: "1 week ago",
        rating: 5,
        comment:
          "Upgraded from a plastic gaming board and the tactile quality is night and day. VIA keymapping in Google Chrome took 30 seconds. Battery easily lasts 3 weeks with RGB turned off.",
        helpfulCount: 19,
      },
    ],
    recommendations: [
      {
        id: "rec-acc-1",
        title: "Gateron Jupiter Mechanical Switch Pack (110 Pcs)",
        brand: "Keychron Partner",
        price: 28.0,
        rating: 4.9,
        reviewsCount: 184,
        imageUrl:
          "https://images.unsplash.com/photo-1587829741301-dc798b83add3?q=80&w=400&auto=format&fit=crop",
        specs: ["Factory Pre-Lubed", "5-Pin PCB Mount", "Operating Force 59g"],
      },
      {
        id: "rec-acc-2",
        title: "Braided Coiled Aviator Keyboard Cable (Type-C to USB-A)",
        brand: "CableMod Hardware",
        price: 34.5,
        rating: 4.8,
        reviewsCount: 95,
        imageUrl:
          "https://images.unsplash.com/photo-1595225476474-87563907a212?q=80&w=400&auto=format&fit=crop",
        specs: ["GX16 5-Pin Aviator", "Type-C to USB-A", "1.5m Total Reach"],
      },
      {
        id: "rec-acc-3",
        title: "Topographic XL Ultra-Glide Desk Mat",
        brand: "Deskr Authorized",
        price: 24.0,
        rating: 4.9,
        reviewsCount: 340,
        imageUrl:
          "https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?q=80&w=400&auto=format&fit=crop",
        specs: ["900 × 400 × 4mm Size", "Micro-Woven Cloth", "Precision Stitched Edges"],
      },
    ],
  },
};

export function getMockProductDetail(id: string): ProductDetailData {
  return MOCK_PRODUCT_DETAILS[id] || {
    ...MOCK_PRODUCT_DETAILS["default"],
    id,
    title: id
      .split("-")
      .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
      .join(" ") + " High-Performance Hardware",
  };
}
