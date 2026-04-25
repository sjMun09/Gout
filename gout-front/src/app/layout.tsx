import type { Metadata, Viewport } from 'next'
import { Geist, Geist_Mono } from 'next/font/google'
import './globals.css'
import '@/styles/accessibility.css'
import { QueryProvider } from '@/providers/QueryProvider'
import ServiceWorkerRegister from '@/components/common/ServiceWorkerRegister'
import { Toaster } from '@/components/ui/sonner'

const geistSans = Geist({
  variable: '--font-geist-sans',
  subsets: ['latin'],
})

const geistMono = Geist_Mono({
  variable: '--font-geist-mono',
  subsets: ['latin'],
})

export const metadata: Metadata = {
  title: {
    default: '통풍케어 - Gout Care',
    template: '%s | Gout Care',
  },
  description:
    '통풍 환자를 위한 응급처치·식단·요산 기록·병원 찾기·관리 팁 공유 커뮤니티 플랫폼',
  applicationName: '통풍케어',
  keywords: [
    '통풍',
    '요산',
    '퓨린',
    '식단',
    '통풍 응급처치',
    '통풍 관리',
    '통풍 병원',
    '통풍 커뮤니티',
  ],
  authors: [{ name: 'Gout Care Team' }],
  openGraph: {
    type: 'website',
    locale: 'ko_KR',
    siteName: '통풍케어',
    title: '통풍케어 - Gout Care',
    description:
      '통풍 환자를 위한 응급처치·식단·요산 기록·병원 찾기·관리 팁 공유 커뮤니티 플랫폼',
  },
  formatDetection: {
    telephone: false,
  },
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
        <Toaster />
        <ServiceWorkerRegister />
      </body>
    </html>
  )
}
