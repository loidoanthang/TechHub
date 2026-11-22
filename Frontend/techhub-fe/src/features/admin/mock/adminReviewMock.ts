import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  AdminReviewQueryParams,
  AdminReviewResponse,
  AdminReviewStats,
} from "@/types/review";

let initialAdminReviews: AdminReviewResponse[] = [
  {
    id: "rev-001",
    orderItemId: "oi-101",
    orderId: "ord-881",
    orderNumber: "TH-20261118-091",
    productId: "prod-keychron-k2",
    productName: "Bàn Phím Cơ Không Dây Keychron K2 Pro Gateron G Pro Brown Switch",
    variantName: "Brown Switch / RGB Nhôm",
    sku: "KC-K2P-RGB-BR",
    imageUrl: "https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=400&q=80",
    shopId: "shop-techhub-gear",
    shopName: "TechHub Gear Store",
    rating: 1,
    comment: "Hàng giao bị gãy chân switch, cắm dây không nhận tín hiệu. Shop lừa đảo không chịu đổi trả, liên hệ zalo thì chửi khách hàng vô văn hóa, mọi người né shop này ra!",
    mediaList: [
      {
        id: "med-001",
        mediaUrl: "https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=800&q=80",
        mediaType: "IMAGE",
        displayOrder: 1,
      },
      {
        id: "med-002",
        mediaUrl: "https://images.unsplash.com/photo-1595225476474-87563907a212?auto=format&fit=crop&w=800&q=80",
        mediaType: "IMAGE",
        displayOrder: 2,
      },
    ],
    reviewerId: "user-buyer-01",
    reviewerName: "Trần Minh Quân",
    reviewerAvatar: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=120&q=80",
    sellerResponse: "Dạ chào bạn Quân, shop đã kiểm tra video đóng gói chân switch nguyên vẹn, bạn tự làm rớt gãy rồi dùng lời lẽ vu khống thô tục. Shop đã báo cáo lên Admin sàn TechHub xử lý.",
    sellerResponseAt: "2026-11-19T09:30:00Z",
    helpfulCount: 3,
    isReported: true,
    reportReason: "Ngôn từ xúc phạm, vu khống thông tin sai lệch về dịch vụ của Shop",
    reportCount: 2,
    createdAt: "2026-11-18T20:15:00Z",
  },
  {
    id: "rev-002",
    orderItemId: "oi-102",
    orderId: "ord-882",
    orderNumber: "TH-20261119-104",
    productId: "prod-galaxy-s24",
    productName: "Samsung Galaxy S24 Ultra 12GB/256GB Titan Đen",
    variantName: "Titanium Black / 256GB",
    sku: "SS-S24U-BLK-256",
    imageUrl: "https://images.unsplash.com/photo-1610945415295-d9bbf067e59c?auto=format&fit=crop&w=400&q=80",
    shopId: "shop-techhub-gear",
    shopName: "TechHub Gear Store",
    rating: 5,
    comment: "Máy chính hãng nguyên seal SSVN. Màn hình phẳng sắc nét, bút S-Pen viết siêu nhạy. Shop đóng hàng 4 lớp bọc chống sốc cực kỳ an tâm. 10 điểm chất lượng!",
    mediaList: [
      {
        id: "med-003",
        mediaUrl: "https://images.unsplash.com/photo-1610945415295-d9bbf067e59c?auto=format&fit=crop&w=800&q=80",
        mediaType: "IMAGE",
        displayOrder: 1,
      },
    ],
    reviewerId: "user-buyer-02",
    reviewerName: "Lê Hoàng Phúc",
    reviewerAvatar: "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?auto=format&fit=crop&w=120&q=80",
    sellerResponse: "Cảm ơn bạn Phúc rất nhiều đã tin tưởng lựa chọn TechHub Gear! Chúc bạn có trải nghiệm tuyệt vời với Galaxy S24 Ultra nhé ạ.",
    sellerResponseAt: "2026-11-20T10:00:00Z",
    helpfulCount: 12,
    isReported: false,
    reportCount: 0,
    createdAt: "2026-11-19T14:40:00Z",
  },
  {
    id: "rev-003",
    orderItemId: "oi-103",
    orderId: "ord-883",
    orderNumber: "TH-20261120-112",
    productId: "prod-logitech-superlight",
    productName: "Chuột Không Dây Công Thái Học Logitech G Pro X Superlight 2",
    variantName: "Trắng (White)",
    sku: "LOGI-GPX2-WHT",
    imageUrl: "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=400&q=80",
    shopId: "shop-techhub-gear",
    shopName: "TechHub Gear Store",
    rating: 1,
    comment: "Nhận kéo game liên tục, vào web http://casino-uytin999.com/tang-code nhận 500k miễn phí không cần nạp tiền. Liên hệ telegram: @koreatools để mua acc giá rẻ.",
    mediaList: [],
    reviewerId: "user-buyer-03",
    reviewerName: "BotSpam_882",
    reviewerAvatar: null,
    sellerResponse: null,
    sellerResponseAt: null,
    helpfulCount: 0,
    isReported: true,
    reportReason: "Spam liên kết độc hại, quảng cáo cờ bạc / cá độ bất hợp pháp",
    reportCount: 5,
    createdAt: "2026-11-20T08:12:00Z",
  },
  {
    id: "rev-004",
    orderItemId: "oi-104",
    orderId: "ord-884",
    orderNumber: "TH-20261120-130",
    productId: "prod-ipad-air-m2",
    productName: "iPad Air M2 11 inch Wi-Fi 128GB Chính Hãng Apple",
    variantName: "Xanh Ánh Sao / 128GB",
    sku: "APL-IPAD-M2-128-BLU",
    imageUrl: "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?auto=format&fit=crop&w=400&q=80",
    shopId: "shop-apple-flagship",
    shopName: "Apple Flagship Store",
    rating: 4,
    comment: "Máy đẹp, chip M2 mượt mà chơi Genshin không giật lag. Trừ 1 sao do bên vận chuyển giao chậm hơn dự kiến 2 ngày so với đơn hẹn.",
    mediaList: [],
    reviewerId: "user-buyer-04",
    reviewerName: "Nguyễn Thị Ngọc Ánh",
    reviewerAvatar: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=120&q=80",
    sellerResponse: "Chào bạn Ánh, Apple Flagship chân thành xin lỗi vì sự bất tiện do đơn vị vận chuyển giao trễ. Cảm ơn phản hồi quý giá của bạn!",
    sellerResponseAt: "2026-11-20T16:20:00Z",
    helpfulCount: 4,
    isReported: false,
    reportCount: 0,
    createdAt: "2026-11-20T11:05:00Z",
  },
  {
    id: "rev-005",
    orderItemId: "oi-105",
    orderId: "ord-885",
    orderNumber: "TH-20261121-145",
    productId: "prod-keychron-k2",
    productName: "Bàn Phím Cơ Không Dây Keychron K2 Pro Gateron G Pro Brown Switch",
    variantName: "Red Switch / LED Đơn",
    sku: "KC-K2P-WHT-RED",
    imageUrl: "https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=400&q=80",
    shopId: "shop-techhub-gear",
    shopName: "TechHub Gear Store",
    rating: 2,
    comment: "Gõ nhẹ nhưng âm thanh bị lọc xọc lò xo (ping) khá to, phải tháo ra tự lube lại. Keycap ABS mỏng dễ bóng mồ hôi.",
    mediaList: [
      {
        id: "med-004",
        mediaUrl: "https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=800&q=80",
        mediaType: "IMAGE",
        displayOrder: 1,
      },
    ],
    reviewerId: "user-buyer-05",
    reviewerName: "Đặng Tiến Dũng",
    reviewerAvatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=120&q=80",
    sellerResponse: null,
    sellerResponseAt: null,
    helpfulCount: 6,
    isReported: false,
    reportCount: 0,
    createdAt: "2026-11-21T09:30:00Z",
  },
  {
    id: "rev-006",
    orderItemId: "oi-106",
    orderId: "ord-886",
    orderNumber: "TH-20261121-160",
    productId: "prod-sony-wh1000xm5",
    productName: "Tai Nghe Chống Ồn Không Dây Sony WH-1000XM5",
    variantName: "Bạc (Silver)",
    sku: "SONY-WHXM5-SLV",
    imageUrl: "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=400&q=80",
    shopId: "shop-sony-official",
    shopName: "Sony Official Store",
    rating: 1,
    comment: "Mới dùng 3 ngày nghe tiếng rẹt rẹt tai bên phải. Đem ra TTBH thì kêu do ẩm không bảo hành, dịch vụ như đấm vào tai khách hàng!",
    mediaList: [
      {
        id: "med-005",
        mediaUrl: "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=800&q=80",
        mediaType: "IMAGE",
        displayOrder: 1,
      },
    ],
    reviewerId: "user-buyer-06",
    reviewerName: "Vũ Hải Đăng",
    reviewerAvatar: null,
    sellerResponse: "Chào anh Đăng, Sony Store đã tiếp nhận khiếu nại và chuyển lại bộ phận kỹ thuật để thẩm định lại trường hợp của anh ngay trong hôm nay ạ.",
    sellerResponseAt: "2026-11-21T15:00:00Z",
    helpfulCount: 9,
    isReported: true,
    reportReason: "Khách hàng sử dụng từ ngữ xúc phạm trung tâm bảo hành",
    reportCount: 1,
    createdAt: "2026-11-21T12:00:00Z",
  },
];

