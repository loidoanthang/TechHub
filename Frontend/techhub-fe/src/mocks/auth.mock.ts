export interface AuthUser {
  id: string;
  name: string;
  email: string;
  avatarUrl?: string;
  role: 'BUYER' | 'SELLER' | 'ADMIN';
  token: string;
}

export const mockCurrentUser: AuthUser = {
  id: "usr_alex_01",
  name: "Alex Morgan",
  email: "alex.morgan@example.com",
  avatarUrl: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=160&q=80",
  role: "BUYER",
  token: "mock-jwt-token-techhub-2026",
};

export const mockSuspendedInfo = {
  accountEmail: "alex.m@techhub.com",
  detectionFlag: "Multi-Node Auth Mismatch",
  status: "Restricted",
  supportEmail: "support@techhub.market",
  referenceCode: "TH-8902",
};
