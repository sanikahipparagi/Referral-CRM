import type { Metadata } from "next";
import type { ReactNode } from "react";
import { AuthProvider } from "@/components/auth-provider";
import "./globals.css";

export const metadata: Metadata = {
  title: { default: "Referral CRM", template: "%s · Referral CRM" },
  description: "A calm workspace for referral outreach and your job search.",
};

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return <html lang="en" suppressHydrationWarning><body><AuthProvider>{children}</AuthProvider></body></html>;
}
