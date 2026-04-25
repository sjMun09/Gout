import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { getCurrentUserId, getCurrentUserRoles, hasAdminRole } from './client'

function makeJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'none', typ: 'JWT' }))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
  const body = btoa(JSON.stringify(payload))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
  return `${header}.${body}.sig`
}

beforeEach(() => {
  localStorage.clear()
})

afterEach(() => {
  localStorage.clear()
})

describe('getCurrentUserId', () => {
  it('returns null when no token is stored', () => {
    expect(getCurrentUserId()).toBeNull()
  })

  it('extracts the sub claim from a valid JWT payload', () => {
    localStorage.setItem('accessToken', makeJwt({ sub: 'user-42', roles: ['USER'] }))
    expect(getCurrentUserId()).toBe('user-42')
  })

  it('returns null when the token is malformed', () => {
    localStorage.setItem('accessToken', 'not-a-jwt')
    expect(getCurrentUserId()).toBeNull()
  })

  it('returns null when the payload has no sub claim', () => {
    localStorage.setItem('accessToken', makeJwt({ roles: ['USER'] }))
    expect(getCurrentUserId()).toBeNull()
  })
})

describe('getCurrentUserRoles', () => {
  it('returns empty array when no token is stored', () => {
    expect(getCurrentUserRoles()).toEqual([])
  })

  it('returns the roles claim verbatim', () => {
    localStorage.setItem('accessToken', makeJwt({ sub: 'u', roles: ['USER', 'ADMIN'] }))
    expect(getCurrentUserRoles()).toEqual(['USER', 'ADMIN'])
  })

  it('returns empty array when roles claim is missing', () => {
    localStorage.setItem('accessToken', makeJwt({ sub: 'u' }))
    expect(getCurrentUserRoles()).toEqual([])
  })

  it('returns empty array when roles claim is not an array', () => {
    localStorage.setItem('accessToken', makeJwt({ sub: 'u', roles: 'ADMIN' }))
    expect(getCurrentUserRoles()).toEqual([])
  })
})

describe('hasAdminRole', () => {
  it('is true when ADMIN is in roles', () => {
    localStorage.setItem('accessToken', makeJwt({ sub: 'u', roles: ['USER', 'ADMIN'] }))
    expect(hasAdminRole()).toBe(true)
  })

  it('is false when ADMIN is absent', () => {
    localStorage.setItem('accessToken', makeJwt({ sub: 'u', roles: ['USER'] }))
    expect(hasAdminRole()).toBe(false)
  })

  it('is false when no token is stored', () => {
    expect(hasAdminRole()).toBe(false)
  })
})
