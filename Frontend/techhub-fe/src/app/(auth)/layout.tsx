import { AuthCardWrapper } from "@/features/auth/components/AuthCardWrapper";

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <AuthCardWrapper>{children}</AuthCardWrapper>;
}