export function getMockAdminReviewStats(): AdminReviewStats {
  const totalReviews = initialAdminReviews.length;
  const reportedReviews = initialAdminReviews.filter((r) => r.isReported).length;
  const negativeReviews = initialAdminReviews.filter((r) => r.rating <= 2).length;
  const sumRating = initialAdminReviews.reduce((sum, r) => sum + r.rating, 0);
  const averageRating = totalReviews > 0 ? Number((sumRating / totalReviews).toFixed(1)) : 5.0;

  return {
    totalReviews,
    reportedReviews,
    negativeReviews,
    averageRating,
  };
}

export function removeMockAdminReview(reviewId: string): boolean {
  const prevCount = initialAdminReviews.length;
  initialAdminReviews = initialAdminReviews.filter((r) => r.id !== reviewId);
  return initialAdminReviews.length < prevCount;
}

export function getMockAdminReviews(
  params?: AdminReviewQueryParams
): ApiResponse<PageResponse<AdminReviewResponse>> {
  let filtered = [...initialAdminReviews];

  // 1. Keyword search (content, reviewerName, shopName, productName)
  if (params?.keyword) {
    const kw = params.keyword.trim().toLowerCase();
    filtered = filtered.filter(
      (r) =>
        r.comment?.toLowerCase().includes(kw) ||
        r.reviewerName.toLowerCase().includes(kw) ||
        r.productName.toLowerCase().includes(kw) ||
        (r.shopName && r.shopName.toLowerCase().includes(kw)) ||
        (r.orderNumber && r.orderNumber.toLowerCase().includes(kw))
    );
  }

  // 2. Rating filter
  if (params?.rating !== undefined && params?.rating !== null) {
    filtered = filtered.filter((r) => r.rating === params.rating);
  }

  // 3. hasMedia filter
  if (params?.hasMedia !== undefined && params?.hasMedia !== null) {
    filtered = filtered.filter((r) => (params.hasMedia ? r.mediaList.length > 0 : r.mediaList.length === 0));
  }

  // 4. isReported filter
  if (params?.isReported !== undefined && params?.isReported !== null) {
    filtered = filtered.filter((r) => Boolean(r.isReported) === params.isReported);
  }

  // 5. Date filter
  if (params?.fromDate) {
    filtered = filtered.filter((r) => r.createdAt >= params.fromDate!);
  }
  if (params?.toDate) {
    filtered = filtered.filter((r) => r.createdAt <= params.toDate!);
  }

  // 6. Sorting
  const sortBy = params?.sortBy || "createdAt";
  const sortDirection = params?.sortDirection || "DESC";

  filtered.sort((a, b) => {
    let comparison = 0;
    if (sortBy === "rating") {
      comparison = a.rating - b.rating;
    } else if (sortBy === "reportCount") {
      comparison = (a.reportCount || 0) - (b.reportCount || 0);
    } else {
      comparison = new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
    }
    return sortDirection === "DESC" ? -comparison : comparison;
  });

  // 7. Pagination
  const page = params?.page ?? 0;
  const size = params?.size ?? 10;
  const totalElements = filtered.length;
  const totalPages = Math.ceil(totalElements / size) || 1;
  const startIndex = page * size;
  const pageContent = filtered.slice(startIndex, startIndex + size);

  return {
    success: true,
    message: "Admin reviews retrieved successfully (Fallback Mock)",
    data: {
      content: pageContent,
      pageNumber: page,
      pageSize: size,
      totalElements,
      totalPages,
      last: page >= totalPages - 1,
    },
  };
}
