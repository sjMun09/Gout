import type { NextConfig } from 'next'

// next-pwa는 CommonJS — require로 로드하고 dev 모드에서는 비활성화
// (Turbopack/개발 서버와 서비스워커 충돌 방지)
// eslint-disable-next-line @typescript-eslint/no-require-imports
const withPWA = require('next-pwa')({
  dest: 'public',
  register: true,
  skipWaiting: true,
  disable: process.env.NODE_ENV === 'development',
})

const nextConfig: NextConfig = {
  reactStrictMode: true,
}

export default withPWA(nextConfig)

