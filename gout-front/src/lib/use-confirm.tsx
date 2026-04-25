"use client"

import * as React from "react"

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogActionDestructive,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"

type ConfirmOptions = {
  title: string
  description?: string
  confirmText?: string
  cancelText?: string
  destructive?: boolean
}

type PendingConfirm = ConfirmOptions & {
  resolve: (value: boolean) => void
}

/**
 * 브라우저 native `confirm()` 의 동기 동작을 흉내낸 훅.
 * 호출측에서 `await confirm({ title, ... })` 형태로 쓴다.
 *
 * 반환된 `<ConfirmDialog />` 를 페이지 어딘가에 렌더링해야
 * AlertDialog 마운트 포인트가 생성된다.
 */
export function useConfirm() {
  const [pending, setPending] = React.useState<PendingConfirm | null>(null)
  const [open, setOpen] = React.useState(false)

  const confirm = React.useCallback(
    (options: ConfirmOptions) =>
      new Promise<boolean>((resolve) => {
        setPending({ ...options, resolve })
        setOpen(true)
      }),
    [],
  )

  const handleClose = React.useCallback(
    (value: boolean) => {
      pending?.resolve(value)
      setOpen(false)
    },
    [pending],
  )

  const ConfirmDialog = React.useCallback(() => {
    if (!pending) return null
    const ActionButton = pending.destructive
      ? AlertDialogActionDestructive
      : AlertDialogAction
    return (
      <AlertDialog
        open={open}
        onOpenChange={(next) => {
          if (!next) handleClose(false)
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{pending.title}</AlertDialogTitle>
            {pending.description && (
              <AlertDialogDescription>
                {pending.description}
              </AlertDialogDescription>
            )}
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel onClick={() => handleClose(false)}>
              {pending.cancelText ?? "취소"}
            </AlertDialogCancel>
            <ActionButton onClick={() => handleClose(true)}>
              {pending.confirmText ?? "확인"}
            </ActionButton>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    )
  }, [open, pending, handleClose])

  return { confirm, ConfirmDialog }
}
