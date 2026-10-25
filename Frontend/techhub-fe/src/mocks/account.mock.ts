export interface UserProfileDTO {
  id: string; // UUID v4
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  avatarUrl: string;
  roles: Array<"BUYER" | "SELLER" | "ADMIN">;
  status: "ACTIVE" | "SUSPENDED" | "BANNED" | "DELETED";
  emailVerified: boolean;
  tier: string;
  ordersCount: number;
}

export const mockUserProfile: UserProfileDTO = {
  id: "e4eaaaf2-a035-4702-86bb-b2d9a6c117d9",
  firstName: "Alex",
  lastName: "Morgan",
  email: "alex.morgan@example.com",
  phone: "(+84) 912 345 678",
  avatarUrl:
    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80",
  roles: ["BUYER"],
  status: "ACTIVE",
  emailVerified: true,
  tier: "Prime Tech",
  ordersCount: 3,
};
