type ClientErrorSource = 'global-error' | 'service-worker'

type ReportClientErrorInput = {
  source: ClientErrorSource
  error: unknown
  context?: Record<string, string | number | boolean | null | undefined>
}

const CLIENT_ERROR_ENDPOINT = process.env.NEXT_PUBLIC_CLIENT_ERROR_ENDPOINT ?? ''
const MAX_FIELD_LENGTH = 500

function truncate(value: string): string {
  return value.length > MAX_FIELD_LENGTH
    ? `${value.slice(0, MAX_FIELD_LENGTH)}...`
    : value
}

function normalizeError(error: unknown): { name: string; message: string; digest?: string } {
  if (error instanceof Error) {
    const digest = (error as Error & { digest?: unknown }).digest
    return {
      name: truncate(error.name || 'Error'),
      message: truncate(error.message || 'Unknown client error'),
      digest: typeof digest === 'string' ? truncate(digest) : undefined,
    }
  }

  return {
    name: 'NonError',
    message: truncate(String(error ?? 'Unknown client error')),
  }
}

function currentPath(): string | undefined {
  if (typeof window === 'undefined') return undefined
  return window.location.pathname
}

/**
 * Minimal client-side operational error hook.
 *
 * Privacy notes:
 * - no cookies, tokens, request bodies, query strings, or localStorage values are read
 * - endpoint is opt-in via NEXT_PUBLIC_CLIENT_ERROR_ENDPOINT
 * - failures while reporting are swallowed so error handling never creates another UI error
 */
export function reportClientError({ source, error, context }: ReportClientErrorInput): void {
  const payload = {
    source,
    ...normalizeError(error),
    path: currentPath(),
    context,
    occurredAt: new Date().toISOString(),
  }

  if (process.env.NODE_ENV !== 'production') {
    console.error('[ClientError]', payload)
  }

  if (!CLIENT_ERROR_ENDPOINT || typeof window === 'undefined') return

  try {
    const body = JSON.stringify(payload)
    if (navigator.sendBeacon) {
      const sent = navigator.sendBeacon(
        CLIENT_ERROR_ENDPOINT,
        new Blob([body], { type: 'application/json' }),
      )
      if (sent) return
    }

    void fetch(CLIENT_ERROR_ENDPOINT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body,
      keepalive: true,
      credentials: 'omit',
    }).catch(() => undefined)
  } catch {
    // Reporting must not interfere with the app error boundary or SW bootstrap path.
  }
}
