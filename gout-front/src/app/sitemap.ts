import type { MetadataRoute } from 'next'

const STATIC_ROUTES = [
  '/home',
  '/food',
  '/hospital',
  '/record',
  '/community',
  '/encyclopedia',
  '/age-info',
  '/emergency',
  '/exercise',
  '/research',
  '/more',
  '/profile',
] as const

export default function sitemap(): MetadataRoute.Sitemap {
  const now = new Date()
  return STATIC_ROUTES.map((path) => ({
    url: path,
    lastModified: now,
    changeFrequency: 'weekly',
    priority: path === '/home' ? 1 : 0.6,
  }))
}
