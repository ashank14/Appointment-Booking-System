import type React from "react"
import type { Metadata } from "next"
import { Inter } from "next/font/google"
import "./globals.css"
import { Toaster } from "@/components/ui/toaster"
import Navigation from "@/components/navigation"

const inter = Inter({ subsets: ["latin"] })

export const metadata: Metadata = {
  title: "Smart Appointment Booking System",
  description: "Modern appointment booking system with role-based access",
    generator: 'v0.dev'
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en" className="dark">
      <body className={inter.className + " flex flex-col overflow-auto max-h-screen bg-background"}>
        <Navigation />
        <main className="flex-1 max-h-full">{children}</main>
        <Toaster />
      </body>
    </html>
  )
}
