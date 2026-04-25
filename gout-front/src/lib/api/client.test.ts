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
    expect(err.code).toBeNull()
    expect(err.fieldErrors).toBeNull()
    expect(err.retryAfter).toBeNull()
  })

  it('accepts code/fieldErrors/retryAfter via opts', () => {
    const err = new ApiError(422, '검증 실패', {
      code: 'VALIDATION_FAILED',
      fieldErrors: [{ field: 'nickname', code: 'NotBlank', message: '필수' }],
      retryAfter: 30,
    })
    expect(err.code).toBe('VALIDATION_FAILED')
    expect(err.fieldErrors).toEqual([
      { field: 'nickname', code: 'NotBlank', message: '필수' },
    ])
    expect(err.retryAfter).toBe(30)
  })
})

describe('apiFetch ErrorResponse parsing', () => {
  it('parses code and message from a 4xx ErrorResponse body', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(401, {
        code: 'AUTH_EXPIRED_TOKEN',
        message: '토큰이 만료되었습니다.',
      }),
    )

    const error = (await apiFetch('/api/me').catch((e: unknown) => e)) as ApiError

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(401)
    expect(error.code).toBe('AUTH_EXPIRED_TOKEN')
    expect(error.message).toBe('토큰이 만료되었습니다.')
  })

  it('parses fieldErrors on 422 validation responses', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(422, {
        code: 'VALIDATION_FAILED',
        message: '입력값이 올바르지 않습니다.',
        fieldErrors: [
          { field: 'nickname', code: 'Size', message: '2~20자' },
          { field: 'birthYear', code: 'Min', message: '1900 이상' },
        ],
      }),
    )

    const error = (await apiFetch('/api/profile').catch((e: unknown) => e)) as ApiError

    expect(error).toBeInstanceOf(ApiError)
    expect(error.fieldErrors).toEqual([
      { field: 'nickname', code: 'Size', message: '2~20자' },
      { field: 'birthYear', code: 'Min', message: '1900 이상' },
    ])
  })

  it('parses Retry-After header on 429', async () => {
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ code: 'RATE_LIMITED', message: '요청 과다' }), {
        status: 429,
        headers: { 'Content-Type': 'application/json', 'Retry-After': '30' },
      }),
    )

    const error = (await apiFetch('/api/posts').catch((e: unknown) => e)) as ApiError

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(429)
    expect(error.retryAfter).toBe(30)
  })

  it('returns null retryAfter when 429 has no Retry-After header', async () => {
    fetchMock.mockResolvedValue(jsonResponse(429, { code: 'RATE_LIMITED', message: '과다' }))

    const error = (await apiFetch('/api/posts').catch((e: unknown) => e)) as ApiError

    expect(error.retryAfter).toBeNull()
  })

  it('falls back to code=null for legacy bodies without code field', async () => {
    fetchMock.mockResolvedValue(jsonResponse(409, { message: '이미 존재' }))

    const error = (await apiFetch('/api/posts').catch((e: unknown) => e)) as ApiError

    expect(error.code).toBeNull()
    expect(error.fieldErrors).toBeNull()
  })
})
