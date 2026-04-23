import type { Metadata, Viewport } from 'next'
import { Geist, Geist_Mono } from 'next/font/google'
import './globals.css'
import '@/styles/accessibility.css'
import { QueryProvider } from '@/providers/QueryProvider'

const geistSans = Geist({
  variable: '--font-geist-sans',
  subsets: ['latin'],
})

const geistMono = Geist_Mono({
  variable: '--font-geist-mono',
  subsets: ['latin'],
})

export const metadata: Metadata = {
  title: '통풍케어 - Gout Care',
  description: '통풍 환자를 위한 식단·수치·발작 관리 앱',
  applicationName: '통풍케어',
  manifest: '/manifest.json',
}

// 노인 사용자 접근성: 확대/축소 허용
export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  maximumScale: 5,
  userScalable: true,
  themeColor: '#2563eb',
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html
      lang="ko"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col bg-gray-50 text-base">
        <QueryProvider>{children}</QueryProvider>
      </body>
    </html>
  )
}
