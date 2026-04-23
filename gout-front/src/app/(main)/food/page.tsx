import { Search } from 'lucide-react'

const categories = [
  { key: 'all', label: '전체' },
  { key: 'high', label: '주의' },
  { key: 'medium', label: '괜찮음' },
  { key: 'low', label: '좋음' },
] as const

const signalItems = [
  {
    color: 'bg-red-500',
    label: '빨강',
    description: '퓨린 높음 — 피해주세요',
  },
  {
    color: 'bg-yellow-400',
    label: '노랑',
    description: '퓨린 중간 — 조금만',
  },
  {
    color: 'bg-green-500',
    label: '초록',
    description: '퓨린 낮음 — 괜찮아요',
  },
]

export default function FoodPage() {
  return (
    <div className="flex flex-col gap-5 px-5 py-6">
      <header>
        <h1 className="text-2xl font-bold text-gray-900">음식 확인</h1>
        <p className="mt-1 text-base text-gray-600">
          퓨린 수치를 신호등으로 쉽게 확인하세요
        </p>
      </header>

      {/* 검색창 */}
      <div className="relative">
        <label htmlFor="food-search" className="sr-only">
          음식 검색
        </label>
        <Search
          className="pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400"
          aria-hidden="true"
        />
        <input
          id="food-search"
          type="search"
          placeholder="음식 이름을 검색하세요"
          className="h-14 w-full rounded-2xl border border-gray-200 bg-white pl-12 pr-4 text-base placeholder:text-gray-400 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
        />
      </div>

      {/* 퓨린 신호등 설명 */}
      <section
        aria-labelledby="signal-title"
        className="rounded-2xl border border-gray-200 bg-white p-4"
      >
        <h2
          id="signal-title"
          className="mb-3 text-base font-semibold text-gray-900"
        >
          퓨린 신호등
        </h2>
        <ul className="flex flex-col gap-2">
          {signalItems.map((item) => (
            <li key={item.label} className="flex items-center gap-3">
              <span
                className={`inline-block h-5 w-5 rounded-full ${item.color}`}
                aria-hidden="true"
              />
              <span className="text-base text-gray-800">
                <strong className="font-semibold">{item.label}</strong>{' '}
                <span className="text-gray-600">— {item.description}</span>
              </span>
            </li>
          ))}
        </ul>
      </section>

      {/* 카테고리 탭 */}
      <section aria-labelledby="category-title">
        <h2 id="category-title" className="sr-only">
          카테고리
        </h2>
        <div
          className="flex gap-2 overflow-x-auto pb-1"
          role="tablist"
          aria-label="음식 카테고리"
        >
          {categories.map((cat, idx) => (
            <button
              key={cat.key}
              type="button"
              role="tab"
              aria-selected={idx === 0}
              className={`min-h-[48px] shrink-0 rounded-full border px-5 text-base font-medium transition-colors ${
                idx === 0
                  ? 'border-blue-600 bg-blue-600 text-white'
                  : 'border-gray-200 bg-white text-gray-700 hover:bg-gray-50'
              }`}
            >
              {cat.label}
            </button>
          ))}
        </div>
      </section>

      {/* 리스트 placeholder */}
      <div className="rounded-2xl border border-dashed border-gray-200 bg-white p-6 text-center text-gray-500">
        검색 결과가 여기 표시됩니다
      </div>
    </div>
  )
}
