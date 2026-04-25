import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiFetch } from './client'

const fetchMock = vi.fn()

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
  localStorage.clear()
})

afterEach(() => {
  fetchMock.mockReset()
  vi.unstubAllGlobals()
})

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('apiFetch', () => {
  it('returns the data field on success', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { success: true, data: { id: 'p1' } }))

    const result = await apiFetch<{ id: string }>('/api/posts/p1')

    expect(result).toEqual({ id: 'p1' })
  })

  it('throws ApiError with the server message on 4xx', async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse(409, { message: '이미 신고한 대상입니다.' }),
    )

    const error = await apiFetch('/api/reports').catch((e) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      status: 409,
      message: '이미 신고한 대상입니다.',
    })
  })

  it('falls back to "API <status>: <statusText>" when the body is empty', async () => {
    fetchMock.mockResolvedValue(new Response('', { status: 503, statusText: 'Service Unavailable' }))

    await expect(apiFetch('/api/health')).rejects.toMatchObject({
      status: 503,
      message: 'API 503: Service Unavailable',
    })
  })

  it('throws ApiError when success:false even on 200', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { success: false, message: '검증 실패' }))

    await expect(apiFetch('/api/posts')).rejects.toMatchObject({
      status: 200,
      message: '검증 실패',
    })
  })

  it('attaches Authorization header when accessToken is present', async () => {
    localStorage.setItem('accessToken', 'tok-abc')
    fetchMock.mockResolvedValue(jsonResponse(200, { success: true, data: null }))

    await apiFetch('/api/me')

    const [, init] = fetchMock.mock.calls[0]
    expect(init.headers).toMatchObject({ Authorization: 'Bearer tok-abc' })
  })

  it('omits Authorization header when no token is present', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { success: true, data: null }))

    await apiFetch('/api/me')

    const [, init] = fetchMock.mock.calls[0]
    expect(init.headers).not.toHaveProperty('Authorization')
  })
})

describe('ApiError', () => {
  it('exposes status and inherits from Error', () => {
    const err = new ApiError(404, 'not found')
    expect(err).toBeInstanceOf(Error)
    expect(err.name).toBe('ApiError')
    expect(err.status).toBe(404)
    expect(err.message).toBe('not found')
  })
})
