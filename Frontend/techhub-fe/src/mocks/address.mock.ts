export interface AddressDTO {
  id: string; // UUID v4
  userId?: string;
  recipientName: string;
  phone: string;
  province: string; // City / Province
  district: string; // District / County
  ward: string; // Ward / Sub-district
  streetAddress: string; // Detailed street address, suite, apartment
  label: "HOME" | "OFFICE" | "OTHER";
  isDefault: boolean;
  phoneTag?: string; // Direct Mobile, Ext. 409 (Hardware Desk), etc.
}

export interface ProvinceData {
  name: string;
  districts: {
    name: string;
    wards: string[];
  }[];
}

export const VIETNAM_PROVINCES: ProvinceData[] = [
  {
    name: "Ho Chi Minh City",
    districts: [
      {
        name: "District 1",
        wards: ["Da Kao Ward", "Ben Nghe Ward", "Ben Thanh Ward", "Tan Dinh Ward"],
      },
      {
        name: "District 7",
        wards: ["Tan Phong Ward", "Tan Phu Ward", "Phu My Ward"],
      },
      {
        name: "Thu Duc City",
        wards: ["Thao Dien Ward", "An Phu Ward", "Hiep Phu Ward", "Linh Trung Ward"],
      },
      {
        name: "Binh Thanh District",
        wards: ["Ward 19", "Ward 22", "Ward 25"],
      },
    ],
  },
  {
    name: "Ha Noi Capital",
    districts: [
      {
        name: "Ba Dinh District",
        wards: ["Kim Ma Ward", "Cong Vi Ward", "Lieu Giai Ward"],
      },
      {
        name: "Cau Giay District",
        wards: ["Dich Vong Ward", "Yen Hoa Ward", "Trung Hoa Ward"],
      },
      {
        name: "Hoan Kiem District",
        wards: ["Hang Bac Ward", "Trang Tien Ward", "Cua Nam Ward"],
      },
    ],
  },
  {
    name: "Da Nang City",
    districts: [
      {
        name: "Hai Chau District",
        wards: ["Thach Thang Ward", "Thuan Phuoc Ward", "Hoa Thuan Dong Ward"],
      },
      {
        name: "Son Tra District",
        wards: ["An Hai Bac Ward", "Phuoc My Ward"],
      },
    ],
  },
];

export const MOCK_ADDRESSES: AddressDTO[] = [
  {
    id: "addr-001",
    recipientName: "Alex Morgan",
    phone: "(+84) 912 345 678",
    province: "Ho Chi Minh City",
    district: "District 1",
    ward: "Da Kao Ward",
    streetAddress: "TechHub Tower, 123 Dien Bien Phu Street, Floor 12",
    label: "HOME",
    isDefault: true,
    phoneTag: "Direct Mobile",
  },
  {
    id: "addr-002",
    recipientName: "Alex Morgan",
    phone: "(+84) 912 345 678",
    province: "Ho Chi Minh City",
    district: "District 7",
    ward: "Tan Phong Ward",
    streetAddress: "500 Nguyen Van Linh Parkway, Tech Innovation Lab",
    label: "OFFICE",
    isDefault: false,
    phoneTag: "Ext. 409 (Hardware Desk)",
  },
  {
    id: "addr-003",
    recipientName: "Eleanor Morgan",
    phone: "(+84) 987 654 321",
    province: "Da Nang City",
    district: "Hai Chau District",
    ward: "Thach Thang Ward",
    streetAddress: "1204 Bach Dang Riverside Road, Apartment 8B",
    label: "OTHER",
    isDefault: false,
    phoneTag: "Recipient Phone",
  },
];
