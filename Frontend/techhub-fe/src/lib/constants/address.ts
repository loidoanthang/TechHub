export interface DistrictData {
  name: string;
  wards: string[];
}

export interface ProvinceData {
  name: string;
  districts: DistrictData[];
}

// 63 Tinh / Thanh pho Viet Nam
export const VIETNAM_PROVINCE_NAMES: string[] = [
  "TP. Hồ Chí Minh",
  "Hà Nội",
  "Đà Nẵng",
  "Hải Phòng",
  "Cần Thơ",
  "An Giang",
  "Bà Rịa - Vũng Tàu",
  "Bắc Giang",
  "Bắc Kạn",
  "Bạc Liêu",
  "Bắc Ninh",
  "Bến Tre",
  "Bình Định",
  "Bình Dương",
  "Bình Phước",
  "Bình Thuận",
  "Cà Mau",
  "Cao Bằng",
  "Đắk Lắk",
  "Đắk Nông",
  "Điện Biên",
  "Đồng Nai",
  "Đồng Tháp",
  "Gia Lai",
  "Hà Giang",
  "Hà Nam",
  "Hà Tĩnh",
  "Hải Dương",
  "Hậu Giang",
  "Hòa Bình",
  "Hưng Yên",
  "Khánh Hòa",
  "Kiên Giang",
  "Kon Tum",
  "Lai Châu",
  "Lâm Đồng",
  "Lạng Sơn",
  "Lào Cai",
  "Long An",
  "Nam Định",
  "Nghệ An",
  "Ninh Bình",
  "Ninh Thuận",
  "Phú Thọ",
  "Phú Yên",
  "Quảng Bình",
  "Quảng Nam",
  "Quảng Ngãi",
  "Quảng Ninh",
  "Quảng Trị",
  "Sóc Trăng",
  "Sơn La",
  "Tây Ninh",
  "Thái Bình",
  "Thái Nguyên",
  "Thanh Hóa",
  "Thừa Thiên Huế",
  "Tiền Giang",
  "Trà Vinh",
  "Tuyên Quang",
  "Vĩnh Long",
  "Vĩnh Phúc",
  "Yên Bái",
];

