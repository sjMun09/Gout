'use client'

import Link from 'next/link'
import { useEffect, useMemo, useState, useCallback, use, FormEvent } from 'react'
import { useRouter } from 'next/navigation'
import { Bookmark, ChevronLeft, Eye, Heart, Send } from 'lucide-react'
import {
  bookmarkApi,
  communityApi,
  getCurrentUserId,
  postImageApi,
  CATEGORY_LABELS,
  type PostDetail,
  type Comment,
} from '@/lib/api'
// ===== [AGENT-F: report] BEGIN =====
import ReportDialog from '@/components/report/ReportDialog'
import type { ReportTargetType } from '@/lib/api'
// ===== [AGENT-F: report] END =====

// 대댓글 들여쓰기 단계. depth 0(루트), 1, 2, 3 까지 시각적으로 들여쓰고
// 이후(최대 표시 깊이 MAX_DISPLAY_DEPTH 까지)는 깊이 3 위치에서 그대로 쌓아 스레드 연속성을 유지.
// MAX_DISPLAY_DEPTH 초과분은 같은 부모 아래로 평탄화(flatten) 해서 보여준다.
const INDENT_CAP = 3
const MAX_DISPLAY_DEPTH = 5
const INDENT_CLASSES: Record<number, string> = {
  0: '',
  1: 'ml-6',
  2: 'ml-12',
  3: 'ml-18',
}

