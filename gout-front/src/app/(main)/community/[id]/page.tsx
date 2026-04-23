'use client'

import Link from 'next/link'
import { useEffect, useState, useCallback, use, FormEvent } from 'react'
import { useRouter } from 'next/navigation'
import { ChevronLeft, Eye, Heart, Send } from 'lucide-react'
import {
  communityApi,
  CATEGORY_LABELS,
  type PostDetail,
  type Comment,
} from '@/lib/api'

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

  const [commentText, setCommentText] = useState('')
  const [commentAnonymous, setCommentAnonymous] = useState(false)
  const [commentSubmitting, setCommentSubmitting] = useState(false)
  const [commentError, setCommentError] = useState<string | null>(null)

  const [authed, setAuthed] = useState(false)

  useEffect(() => {
    setAuthed(isLoggedIn())
  }, [])

  const loadPost = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await communityApi.getPost(id)
      setPost(data)
      setLiked(data.liked ?? false)
      setLikeCount(data.likeCount ?? 0)
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

  // 댓글 tree: 부모 없으면 루트, parentId 있으면 자식
  const rootComments = comments.filter((c) => !c.parentId)
  const childrenByParent = comments.reduce<Record<string, Comment[]>>(
    (acc, c) => {
      if (c.parentId) {
        if (!acc[c.parentId]) acc[c.parentId] = []
        acc[c.parentId].push(c)
      }
      return acc
    },
    {},
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
            </div>

            <p className="whitespace-pre-wrap text-base leading-7 text-gray-800">
              {post.content}
            </p>

            {/* 좋아요 버튼 */}
            <div className="pt-2">
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
                {rootComments.map((c) => (
                  <li key={c.id} className="flex flex-col gap-2">
                    <CommentItem comment={c} />
                    {(childrenByParent[c.id] ?? []).map((child) => (
                      <div key={child.id} className="pl-8">
                        <CommentItem comment={child} />
                      </div>
                    ))}
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
    </div>
  )
}

function CommentItem({ comment }: { comment: Comment }) {
  return (
    <div className="rounded-2xl border border-gray-200 bg-white p-4">
      <div className="mb-1 flex flex-wrap items-center gap-x-2 text-xs text-gray-500">
        <span className="font-semibold text-gray-800">{comment.nickname}</span>
        <span aria-hidden="true">·</span>
        <span>{formatDetailDate(comment.createdAt)}</span>
      </div>
      <p className="whitespace-pre-wrap text-sm leading-6 text-gray-800">
        {comment.content}
      </p>
    </div>
  )
}
