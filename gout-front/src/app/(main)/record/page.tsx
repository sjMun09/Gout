const tabs = [
  { key: 'uric', label: '요산수치' },
  { key: 'attack', label: '발작일지' },
  { key: 'medication', label: '복약' },
] as const

export default function RecordPage() {
  return (
    <div className="flex flex-col gap-5 px-5 py-6">
      <header>
        <h1 className="text-2xl font-bold text-gray-900">건강 기록</h1>
        <p className="mt-1 text-base text-gray-600">
          꾸준한 기록이 건강 관리의 시작입니다
        </p>
      </header>

      {/* 탭 */}
      <div
        className="flex rounded-2xl bg-gray-100 p-1"
        role="tablist"
        aria-label="기록 종류"
      >
        {tabs.map((tab, idx) => (
          <button
            key={tab.key}
            type="button"
            role="tab"
            aria-selected={idx === 0}
            className={`min-h-[48px] flex-1 rounded-xl text-base font-medium transition-colors ${
              idx === 0
                ? 'bg-white text-gray-900 shadow-sm'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* placeholder 콘텐츠 */}
      <section className="flex min-h-[240px] flex-col items-center justify-center gap-2 rounded-2xl border border-dashed border-gray-200 bg-white p-6 text-center">
        <p className="text-lg font-semibold text-gray-800">요산수치 기록</p>
        <p className="text-base text-gray-500">
          측정한 수치를 입력하면 추이 그래프로 보여드려요
        </p>
      </section>
    </div>
  )
}
