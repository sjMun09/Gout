'use client'

import { useCallback, useEffect, useState } from 'react'
import { userApi } from '@/lib/api'
import { URIC_ACID_POLICY, isValidUricAcidTarget } from '@/lib/health-policy'
import { loadLocalProfile } from '@/lib/profile-cache'

function normalizeTarget(value: unknown): number | null {
  return typeof value === 'number' && isValidUricAcidTarget(value)
    ? value
    : null
}

export function useUricAcidTarget(accessToken?: string | null) {
  const [target, setTarget] = useState<number>(URIC_ACID_POLICY.defaultTarget)

  const loadTarget = useCallback(async () => {
    const localTarget = normalizeTarget(loadLocalProfile()?.targetUricAcid)
    if (localTarget !== null) setTarget(localTarget)

    if (!accessToken) return

    try {
      const me = await userApi.me()
      const serverTarget = normalizeTarget(me.targetUricAcid)
      if (serverTarget !== null) setTarget(serverTarget)
    } catch {
      // /api/users/me can be absent; local cache/default is the supported fallback.
    }
  }, [accessToken])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadTarget()
  }, [loadTarget])

  return target
}
