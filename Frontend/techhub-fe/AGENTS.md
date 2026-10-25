<!-- BEGIN:nextjs-agent-rules -->

## This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` (resolved from this file's directory; in monorepos the `next` package may not be visible from the repo root) before writing any code. Heed deprecation notices.

This block is written and re-added by `next dev` — verify at `node_modules/next/dist/server/lib/generate-agent-files.js`. Removing it from a diff only re-creates the uncommitted change; committing it with your work keeps the tree clean.

<!-- END:nextjs-agent-rules -->

# TechHub Frontend Architecture & Implementation Guidelines

Khi thực hiện (implement) bất kỳ tính năng, trang hoặc component nào cho TechHub Frontend, BẮT BUỘC tuân thủ cấu trúc thư mục và quy tắc kiến trúc dưới đây.

---

## 1. Cấu trúc thư mục chuẩn (Folder Hierarchy)

Dự án áp dụng mô hình **Feature-Based Architecture kết hợp Next.js App Router (Thin Pages)**:

```text
techhub-fe/
├── public/
│   ├── images/
│   └── icons/
│
├── src/
│   ├── app/                         # Next.js App Router (Thin Pages & Layouts only)
│   │   ├── layout.tsx               # Root layout
│   │   ├── globals.css
│   │   ├── not-found.tsx
│   │   │
│   │   ├── (storefront)/            # Nhóm trang khách hàng mua sắm (Storefront)
│   │   │   ├── layout.tsx
│   │   │   ├── page.tsx             # Trang chủ Marketplace
│   │   │   ├── products/
│   │   │   │   ├── page.tsx         # Danh sách & Bộ lọc sản phẩm
│   │   │   │   └── [productId]/
│   │   │   │       └── page.tsx     # Chi tiết sản phẩm
│   │   │   ├── search/
│   │   │   │   └── page.tsx         # Tìm kiếm nâng cao
│   │   │   ├── cart/
│   │   │   │   └── page.tsx         # Giỏ hàng & Drawer
│   │   │   └── checkout/
│   │   │       └── page.tsx         # Thanh toán & Đặt hàng
│   │   │
│   │   ├── (auth)/                  # Xác thực người dùng (Auth Flow)
│   │   │   ├── layout.tsx
│   │   │   ├── login/page.tsx
│   │   │   ├── register/page.tsx
│   │   │   ├── forgot-password/page.tsx
│   │   │   ├── reset-password/page.tsx
│   │   │   ├── verify-otp/page.tsx
│   │   │   └── suspended/page.tsx
│   │   │
│   │   ├── account/                 # Cổng tài khoản Buyer
│   │   │   ├── layout.tsx
│   │   │   ├── profile/page.tsx     # Thông tin cá nhân
│   │   │   ├── addresses/page.tsx   # Sổ địa chỉ nhận hàng
│   │   │   └── orders/
│   │   │       ├── page.tsx         # Đơn mua của tôi
│   │   │       └── [orderId]/page.tsx
│   │   │
│   │   ├── seller/                  # Kênh Người Bán (Seller Portal)
│   │   │   ├── layout.tsx
│   │   │   ├── register/page.tsx    # Đăng ký bán hàng
│   │   │   ├── dashboard/page.tsx
│   │   │   ├── products/
│   │   │   │   ├── page.tsx         # Quản lý tất cả sản phẩm
│   │   │   │   ├── new/page.tsx     # Tạo sản phẩm mới
│   │   │   │   └── [productId]/edit/page.tsx
│   │   │   └── orders/
│   │   │       ├── page.tsx         # Danh sách đơn bán
│   │   │       └── [orderId]/page.tsx
│   │   │
│   │   └── admin/                   # Kênh Quản trị (Admin Portal)
│   │       ├── layout.tsx
│   │       ├── dashboard/page.tsx
│   │       ├── products/page.tsx
│   │       ├── orders/page.tsx
│   │       ├── users/page.tsx
│   │       └── categories/page.tsx
│   │
│   ├── features/                    # Logic & UI theo từng nghiệp vụ (Business Domain)
│   │   ├── auth/
│   │   ├── products/
│   │   ├── cart/
│   │   ├── checkout/
│   │   ├── orders/
│   │   ├── account/
│   │   ├── seller/
│   │   └── admin/
│   │
│   ├── components/
│   │   ├── ui/                      # Base primitive components (shadcn/ui, Radix UI)
│   │   └── shared/                  # Header, Footer, Navbar, Breadcrumb, EmptyState...
│   │
│   ├── lib/
│   │   ├── api/                     # Axios/Fetch client, interceptors, error handling
│   │   ├── utils.ts                 # cn, formatters
│   │   └── constants.ts             # App config, routes, local storage keys
│   │
│   ├── hooks/                       # Custom hooks dùng chung (useDebounce, useMediaQuery...)
│   ├── providers/                   # Context/State providers (React Query, Auth, Toast...)
│   └── types/                       # Core shared types (ApiResponse<T>, Pagination, BaseUser...)
│
├── package.json
├── tsconfig.json
└── next.config.ts
```

---

## 2. Quy chuẩn trong từng thư mục Feature (`src/features/<feature-name>/`)

Mỗi module nghiệp vụ nằm trong `src/features/` phải được tổ chức cô lập và khép kín:

```text
src/features/<feature-name>/
├── api/             # React Query hooks hoặc API fetch functions của riêng feature
├── components/      # UI components độc quyền cho feature (VD: ProductCard, ProductSpecs, CartDrawer)
├── hooks/           # Custom hook nghiệp vụ nội bộ (VD: useCartCalculation, useAddressForm)
├── types/           # Type/Interface phục vụ feature (DTOs, Form schema types)
└── utils/           # Helper formats/mappers riêng của feature
```

---

## 3. Nguyên tắc khi Code (Coding Principles)

1. **Thin Pages trong `src/app/`**:
   - File `page.tsx` trong `src/app/` chỉ nhận dynamic params/searchParams, gán metadata và render component chính từ `src/features/`. Không viết logic dài hay JSX phức tạp trong `page.tsx`.
2. **Component Separation**:
   - `components/ui/`: Các thành phần primitive không dính business logic (Button, Input, Modal, Dropdown).
   - `components/shared/`: Layout components dùng chung nhiều nơi (Header, Footer, Navbar, Global Search Bar).
   - `features/<feature>/components/`: Chứa các component đặc thù của tính năng.
3. **Data Fetching & State**:
   - Sử dụng **TanStack Query (React Query)** cho server state.
   - API Client tập trung tại `src/lib/api/` có gắn interceptors đồng bộ với Backend Spring Boot (Bearer token, ApiResponse unwrap, Error handling).
4. **Tham chiếu UI từ Stitch**:
   - Dự án Stitch: `TechHub Electronics Marketplace` (Project ID: `13321818460421815627`).
   - Luôn bám sát design theme: màu chính `#0D5BFF` (Electric Cobalt), font `Plus Jakarta Sans` / `Inter`, bo góc mềm và layout Bento/Card đã chốt trong Design System.
