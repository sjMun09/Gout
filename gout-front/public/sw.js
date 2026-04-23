/* Gout Care 서비스워커 — 최소 오프라인 캐시 전략
 *  - static(HTML/JS/CSS/이미지/폰트): cache-first + background update
 *  - /api/** : network-only (민감/최신 데이터)
 *  - manifest.webmanifest: stale-while-revalidate
 */
const CACHE_VERSION = 'v1'
const STATIC_CACHE = `goutcare-static-${CACHE_VERSION}`
const RUNTIME_CACHE = `goutcare-runtime-${CACHE_VERSION}`

const PRECACHE_URLS = [
  '/',
  '/home',
  '/icon-192.png',
  '/icon-512.png',
  '/icon-maskable.png',
]

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches
      .open(STATIC_CACHE)
      .then((cache) => cache.addAll(PRECACHE_URLS).catch(() => undefined))
      .then(() => self.skipWaiting()),
  )
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) =>
        Promise.all(
          keys
            .filter((k) => k !== STATIC_CACHE && k !== RUNTIME_CACHE)
            .map((k) => caches.delete(k)),
        ),
      )
      .then(() => self.clients.claim()),
  )
})

function isApiRequest(url) {
  return url.pathname.startsWith('/api/')
}

function isStaticAsset(url) {
  return (
    url.pathname.startsWith('/_next/static/') ||
    /\.(?:png|jpg|jpeg|svg|gif|webp|ico|woff2?|ttf|otf|css|js)$/i.test(
      url.pathname,
    )
  )
}

self.addEventListener('fetch', (event) => {
  const { request } = event
  if (request.method !== 'GET') return

  const url = new URL(request.url)
  // 동일 출처만 캐시 처리
  if (url.origin !== self.location.origin) return

  // API 는 항상 네트워크
  if (isApiRequest(url)) {
    return
  }

  if (isStaticAsset(url)) {
    event.respondWith(
      caches.match(request).then((cached) => {
        if (cached) {
          // background revalidate
          fetch(request)
            .then((res) => {
              if (res && res.ok) {
                caches
                  .open(RUNTIME_CACHE)
                  .then((cache) => cache.put(request, res.clone()))
              }
            })
            .catch(() => undefined)
          return cached
        }
        return fetch(request)
          .then((res) => {
            if (res && res.ok) {
              const clone = res.clone()
              caches
                .open(RUNTIME_CACHE)
                .then((cache) => cache.put(request, clone))
            }
            return res
          })
          .catch(() => cached)
      }),
    )
    return
  }

  // 네비게이션: network-first with cached home fallback
  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request).catch(() =>
        caches.match(request).then((r) => r || caches.match('/home')),
      ),
    )
  }
})
