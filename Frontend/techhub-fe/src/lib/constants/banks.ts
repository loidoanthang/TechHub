export interface BankOption {
  code: string;
  name: string;
}

export const VIETNAMESE_BANKS: BankOption[] = [
  { code: "VCB", name: "Vietcombank (Ngân hàng Ngoại thương Việt Nam)" },
  { code: "MB", name: "MB Bank (Ngân hàng Quân đội)" },
  { code: "TCB", name: "Techcombank (Ngân hàng Kỹ thương Việt Nam)" },
  { code: "ACB", name: "ACB (Ngân hàng Á Châu)" },
  { code: "VPB", name: "VPBank (Ngân hàng Việt Nam Thịnh Vượng)" },
  { code: "BIDV", name: "BIDV (Ngân hàng Đầu tư & Phát triển VN)" },
  { code: "CTG", name: "VietinBank (Ngân hàng Công thương Việt Nam)" },
  { code: "TPB", name: "TPBank (Ngân hàng Tiên Phong)" },
  { code: "STB", name: "Sacombank (Ngân hàng Sài Gòn Thương Tín)" },
];
