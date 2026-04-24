'use client'

import Link from 'next/link'
import { Suspense, useEffect, useState, useCallback, useRef } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { Eye, Heart, MessageSquare, PencilLine, Search, X } from 'lucide-react'
import {
  communityApi,
  postImageApi,
  CATEGORY_LABELS,
  type PostSort,
  type PostSummary,
} from '@/lib/api'

const SORT_OPTIONS: { key: PostSort; label: string }[] = [
  { key: 'latest', label: '최신순' },
  { key: 'popular', label: '인기순' },
  { key: 'views', label: '조회순' },
]

function parseSort(raw: string | null): PostSort {
  if (raw === 'popular' || raw === 'views') return raw
  return 'latest'
}

interface CategoryTab {
  key: string // 'ALL' or backend category code
  label: string
}

const categoryTabs: CategoryTab[] = [
  { key: 'ALL', label: '전체' },
  { key: 'FREE', label: '자유' },
  { key: 'QUESTION', label: '질문' },
  { key: 'FOOD_EXPERIENCE', label: '식단' },
  { key: 'EXERCISE', label: '운동' },
  { key: 'SUCCESS_STORY', label: '성공담' },
]

function formatListDate(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return ''
  const now = new Date()
  const sameDay =
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate()
  if (sameDay) {
    const hh = String(date.getHours()).padStart(2, '0')
    const mm = String(date.getMinutes()).padStart(2, '0')
    return `${hh}:${mm}`
  }
  const diffDays = Math.floor(
    (now.getTime() - date.getTime()) / (1000 * 60 * 60 * 24),
  )
  if (diffDays >= 1 && diffDays <= 6) {
    return `${diffDays}일 전`
  }
  const MM = String(date.getMonth() + 1).padStart(2, '0')
  const DD = String(date.getDate()).padStart(2, '0')
  return `${MM}.${DD}`
}

export default function CommunityListPage() {
  // useSearchParams() 는 Next 15+ 에서 반드시 <Suspense> 경계가 필요함.
  return (
    <Suspense
      fallback={
        <div className="flex flex-col gap-5 px-5 py-6">
          <div className="h-12 w-40 animate-pulse rounded bg-gray-100" />
          <div className="h-12 w-full animate-pulse rounded-2xl bg-gray-100" />
        </div>
      }
    >
      <CommunityListContent />
    </Suspense>
  )
}

function CommunityListContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const urlKeyword = searchParams.get('keyword') ?? ''
  const urlSort = parseSort(searchParams.get('sort'))
  const urlTag = searchParams.get('tag') ?? ''

  const [activeCategory, setActiveCategory] = useState<string>('ALL')
  // 입력창용 로컬 state. 제출(Enter) 시에만 URL ?keyword 갱신.
  const [keywordInput, setKeywordInput] = useState<string>(urlKeyword)
  const [posts, setPosts] = useState<PostSummary[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  // IntersectionObserver sentinel. hasMore && !loading 일 때만 다음 페이지 fetch.
  const sentinelRef = useRef<HTMLDivElement | null>(null)

  // 외부에서 URL 이 바뀌면(예: 뒤로가기) 입력창에도 반영
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setKeywordInput(urlKeyword)
  }, [urlKeyword])

  const fetchPosts = useCallback(
    async (
      category: string,
      keyword: string,
      sort: PostSort,
      tag: string,
      nextPage: number,
      append: boolean,
    ) => {
      setLoading(true)
      setError(null)
      try {
        const data = await communityApi.getPosts({
          category: category === 'ALL' ? undefined : category,
          keyword: keyword || undefined,
          sort,
          tag: tag || undefined,
          page: nextPage,
          size: 20,
        })
        setTotalPages(data.totalPages ?? 0)
        setPage(data.number ?? nextPage)
        setPosts((prev) =>
          append ? [...prev, ...(data.content ?? [])] : data.content ?? [],
        )
      } catch (err) {
        setError(err instanceof Error ? err.message : '불러오기 실패')
        if (!append) setPosts([])
      } finally {
        setLoading(false)
      }
    },
    [],
  )

  useEffect(() => {
    fetchPosts(activeCategory, urlKeyword, urlSort, urlTag, 0, false)
  }, [activeCategory, urlKeyword, urlSort, urlTag, fetchPosts])

  const hasMore = page + 1 < totalPages

  // IntersectionObserver 로 sentinel 가시화 시 다음 페이지 로드.
  // deps 에 loading 을 포함하면 fetch 중 observer 재생성이 잦아지므로 ref 로 최신값 참조.
  const loadingRef = useRef(loading)
  const pageRef = useRef(page)
  useEffect(() => {
    loadingRef.current = loading
  }, [loading])
  useEffect(() => {
    pageRef.current = page
  }, [page])

  useEffect(() => {
    if (!hasMore) return
    const el = sentinelRef.current
    if (!el) return
    const observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0]
        if (entry?.isIntersecting && !loadingRef.current) {
          fetchPosts(
            activeCategory,
            urlKeyword,
            urlSort,
            urlTag,
            pageRef.current + 1,
            true,
          )
        }
      },
      { rootMargin: '200px' },
    )
    observer.observe(el)
    return () => observer.disconnect()
  }, [hasMore, activeCategory, urlKeyword, urlSort, urlTag, fetchPosts])

  const replaceCommunityQuery = useCallback(
    (mutate: (params: URLSearchParams) => void) => {
      const params = new URLSearchParams(searchParams.toString())
      mutate(params)
      const qs = params.toString()
      router.replace(qs ? `/community?${qs}` : '/community')
    },
    [router, searchParams],
  )

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const trimmed = keywordInput.trim()
    replaceCommunityQuery((params) => {
      if (trimmed) {
        params.set('keyword', trimmed)
      } else {
        params.delete('keyword')
      }
    })
  }

  const handleSortChange = (next: PostSort) => {
    replaceCommunityQuery((params) => {
      if (next === 'latest') {
        // 기본값은 URL 오염 방지 위해 생략.
        params.delete('sort')
      } else {
        params.set('sort', next)
      }
    })
  }

  const clearTagFilter = () => {
    replaceCommunityQuery((params) => params.delete('tag'))
  }

  const applyTagFilter = (tag: string) => {
    replaceCommunityQuery((params) => params.set('tag', tag))
  }

  const hasKeyword = urlKeyword.length > 0
  const hasTagFilter = urlTag.length > 0

  return (
    <div className="flex flex-col gap-5 px-5 py-6">
      {/* 헤더 */}
      <header className="flex items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">커뮤니티</h1>
          <p className="mt-1 text-base text-gray-600">
            같은 고민을 가진 분들과 이야기 나눠보세요
          </p>
        </div>
        <Link
          href="/community/write"
          className="inline-flex h-11 items-center gap-1.5 rounded-full bg-blue-600 px-4 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-blue-700"
          aria-label="글쓰기"
        >
          <PencilLine className="h-4 w-4" aria-hidden="true" />
          글쓰기
        </Link>
      </header>

      {/* 검색창 — 제출(Enter) 시 URL ?keyword 갱신 */}
      <form onSubmit={handleSubmit} role="search" aria-label="게시글 검색">
        <label htmlFor="community-keyword" className="sr-only">
          제목 또는 본문 키워드 검색
        </label>
        <div className="relative">
          <Search
            className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400"
            aria-hidden="true"
          />
          <input
            id="community-keyword"
            type="search"
            inputMode="search"
            value={keywordInput}
            onChange={(e) => setKeywordInput(e.target.value)}
            placeholder="제목 또는 본문 검색"
            className="h-12 w-full rounded-2xl border border-gray-200 bg-white pl-11 pr-4 text-base text-gray-900 placeholder:text-gray-400 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-100"
          />
        </div>
      </form>

      {/* 태그 필터 힌트 */}
      {hasTagFilter && (
        <div className="flex items-center gap-2 rounded-2xl border border-blue-100 bg-blue-50 px-4 py-2.5 text-sm text-blue-700">
          <span>태그: <strong>#{urlTag}</strong> 필터 중</span>
          <button
            type="button"
            onClick={clearTagFilter}
            aria-label="태그 필터 해제"
            className="ml-auto inline-flex h-6 w-6 items-center justify-center rounded-full text-blue-500 hover:bg-blue-100"
          >
            <X className="h-3.5 w-3.5" aria-hidden="true" />
          </button>
        </div>
      )}

      {/* 카테고리 탭 */}
      <section aria-labelledby="community-category-title">
        <h2 id="community-category-title" className="sr-only">
          카테고리
        </h2>
        <div
          className="flex gap-2 overflow-x-auto pb-1"
          role="tablist"
          aria-label="커뮤니티 카테고리"
        >
          {categoryTabs.map((cat) => {
            const selected = activeCategory === cat.key
            return (
              <button
                key={cat.key}
                type="button"
                role="tab"
                aria-selected={selected}
                onClick={() => setActiveCategory(cat.key)}
                className={`min-h-[44px] shrink-0 rounded-full border px-4 text-sm font-medium transition-colors ${
                  selected
                    ? 'border-blue-600 bg-blue-600 text-white'
                    : 'border-gray-200 bg-white text-gray-700 hover:bg-gray-50'
                }`}
              >
                {cat.label}
              </button>
            )
          })}
        </div>
      </section>

      {/* 정렬 옵션 */}
      <section aria-labelledby="community-sort-title" className="-mt-2">
        <h2 id="community-sort-title" className="sr-only">
          정렬
        </h2>
        <div
          role="tablist"
          aria-label="정렬 기준"
          className="flex gap-1 text-sm"
        >
          {SORT_OPTIONS.map((opt) => {
            const selected = urlSort === opt.key
            return (
              <button
                key={opt.key}
                type="button"
                role="tab"
                aria-selected={selected}
                onClick={() => handleSortChange(opt.key)}
                className={`min-h-[36px] rounded-full px-3 font-medium transition-colors ${
                  selected
                    ? 'bg-gray-900 text-white'
                    : 'bg-transparent text-gray-600 hover:bg-gray-100'
                }`}
              >
                {opt.label}
              </button>
            )
          })}
        </div>
      </section>

      {/* 게시글 목록 */}
      <section aria-labelledby="community-list-title">
        <h2 id="community-list-title" className="sr-only">
          게시글 목록
        </h2>

        {error && (
          <div className="rounded-2xl border border-red-100 bg-red-50 p-4 text-sm text-red-700">
            {error}
          </div>
        )}

        {loading && posts.length === 0 ? (
          <ul className="flex flex-col gap-2">
            {Array.from({ length: 5 }).map((_, idx) => (
              <li
                key={idx}
                className="animate-pulse rounded-2xl border border-gray-200 bg-white p-4"
              >
                <div className="mb-2 h-4 w-16 rounded bg-gray-200" />
                <div className="mb-2 h-5 w-3/4 rounded bg-gray-200" />
                <div className="h-3 w-1/2 rounded bg-gray-100" />
              </li>
            ))}
          </ul>
        ) : posts.length === 0 && !loading ? (
          <div className="rounded-2xl border border-dashed border-gray-200 bg-white p-6 text-center text-gray-500">
            {hasKeyword ? '검색 결과가 없습니다' : '아직 작성된 글이 없어요'}
          </div>
        ) : (
          <ul className="flex flex-col gap-2">
            {posts.map((post) => {
              const categoryLabel =
                CATEGORY_LABELS[post.category] ?? post.category
              return (
                <li key={post.id}>
                  <article className="flex flex-col gap-2 rounded-2xl border border-gray-200 bg-white p-4 transition-colors hover:bg-gray-50">
                    <Link
                      href={`/community/${post.id}`}
                      className="flex flex-col gap-2"
                    >
                      <span className="inline-flex w-fit items-center rounded-full bg-blue-50 px-2.5 py-0.5 text-xs font-semibold text-blue-700">
                        {categoryLabel}
                      </span>
                      <p className="text-base font-semibold text-gray-900">
                        {post.title}
                      </p>
                      {post.imageUrls && post.imageUrls.length > 0 && (
                        <div className="flex gap-1.5 overflow-x-auto">
                          {post.imageUrls.slice(0, 4).map((url, idx) => (
                            // eslint-disable-next-line @next/next/no-img-element
                            <img
                              key={`${post.id}-img-${idx}`}
                              src={postImageApi.absolute(url)}
                              alt=""
                              className="h-16 w-16 shrink-0 rounded-lg object-cover"
                              loading="lazy"
                            />
                          ))}
                        </div>
                      )}
                      <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-gray-500">
                        <span>{post.nickname}</span>
                        <span aria-hidden="true">·</span>
                        <span>{formatListDate(post.createdAt)}</span>
                      </div>
                      <div className="flex items-center gap-3 text-xs text-gray-500">
                        <span className="inline-flex items-center gap-1">
                          <Eye className="h-3.5 w-3.5" aria-hidden="true" />
                          {post.viewCount ?? 0}
                        </span>
                        <span className="inline-flex items-center gap-1">
                          <Heart className="h-3.5 w-3.5" aria-hidden="true" />
                          {post.likeCount ?? 0}
                        </span>
                        <span className="inline-flex items-center gap-1">
                          <MessageSquare
                            className="h-3.5 w-3.5"
                            aria-hidden="true"
                          />
                          {post.commentCount ?? 0}
                        </span>
                      </div>
                    </Link>
                    {post.tags && post.tags.length > 0 && (
                      <div className="flex flex-wrap gap-1">
                        {post.tags.map((tag) => (
                          <button
                            key={tag}
                            type="button"
                            onClick={() => applyTagFilter(tag)}
                            className="inline-flex items-center rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600 hover:bg-blue-100 hover:text-blue-700 transition-colors"
                          >
                            #{tag}
                          </button>
                        ))}
                      </div>
                    )}
                  </article>
                </li>
              )
            })}
          </ul>
        )}

        {/* 인피니트 스크롤 sentinel. 가시화되면 observer 가 다음 페이지 fetch.
            hasMore=false 일 땐 렌더하지 않아 observer 가 재연결되지 않는다. */}
        {hasMore && posts.length > 0 && (
          <div
            ref={sentinelRef}
            aria-hidden="true"
            className="mt-3 flex min-h-[48px] items-center justify-center text-sm text-gray-500"
          >
            {loading ? '불러오는 중…' : ''}
          </div>
        )}
      </section>
    </div>
  )
}