// Chi tiet quan huyen va phuong xa cho cac tinh thanh trong diem va danh sach 63 tinh thanh
export const VIETNAM_PROVINCES: ProvinceData[] = [
  {
    name: "TP. Hồ Chí Minh",
    districts: [
      {
        name: "Quận 1",
        wards: [
          "Phường Bến Nghé",
          "Phường Bến Thành",
          "Phường Đa Kao",
          "Phường Tân Định",
          "Phường Phạm Ngũ Lão",
        ],
      },
      {
        name: "Quận 3",
        wards: ["Phường Võ Thị Sáu", "Phường 1", "Phường 2", "Phường 3"],
      },
      {
        name: "Quận 7",
        wards: ["Phường Tân Phong", "Phường Tân Phú", "Phường Phú Mỹ"],
      },
      {
        name: "Quận 10",
        wards: ["Phường 12", "Phường 14", "Phường 15"],
      },
      {
        name: "Thành phố Thủ Đức",
        wards: [
          "Phường Thảo Điền",
          "Phường An Phú",
          "Phường Hiệp Phú",
          "Phường Linh Trung",
        ],
      },
      {
        name: "Quận Bình Thạnh",
        wards: ["Phường 19", "Phường 22", "Phường 25"],
      },
      {
        name: "Quận Tân Bình",
        wards: ["Phường 2", "Phường 4", "Phường 12"],
      },
      {
        name: "Quận Phú Nhuận",
        wards: ["Phường 1", "Phường 2", "Phường 7"],
      },
    ],
  },
  {
    name: "Hà Nội",
    districts: [
      {
        name: "Quận Ba Đình",
        wards: ["Phường Kim Mã", "Phường Cống Vị", "Phường Liễu Giai"],
      },
      {
        name: "Quận Hoàn Kiếm",
        wards: ["Phường Hàng Bạc", "Phường Tràng Tiền", "Phường Cửa Nam"],
      },
      {
        name: "Quận Cầu Giấy",
        wards: ["Phường Dịch Vọng", "Phường Yên Hòa", "Phường Trung Hòa"],
      },
      {
        name: "Quận Đống Đa",
        wards: ["Phường Láng Hạ", "Phường Ô Chợ Dừa", "Phường Văn Miếu"],
      },
      {
        name: "Quận Hai Bà Trưng",
        wards: ["Phường Bạch Đằng", "Phường Bách Khoa"],
      },
      {
        name: "Quận Tây Hồ",
        wards: ["Phường Quảng An", "Phường Xuân La"],
      },
    ],
  },
  {
    name: "Đà Nẵng",
    districts: [
      {
        name: "Quận Hải Châu",
        wards: ["Phường Thạch Thang", "Phường Thuận Phước", "Phường Hòa Thuận Đông"],
      },
      {
        name: "Quận Sơn Trà",
        wards: ["Phường An Hải Bắc", "Phường Phước Mỹ"],
      },
      {
        name: "Quận Thanh Khê",
        wards: ["Phường Vĩnh Trung", "Phường Tam Thuận"],
      },
      {
        name: "Quận Ngũ Hành Sơn",
        wards: ["Phường Mỹ An", "Phường Khuê Mỹ"],
      },
    ],
  },
  {
    name: "Hải Phòng",
    districts: [
      {
        name: "Quận Hồng Bàng",
        wards: ["Phường Hoàng Văn Thụ", "Phường Minh Khai"],
      },
      {
        name: "Quận Ngô Quyền",
        wards: ["Phường Lạc Viên", "Phường Cầu Đất"],
      },
      {
        name: "Quận Lê Chân",
        wards: ["Phường An Biên", "Phường Lam Sơn"],
      },
    ],
  },
  {
    name: "Cần Thơ",
    districts: [
      {
        name: "Quận Ninh Kiều",
        wards: ["Phường Tân An", "Phường An Khánh", "Phường Xuân Khánh"],
      },
      {
        name: "Quận Bình Thủy",
        wards: ["Phường Bình Thủy", "Phường Trà Nóc"],
      },
      {
        name: "Quận Cái Răng",
        wards: ["Phường Lê Bình", "Phường Hưng Phú"],
      },
    ],
  },
  {
    name: "Bình Dương",
    districts: [
      {
        name: "Thành phố Thủ Dầu Một",
        wards: ["Phường Phú Hòa", "Phường Hiệp Thành"],
      },
      {
        name: "Thành phố Thuận An",
        wards: ["Phường Lái Thiêu", "Phường An Phú"],
      },
      {
        name: "Thành phố Dĩ An",
        wards: ["Phường Dĩ An", "Phường Tân Đông Hiệp"],
      },
    ],
  },
  {
    name: "Đồng Nai",
    districts: [
      {
        name: "Thành phố Biên Hòa",
        wards: ["Phường Quyết Thắng", "Phường Thống Nhất", "Phường Tân Phong"],
      },
      {
        name: "Huyện Long Thành",
        wards: ["Thị trấn Long Thành", "Xã An Phước"],
      },
    ],
  },
  ...VIETNAM_PROVINCE_NAMES.filter(
    (name) =>
      ![
        "TP. Hồ Chí Minh",
        "Hà Nội",
        "Đà Nẵng",
        "Hải Phòng",
        "Cần Thơ",
        "Bình Dương",
        "Đồng Nai",
      ].includes(name)
  ).map((name) => ({
    name,
    districts: [
      {
        name: "Thành phố / Thị xã trung tâm",
        wards: ["Phường trung tâm", "Xã / Thị trấn"],
      },
      {
        name: "Khu vực huyện / ngoại thành",
        wards: ["Xã / Thị trấn 1", "Xã / Thị trấn 2"],
      },
    ],
  })),
];
