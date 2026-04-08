import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { AppProvider } from "@/stores/AppContext";
import { Toaster } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";

const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "QiShiLu-RAG - 企业级 RAG 系统",
  description: "多租户隔离、长期记忆、智能问答",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-CN" className="h-full">
      <body className={`${inter.variable} h-full antialiased`}>
        <AppProvider>
          <TooltipProvider>
            {children}
          </TooltipProvider>
          <Toaster />
        </AppProvider>
      </body>
    </html>
  );
}
