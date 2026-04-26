'use client'

import { useCallback, useEffect, useState } from 'react'
import { parseError } from '../_components/shared'

export function useHealthLogs<T>(fetchLogs: () => Promise<T[]>) {
  const [logs, setLogs] = useState<T[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await fetchLogs()
      setLogs(data)
    } catch (e) {
      setError(parseError(e))
    } finally {
      setLoading(false)
    }
  }, [fetchLogs])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load()
  }, [load])

  return { logs, setLogs, loading, error, load }
}
