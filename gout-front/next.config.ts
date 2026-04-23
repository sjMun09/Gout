import type { NextConfig } from 'next'

/**
 * Next.js 설정.
 *
 * PWA 전략:
 * - Next 16은 Turbopack 기본. next-pwa(5.x)는 webpack 플러그인이라 빌드 실패.
 * - 현재는 public/manifest.json 기반 "설치 가능 PWA"까지만 지원.
 * - 서비스워커/오프라인 캐시가 필요해지면 @serwist/next 등 Turbopack 호환 패키지로 교체.
 */
const nextConfig: NextConfig = {
  reactStrictMode: true,
}

export default nextConfig
