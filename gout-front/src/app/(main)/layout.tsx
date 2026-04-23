import BottomNav from '@/components/layout/BottomNav'

export default function MainLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <div className="relative mx-auto flex min-h-screen w-full max-w-md flex-col bg-white">
      {/* 하단 네비게이션 높이(64px)만큼 pb 확보 */}
      <main className="flex-1 pb-16">{children}</main>
      <BottomNav />
    </div>
  )
}