function formatDetailDate(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return ''
  const yyyy = date.getFullYear()
  const MM = String(date.getMonth() + 1).padStart(2, '0')
  const DD = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${yyyy}.${MM}.${DD} ${hh}:${mm}`
}

function isLoggedIn(): boolean {
  if (typeof window === 'undefined') return false
  return !!localStorage.getItem('accessToken')
}

export default function CommunityDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = use(params)
  const router = useRouter()

  const [post, setPost] = useState<PostDetail | null>(null)
  const [comments, setComments] = useState<Comment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [liked, setLiked] = useState(false)
  const [likeCount, setLikeCount] = useState(0)
  const [likeBusy, setLikeBusy] = useState(false)

  const [bookmarked, setBookmarked] = useState(false)
  const [bookmarkBusy, setBookmarkBusy] = useState(false)

  const [commentText, setCommentText] = useState('')
  const [commentAnonymous, setCommentAnonymous] = useState(false)
  const [commentSubmitting, setCommentSubmitting] = useState(false)
  const [commentError, setCommentError] = useState<string | null>(null)

  const [authed, setAuthed] = useState(false)
  const [currentUserId, setCurrentUserId] = useState<string | null>(null)

  // ===== [AGENT-F: report] BEGIN =====
  const [reportTarget, setReportTarget] = useState<
    { type: ReportTargetType; id: string } | null
  >(null)
  const [reportToast, setReportToast] = useState<
    { kind: 'success' | 'error'; message: string } | null
  >(null)
  useEffect(() => {
    if (!reportToast) return
    const t = setTimeout(() => setReportToast(null), 2500)
    return () => clearTimeout(t)
  }, [reportToast])
  // ===== [AGENT-F: report] END =====

  useEffect(() => {
    setAuthed(isLoggedIn())
    setCurrentUserId(getCurrentUserId())
  }, [])

  const loadPost = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await communityApi.getPost(id)
      setPost(data)
      setLiked(data.liked ?? false)
      setLikeCount(data.likeCount ?? 0)
      setBookmarked(data.bookmarked ?? false)
      setComments(data.comments ?? [])
    } catch (err) {
      setError(err instanceof Error ? err.message : '게시글을 불러오지 못했어요')
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => {
    loadPost()
  }, [loadPost])

  const handleToggleBookmark = async () => {
    if (!authed) {
      router.push('/login')
      return
    }
    if (bookmarkBusy) return
    setBookmarkBusy(true)
    const prev = bookmarked
    // 낙관적 업데이트
    setBookmarked(!prev)
    try {
      const res = await bookmarkApi.toggle(id)
      setBookmarked(res.bookmarked)
    } catch {
      setBookmarked(prev)
    } finally {
      setBookmarkBusy(false)
    }
  }

  const handleToggleLike = async () => {
    if (!authed) {
      router.push('/login')
      return
    }
    if (likeBusy) return
    setLikeBusy(true)
    // 낙관적 업데이트
    const prevLiked = liked
    const prevCount = likeCount
    setLiked(!prevLiked)
    setLikeCount(prevLiked ? Math.max(0, prevCount - 1) : prevCount + 1)
    try {
      await communityApi.toggleLike(id)
    } catch {
      // 롤백
      setLiked(prevLiked)
      setLikeCount(prevCount)
    } finally {
      setLikeBusy(false)
    }
  }

  const handleSubmitComment = async (e: FormEvent) => {
    e.preventDefault()
    if (!authed) return
    const trimmed = commentText.trim()
    if (!trimmed) return
    setCommentSubmitting(true)
    setCommentError(null)
    try {
      const created = await communityApi.createComment(id, {
        content: trimmed,
        isAnonymous: commentAnonymous,
      })
      setComments((prev) => [...prev, created])
      setCommentText('')
    } catch (err) {
      setCommentError(err instanceof Error ? err.message : '댓글 등록 실패')
    } finally {
      setCommentSubmitting(false)
    }
  }

  const handleUpdateComment = useCallback(
    async (commentId: string, content: string) => {
      const updated = await communityApi.updateComment(commentId, content)
      setComments((prev) => prev.map((c) => (c.id === commentId ? updated : c)))
    },
    [],
  )

  // 댓글 트리를 평탄화해서 [{comment, depth}] 배열로 만든다.
  // MAX_DISPLAY_DEPTH 초과 깊이는 시각적으로 depth=MAX_DISPLAY_DEPTH 에 클램프(평탄화)한다.
  const flattenedComments = useMemo(
    () => flattenCommentTree(comments),
    [comments],
  )

  return (
    <div className="flex flex-col gap-5 px-5 py-6">
      {/* 뒤로가기 */}
      <div>
        <Link
          href="/community"
          className="inline-flex h-10 items-center gap-1 rounded-full px-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-100"
        >
          <ChevronLeft className="h-5 w-5" aria-hidden="true" />
          커뮤니티
        </Link>
      </div>

      {loading ? (
        <div className="animate-pulse rounded-2xl border border-gray-200 bg-white p-5">
          <div className="mb-3 h-4 w-16 rounded bg-gray-200" />
          <div className="mb-3 h-6 w-3/4 rounded bg-gray-200" />
          <div className="mb-2 h-3 w-1/2 rounded bg-gray-100" />
          <div className="mt-4 h-24 w-full rounded bg-gray-100" />
        </div>
      ) : error ? (
        <div className="rounded-2xl border border-red-100 bg-red-50 p-4 text-sm text-red-700">
          {error}
        </div>
      ) : post ? (
        <>
          {/* 게시글 본문 */}
          <article className="flex flex-col gap-3 rounded-2xl border border-gray-200 bg-white p-5">
            <span className="inline-flex w-fit items-center rounded-full bg-blue-50 px-2.5 py-0.5 text-xs font-semibold text-blue-700">
              {CATEGORY_LABELS[post.category] ?? post.category}
            </span>
            <h1 className="text-xl font-bold leading-7 text-gray-900">
              {post.title}
            </h1>
            <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-gray-500">
              <span>{post.nickname}</span>
              <span aria-hidden="true">·</span>
              <span>{formatDetailDate(post.createdAt)}</span>
              <span aria-hidden="true">·</span>
              <span className="inline-flex items-center gap-1">
                <Eye className="h-3.5 w-3.5" aria-hidden="true" />
                {post.viewCount ?? 0}
              </span>
              {/* ===== [AGENT-F: report] BEGIN ===== */}
              {authed && (
                <>
                  <span aria-hidden="true">·</span>
                  <button
                    type="button"
                    onClick={() => setReportTarget({ type: 'POST', id: post.id })}
                    className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs text-gray-500 hover:bg-gray-100 hover:text-red-600"
                    aria-label="게시글 신고"
                  >
                    <span aria-hidden="true">🚩</span>
                    신고
                  </button>
                </>
              )}
              {/* ===== [AGENT-F: report] END ===== */}
            </div>

            <p className="whitespace-pre-wrap text-base leading-7 text-gray-800">
              {post.content}
            </p>

            {/* ===== [Agent-C] 게시글 첨부 이미지 표시 ===== */}
            {post.imageUrls && post.imageUrls.length > 0 && (
              <div className="flex flex-col gap-2">
                {post.imageUrls.map((url, idx) => (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img
                    key={`post-image-${idx}`}
                    src={postImageApi.absolute(url)}
                    alt={`첨부 이미지 ${idx + 1}`}
                    className="max-h-[480px] w-full rounded-xl border border-gray-200 object-cover"
                    loading="lazy"
                  />
                ))}
              </div>
            )}
            {/* ===== [Agent-C] 여기까지 ===== */}

            {/* 해시태그 — 클릭 시 해당 태그 목록으로 이동 */}
            {post.tags && post.tags.length > 0 && (
              <div className="flex flex-wrap gap-1.5 pt-1">
                {post.tags.map((tag) => (
                  <Link
                    key={tag}
                    href={`/community?tag=${encodeURIComponent(tag)}`}
                    className="inline-flex items-center rounded-full bg-blue-50 px-2.5 py-0.5 text-xs font-semibold text-blue-700 hover:bg-blue-100"
                  >
                    #{tag}
                  </Link>
                ))}
              </div>
            )}

            {/* 좋아요 / 북마크 버튼 */}
            <div className="flex flex-wrap items-center gap-2 pt-2">
              <button
                type="button"
                onClick={handleToggleLike}
                disabled={likeBusy}
                aria-pressed={liked}
                className={`inline-flex min-h-[44px] items-center gap-2 rounded-full border px-5 text-sm font-semibold transition-colors disabled:opacity-60 ${
                  liked
                    ? 'border-red-500 bg-red-50 text-red-600'
                    : 'border-gray-200 bg-white text-gray-700 hover:bg-gray-50'
                }`}
              >
                <Heart
                  className={`h-4 w-4 ${liked ? 'fill-current' : ''}`}
                  aria-hidden="true"
                />
                좋아요 {likeCount}
              </button>

              <button
                type="button"
                onClick={handleToggleBookmark}
                disabled={bookmarkBusy}
                aria-pressed={bookmarked}
                aria-label={bookmarked ? '북마크 해제' : '북마크'}
                className={`inline-flex min-h-[44px] items-center gap-2 rounded-full border px-5 text-sm font-semibold transition-colors disabled:opacity-60 ${
                  bookmarked
                    ? 'border-amber-500 bg-amber-50 text-amber-600'
                    : 'border-gray-200 bg-white text-gray-700 hover:bg-gray-50'
                }`}
              >
                <Bookmark
                  className={`h-4 w-4 ${bookmarked ? 'fill-current' : ''}`}
                  aria-hidden="true"
                />
                {bookmarked ? '스크랩됨' : '스크랩'}
              </button>
            </div>
          </article>

          {/* 댓글 섹션 */}
          <section aria-labelledby="comment-title" className="flex flex-col gap-3">
            <h2
              id="comment-title"
              className="text-base font-semibold text-gray-900"
            >
              댓글 {comments.length}
            </h2>

            {comments.length === 0 ? (
              <div className="rounded-2xl border border-dashed border-gray-200 bg-white p-5 text-center text-sm text-gray-500">
                첫 댓글을 남겨보세요
              </div>
            ) : (
              <ul className="flex flex-col gap-2">
                {flattenedComments.map(({ comment, depth }) => (
                  <li key={comment.id}>
                    <CommentItem
                      comment={comment}
                      depth={depth}
                      currentUserId={currentUserId}
                      onUpdate={handleUpdateComment}
                      onReport={
                        authed
                          ? () => setReportTarget({ type: 'COMMENT', id: comment.id })
                          : undefined
                      }
                    />
                  </li>
                ))}
              </ul>
            )}

            {/* 댓글 작성 */}
            {authed ? (
              <form
                onSubmit={handleSubmitComment}
                className="flex flex-col gap-2 rounded-2xl border border-gray-200 bg-white p-4"
              >
                <label htmlFor="comment-input" className="sr-only">
                  댓글 입력
                </label>
                <textarea
                  id="comment-input"
                  value={commentText}
                  onChange={(e) => setCommentText(e.target.value)}
                  placeholder="댓글을 입력하세요"
                  className="min-h-[72px] w-full resize-y rounded-xl border border-gray-200 bg-white p-3 text-sm placeholder:text-gray-400 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
                />
                <div className="flex items-center justify-between gap-2">
                  <label className="inline-flex items-center gap-2 text-sm text-gray-700">
                    <input
                      type="checkbox"
                      checked={commentAnonymous}
                      onChange={(e) => setCommentAnonymous(e.target.checked)}
                      className="h-4 w-4 rounded border-gray-300"
                    />
                    익명
                  </label>
                  <button
                    type="submit"
                    disabled={commentSubmitting || !commentText.trim()}
                    className="inline-flex min-h-[44px] items-center gap-1.5 rounded-full bg-blue-600 px-4 text-sm font-semibold text-white transition-colors hover:bg-blue-700 disabled:opacity-60"
                  >
                    <Send className="h-4 w-4" aria-hidden="true" />
                    {commentSubmitting ? '등록 중…' : '등록'}
                  </button>
                </div>
                {commentError && (
                  <p className="text-xs text-red-600">{commentError}</p>
                )}
              </form>
            ) : (
              <div className="flex flex-col items-center gap-3 rounded-2xl border border-gray-200 bg-white p-5 text-center">
                <p className="text-sm text-gray-600">
                  로그인 후 댓글을 작성할 수 있어요
                </p>
                <Link
                  href="/login"
                  className="inline-flex min-h-[44px] items-center justify-center rounded-full bg-blue-600 px-5 text-sm font-semibold text-white hover:bg-blue-700"
                >
                  로그인하러 가기
                </Link>
              </div>
            )}
          </section>
        </>
      ) : null}

      {/* ===== [AGENT-F: report] BEGIN ===== */}
      {reportTarget && (
        <ReportDialog
          key={`${reportTarget.type}:${reportTarget.id}`}
          open
          targetType={reportTarget.type}
          targetId={reportTarget.id}
          onClose={() => setReportTarget(null)}
          onToast={(kind, message) => setReportToast({ kind, message })}
        />
      )}
      {reportToast && (
        <div
          role="status"
          aria-live="polite"
          className={`fixed bottom-6 left-1/2 z-40 -translate-x-1/2 rounded-full px-4 py-2 text-sm font-medium text-white shadow-lg ${
            reportToast.kind === 'success' ? 'bg-gray-900/90' : 'bg-red-600/95'
          }`}
        >
          {reportToast.message}
        </div>
      )}
      {/* ===== [AGENT-F: report] END ===== */}
    </div>
  )
}

/**
 * 댓글 트리를 DFS 순회하며 [{comment, depth}] 배열로 평탄화한다.
 * - parentId 가 없거나 매칭되는 루트가 없으면 루트(depth=0)로 취급
 * - MAX_DISPLAY_DEPTH 초과는 같은 MAX_DISPLAY_DEPTH 깊이로 flatten 하여 표시
 */
function flattenCommentTree(
  comments: Comment[],
): Array<{ comment: Comment; depth: number }> {
  if (!comments || comments.length === 0) return []

  const byId = new Map<string, Comment>()
  const childrenOf = new Map<string, Comment[]>()
  for (const c of comments) {
    byId.set(c.id, c)
  }
  const roots: Comment[] = []
  for (const c of comments) {
    const pid = c.parentId
    if (pid && byId.has(pid)) {
      const arr = childrenOf.get(pid) ?? []
      arr.push(c)
      childrenOf.set(pid, arr)
    } else {
      roots.push(c)
    }
  }

  // createdAt 오름차순 정렬 (동일 부모 내 대댓글 시간순)
  const sortByCreatedAt = (a: Comment, b: Comment) =>
    (a.createdAt ?? '').localeCompare(b.createdAt ?? '')
  roots.sort(sortByCreatedAt)
  for (const arr of childrenOf.values()) arr.sort(sortByCreatedAt)

  const result: Array<{ comment: Comment; depth: number }> = []
  const visit = (c: Comment, depth: number) => {
    const displayDepth = Math.min(depth, MAX_DISPLAY_DEPTH)
    result.push({ comment: c, depth: displayDepth })
    const children = childrenOf.get(c.id) ?? []
    for (const child of children) visit(child, depth + 1)
  }
  for (const r of roots) visit(r, 0)
  return result
}

interface CommentItemProps {
  comment: Comment
  depth: number
  currentUserId: string | null
  onUpdate: (commentId: string, content: string) => Promise<void>
  onReport?: () => void
}

function CommentItem({ comment, depth, currentUserId, onUpdate, onReport }: CommentItemProps) {
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState(comment.content)
  const [saving, setSaving] = useState(false)
  const [err, setErr] = useState<string | null>(null)

  const isMine =
    !!currentUserId && !!comment.userId && comment.userId === currentUserId

  // INDENT_CAP 초과 깊이는 INDENT_CAP 위치에 멈춰 시각적 무한 중첩을 방지.
  const indentDepth = Math.min(depth, INDENT_CAP)
  const indentClass = INDENT_CLASSES[indentDepth] ?? ''

  const startEdit = () => {
    setDraft(comment.content)
    setErr(null)
    setEditing(true)
  }

  const cancelEdit = () => {
    setEditing(false)
    setErr(null)
  }

  const saveEdit = async () => {
    const trimmed = draft.trim()
    if (!trimmed) {
      setErr('내용을 입력하세요')
      return
    }
    setSaving(true)
    setErr(null)
    try {
      await onUpdate(comment.id, trimmed)
      setEditing(false)
    } catch (e) {
      setErr(e instanceof Error ? e.message : '수정 실패')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className={indentClass}>
      <div className="rounded-2xl border border-gray-200 bg-white p-4">
        <div className="mb-1 flex flex-wrap items-center justify-between gap-x-2 text-xs text-gray-500">
          <div className="flex flex-wrap items-center gap-x-2">
            <span className="font-semibold text-gray-800">
              {comment.nickname}
            </span>
            <span aria-hidden="true">·</span>
            <span>{formatDetailDate(comment.createdAt)}</span>
            {comment.updatedAt &&
              comment.updatedAt !== comment.createdAt && (
                <span className="text-gray-400">(수정됨)</span>
              )}
          </div>
          <div className="flex items-center gap-1">
            {isMine && !editing && (
              <button
                type="button"
                onClick={startEdit}
                className="inline-flex items-center rounded-full border border-gray-200 bg-white px-2 py-0.5 text-xs font-medium text-gray-700 hover:bg-gray-50"
              >
                수정
              </button>
            )}
            {/* ===== [AGENT-F: report] BEGIN ===== */}
            {onReport && !isMine && !editing && (
              <button
                type="button"
                onClick={onReport}
                className="inline-flex items-center gap-1 rounded-full px-1.5 py-0.5 text-xs text-gray-500 hover:bg-gray-100 hover:text-red-600"
                aria-label="댓글 신고"
              >
                <span aria-hidden="true">🚩</span>
                신고
              </button>
            )}
            {/* ===== [AGENT-F: report] END ===== */}
          </div>
        </div>

        {editing ? (
          <div className="flex flex-col gap-2">
            <label htmlFor={`edit-${comment.id}`} className="sr-only">
              댓글 수정
            </label>
            <textarea
              id={`edit-${comment.id}`}
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              className="min-h-[72px] w-full resize-y rounded-xl border border-gray-200 bg-white p-3 text-sm placeholder:text-gray-400 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
            />
            <div className="flex items-center justify-end gap-2">
              <button
                type="button"
                onClick={cancelEdit}
                disabled={saving}
                className="inline-flex min-h-[36px] items-center rounded-full border border-gray-200 bg-white px-3 text-xs font-semibold text-gray-700 hover:bg-gray-50 disabled:opacity-60"
              >
                취소
              </button>
              <button
                type="button"
                onClick={saveEdit}
                disabled={saving || !draft.trim()}
                className="inline-flex min-h-[36px] items-center rounded-full bg-blue-600 px-3 text-xs font-semibold text-white hover:bg-blue-700 disabled:opacity-60"
              >
                {saving ? '저장 중…' : '저장'}
              </button>
            </div>
            {err && <p className="text-xs text-red-600">{err}</p>}
          </div>
        ) : (
          <p className="whitespace-pre-wrap text-sm leading-6 text-gray-800">
            {comment.content}
          </p>
        )}
      </div>
    </div>
  )
}
