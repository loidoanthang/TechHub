# TechHub Frontend Architecture & Implementation Guidelines

Tài liệu này ghi nhớ toàn bộ cấu trúc thư mục, quy tắc tổ chức source code và nguyên tắc triển khai (implement) cho dự án TechHub Frontend.

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
│   │   │   │   └── page.tsx         # Giỏ hàng
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

Mỗi module nghiệp vụ nằm trong `src/features/` được tổ chức khép kín:

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
   - Theme: Primary `#0D5BFF`, Font `Plus Jakarta Sans` / `Inter`.
