// 날짜/시간 포맷 유틸 — 모든 함수는 사용자 로컬 타임존(브라우저 TZ) 기준.
// 서버/DB 는 UTC ISO 로 저장되며, 화면 표시는 로컬 변환.
// invalid Date 는 입력 문자열을 그대로 반환 (UI 깨짐 방지용 fallback).

const pad = (n: number) => String(n).padStart(2, '0')

/** 오늘 날짜 (로컬) 를 `YYYY-MM-DD` 로 반환. `<input type="date">` 의 value/max 용. */
export function todayYmd(): string {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** 현재 시각 (로컬) 을 `YYYY-MM-DDTHH:mm` 로 반환. `<input type="datetime-local">` 용. */
export function nowLocalDatetime(): string {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** ISO 또는 `YYYY-MM-DD` 입력을 `YYYY.MM.DD` (한국식) 로 포맷. invalid 면 입력 그대로. */
export function formatDateKr(isoLike: string): string {
  const d = new Date(isoLike)
  if (Number.isNaN(d.getTime())) return isoLike
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())}`
}

/** ISO 입력을 `YYYY.MM.DD HH:mm` (한국식, 로컬 TZ) 로 포맷. invalid 면 입력 그대로. */
export function formatDateTimeKr(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
