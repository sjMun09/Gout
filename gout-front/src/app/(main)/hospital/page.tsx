import { MapPin, Phone } from 'lucide-react'

// placeholder 더미 데이터
const dummyHospitals = [
  { id: '1', name: '○○ 류마티스내과', distance: '320m', phone: '02-000-0000' },
  { id: '2', name: '△△ 통풍 전문 클리닉', distance: '850m', phone: '02-000-1111' },
  { id: '3', name: '□□ 내과의원', distance: '1.2km', phone: '02-000-2222' },
]

export default function HospitalPage() {
  return (
    <div className="flex flex-col gap-5 px-5 py-6">
      <header>
        <h1 className="text-2xl font-bold text-gray-900">병원 찾기</h1>
        <p className="mt-1 text-base text-gray-600">
          내 주변 통풍 전문 병원을 확인하세요
        </p>
      </header>

      {/* 지도 placeholder */}
      <div
        role="img"
        aria-label="지도 영역 (카카오맵 연동 예정)"
        className="flex h-56 items-center justify-center rounded-2xl border border-dashed border-gray-300 bg-gray-100"
      >
        <div className="flex flex-col items-center gap-2 text-gray-500">
          <MapPin className="h-8 w-8" aria-hidden="true" />
          <p className="text-base">지도가 여기에 표시됩니다</p>
        </div>
      </div>

      {/* 주변 병원 리스트 */}
      <section aria-labelledby="nearby-title">
        <h2
          id="nearby-title"
          className="mb-3 text-lg font-semibold text-gray-900"
        >
          내 주변 병원
        </h2>
        <ul className="flex flex-col gap-3">
          {dummyHospitals.map((h) => (
            <li
              key={h.id}
              className="flex items-start justify-between gap-3 rounded-2xl border border-gray-200 bg-white p-4"
            >
              <div className="flex-1">
                <p className="text-base font-semibold text-gray-900">
                  {h.name}
                </p>
                <p className="mt-0.5 text-sm text-gray-500">
                  {h.distance} · 도보 이용 가능
                </p>
              </div>
              <a
                href={`tel:${h.phone}`}
                aria-label={`${h.name}에 전화하기`}
                className="inline-flex h-12 w-12 items-center justify-center rounded-full bg-blue-50 text-blue-700 hover:bg-blue-100"
              >
                <Phone className="h-5 w-5" aria-hidden="true" />
              </a>
            </li>
          ))}
        </ul>
      </section>
    </div>
  )
}
