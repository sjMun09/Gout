import type { UserProfile } from '@/lib/api'

export const PROFILE_CACHE_KEY = 'goutcare:profile'

export function loadLocalProfile(): UserProfile | null {
  if (typeof window === 'undefined') return null
  try {
    const raw = localStorage.getItem(PROFILE_CACHE_KEY)
    if (!raw) return null
    return JSON.parse(raw) as UserProfile
  } catch {
    return null
  }
}

export function saveLocalProfile(profile: UserProfile) {
  if (typeof window === 'undefined') return
  try {
    localStorage.setItem(PROFILE_CACHE_KEY, JSON.stringify(profile))
  } catch {
    // ignore quota/serialization errors
  }
}
