'use client'

import Link from 'next/link'
import { useEffect, useState, FormEvent, ChangeEvent, KeyboardEvent } from 'react'
import { useRouter } from 'next/navigation'
import { ChevronLeft, ImagePlus, X } from 'lucide-react'
import { communityApi, postImageApi, CATEGORY_LABELS } from '@/lib/api'
import {
  POST_CATEGORY_SELECT_OPTIONS,
  type PostCategoryKey,
} from '@/constants'

const TITLE_MAX = 500
const MAX_IMAGES = 5
const MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024 // 5MB
const ALLOWED_IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/webp']
const MAX_TAGS = 10
const TAG_PATTERN = /^[가-힣A-Za-z0-9_]{1,50}$/

interface PendingImage {
  id: string // 로컬 식별자 (객체 URL)
  file: File
  previewUrl: string
}

function isLoggedIn(): boolean {
  if (typeof window === 'undefined') return false
  return !!localStorage.getItem('accessToken')
}

export default function CommunityWritePage() {
  const router = useRouter()
  const [authed, setAuthed] = useState<boolean | null>(null)

  const [category, setCategory] = useState<PostCategoryKey>('FREE')
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [anonymous, setAnonymous] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [images, setImages] = useState<PendingImage[]>([])
  const [imageError, setImageError] = useState<string | null>(null)

  const [tags, setTags] = useState<string[]>([])
  const [tagInput, setTagInput] = useState('')
  const [tagError, setTagError] = useState<string | null>(null)

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setAuthed(isLoggedIn())
  }, [])

  // 컴포넌트 언마운트 시 object URL 정리
  useEffect(() => {
    return () => {
      images.forEach((img) => URL.revokeObjectURL(img.previewUrl))
    }
  }, [images])

  const handleImagePick = (e: ChangeEvent<HTMLInputElement>) => {
    setImageError(null)
    const picked = Array.from(e.target.files ?? [])
    // input 은 같은 파일 재선택 시 onChange 가 안 터지므로 초기화
    e.target.value = ''
    if (picked.length === 0) return

    const remainSlots = MAX_IMAGES - images.length
    if (remainSlots <= 0) {
      setImageError(`이미지는 최대 ${MAX_IMAGES}장까지 첨부할 수 있어요`)
      return
    }

    const accepted: PendingImage[] = []
    for (const file of picked.slice(0, remainSlots)) {
      if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
        setImageError('PNG / JPEG / WEBP 만 업로드할 수 있어요')
        continue
      }
      if (file.size > MAX_IMAGE_SIZE_BYTES) {
        setImageError('파일당 5MB 이하만 업로드할 수 있어요')
        continue
      }
      accepted.push({
        id: `${file.name}-${file.size}-${Date.now()}-${Math.random()}`,
        file,
        previewUrl: URL.createObjectURL(file),
      })
    }
    if (accepted.length > 0) {
      setImages((prev) => [...prev, ...accepted])
    }
  }

  const removeImage = (id: string) => {
    setImages((prev) => {
      const target = prev.find((p) => p.id === id)
      if (target) URL.revokeObjectURL(target.previewUrl)
      return prev.filter((p) => p.id !== id)
    })
  }

  const commitTag = (raw: string) => {
    const trimmed = raw.trim().replace(/^#/, '')
    setTagInput('')
    setTagError(null)
    if (!trimmed) return
    if (!TAG_PATTERN.test(trimmed)) {
      setTagError('태그는 한글·영문·숫자·_ 조합, 1~50자여야 합니다.')
      return
    }
    if (tags.includes(trimmed)) return
    if (tags.length >= MAX_TAGS) {
      setTagError(`태그는 최대 ${MAX_TAGS}개까지 입력할 수 있어요`)
      return
    }
    setTags((prev) => [...prev, trimmed])
  }

  const handleTagKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === ' ' || e.key === 'Enter') {
      e.preventDefault()
      commitTag(tagInput)
    } else if (e.key === 'Backspace' && tagInput === '' && tags.length > 0) {
      setTags((prev) => prev.slice(0, -1))
    }
  }

  const removeTag = (tag: string) => {
    setTags((prev) => prev.filter((t) => t !== tag))
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    const trimmedTitle = title.trim()
    const trimmedContent = content.trim()
    if (!trimmedTitle || !trimmedContent) {
      setError('제목과 내용을 모두 입력해 주세요')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      // 1) 이미지 먼저 업로드 → URL 확보
      let imageUrls: string[] = []
      if (images.length > 0) {
        imageUrls = await postImageApi.upload(images.map((i) => i.file))
      }
      // 2) 게시글 생성
      const created = await communityApi.createPost({
        title: trimmedTitle,
        content: trimmedContent,
        category,
        isAnonymous: anonymous,
        imageUrls,
        tags: tags.length > 0 ? tags : undefined,
      })
      router.push(`/community/${created.id}`)
    } catch (err) {
      setError(err instanceof Error ? err.message : '등록에 실패했어요')
      setSubmitting(false)
    }
  }

  return (
    <div className="flex flex-col gap-5 px-5 py-6">
      {/* 헤더 */}
      <header className="flex items-center justify-between gap-2">
        <h1 className="text-2xl font-bold text-gray-900">글쓰기</h1>
        <Link
          href="/community"
          className="inline-flex h-10 items-center gap-1 rounded-full px-3 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-100"
        >
          <ChevronLeft className="h-5 w-5" aria-hidden="true" />
          취소
        </Link>
      </header>

      {authed === null ? (
        <div className="animate-pulse rounded-2xl border border-gray-200 bg-white p-6">
          <div className="h-5 w-1/2 rounded bg-gray-200" />
        </div>
      ) : !authed ? (
        <div className="flex flex-col items-center gap-3 rounded-2xl border border-gray-200 bg-white p-6 text-center">
          <p className="text-base font-semibold text-gray-900">
            로그인이 필요해요
          </p>
          <p className="text-sm text-gray-600">
            커뮤니티에 글을 쓰려면 먼저 로그인해 주세요
          </p>
          <Link
            href="/login"
            className="mt-2 inline-flex min-h-[48px] items-center justify-center rounded-full bg-blue-600 px-6 text-base font-semibold text-white hover:bg-blue-700"
          >
            로그인하러 가기
          </Link>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {/* 카테고리 */}
          <div className="flex flex-col gap-2">
            <label
              htmlFor="post-category"
              className="text-sm font-semibold text-gray-900"
            >
              카테고리
            </label>
            <select
              id="post-category"
              value={category}
              onChange={(e) => setCategory(e.target.value as PostCategoryKey)}
              className="h-12 w-full rounded-xl border border-gray-200 bg-white px-3 text-base text-gray-900 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
            >
              {POST_CATEGORY_SELECT_OPTIONS.map((option) => (
                <option key={option.key} value={option.key}>
                  {CATEGORY_LABELS[option.key] ?? option.label}
                </option>
              ))}
            </select>
          </div>

          {/* 제목 */}
          <div className="flex flex-col gap-2">
            <label
              htmlFor="post-title"
              className="text-sm font-semibold text-gray-900"
            >
              제목
            </label>
            <input
              id="post-title"
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value.slice(0, TITLE_MAX))}
              maxLength={TITLE_MAX}
              placeholder="제목을 입력하세요"
              className="h-12 w-full rounded-xl border border-gray-200 bg-white px-3 text-base placeholder:text-gray-500 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
            />
            <p className="text-right text-xs text-gray-600">
              {title.length} / {TITLE_MAX}
            </p>
          </div>

          {/* 내용 */}
          <div className="flex flex-col gap-2">
            <label
              htmlFor="post-content"
              className="text-sm font-semibold text-gray-900"
            >
              내용
            </label>
            <textarea
              id="post-content"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="자유롭게 이야기를 나눠보세요"
              className="min-h-[200px] w-full resize-y rounded-xl border border-gray-200 bg-white p-3 text-base placeholder:text-gray-500 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
            />
          </div>

          {/* 이미지 첨부 */}
          <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between">
              <label className="text-sm font-semibold text-gray-900">
                사진 첨부
              </label>
              <span className="text-xs text-gray-500">
                {images.length} / {MAX_IMAGES}
              </span>
            </div>

            <div className="flex flex-wrap gap-3">
              {images.map((img) => (
                <div
                  key={img.id}
                  className="relative h-20 w-20 overflow-hidden rounded-xl border border-gray-200 bg-gray-50"
                >
                  {/* 로컬 미리보기는 object URL */}
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img
                    src={img.previewUrl}
                    alt={`첨부 이미지 ${img.file.name}`}
                    className="h-full w-full object-cover"
                  />
                  <button
                    type="button"
                    onClick={() => removeImage(img.id)}
                    aria-label={`${img.file.name} 삭제`}
                    className="absolute right-1 top-1 inline-flex h-6 w-6 items-center justify-center rounded-full bg-black/60 text-white hover:bg-black/80"
                  >
                    <X className="h-3.5 w-3.5" aria-hidden="true" />
                  </button>
                </div>
              ))}

              {images.length < MAX_IMAGES && (
                <label
                  className="flex h-20 w-20 cursor-pointer flex-col items-center justify-center gap-1 rounded-xl border border-dashed border-gray-300 bg-white text-xs text-gray-500 hover:bg-gray-50"
                  aria-label="사진 추가"
                >
                  <ImagePlus className="h-5 w-5" aria-hidden="true" />
                  추가
                  <input
                    type="file"
                    accept="image/png,image/jpeg,image/webp"
                    multiple
                    onChange={handleImagePick}
                    className="sr-only"
                  />
                </label>
              )}
            </div>

            <p className="text-xs text-gray-600">
              PNG / JPEG / WEBP · 장당 5MB 이하 · 최대 {MAX_IMAGES}장
            </p>
            {imageError && (
              <p className="text-xs text-red-600">{imageError}</p>
            )}
          </div>

          {/* 태그 */}
          <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between">
              <label htmlFor="post-tag-input" className="text-sm font-semibold text-gray-900">
                태그
              </label>
              <span className="text-xs text-gray-500">{tags.length} / {MAX_TAGS}</span>
            </div>
            <div className="flex min-h-[48px] flex-wrap items-center gap-1.5 rounded-xl border border-gray-200 bg-white px-3 py-2 focus-within:border-blue-500 focus-within:ring-2 focus-within:ring-blue-200">
              {tags.map((tag) => (
                <span
                  key={tag}
                  className="inline-flex items-center gap-1 rounded-full bg-blue-50 px-2.5 py-0.5 text-xs font-semibold text-blue-700"
                >
                  #{tag}
                  <button
                    type="button"
                    onClick={() => removeTag(tag)}
                    aria-label={`${tag} 태그 제거`}
                    className="ml-0.5 inline-flex h-4 w-4 items-center justify-center rounded-full text-blue-500 hover:bg-blue-100"
                  >
                    <X className="h-3 w-3" aria-hidden="true" />
                  </button>
                </span>
              ))}
              {tags.length < MAX_TAGS && (
                <input
                  id="post-tag-input"
                  type="text"
                  value={tagInput}
                  onChange={(e) => setTagInput(e.target.value)}
                  onKeyDown={handleTagKeyDown}
                  onBlur={() => commitTag(tagInput)}
                  placeholder={tags.length === 0 ? '태그 입력 후 스페이스 또는 Enter' : ''}
                  className="min-w-[140px] flex-1 bg-transparent text-sm text-gray-900 placeholder:text-gray-500 focus:outline-none"
                />
              )}
            </div>
            <p className="text-xs text-gray-600">
              스페이스 또는 Enter 로 태그 추가 · 한글·영문·숫자·_ · 최대 {MAX_TAGS}개
            </p>
            {tagError && <p className="text-xs text-red-600">{tagError}</p>}
          </div>

          {/* 익명 */}
          <label className="inline-flex items-center gap-2 text-sm text-gray-800">
            <input
              type="checkbox"
              checked={anonymous}
              onChange={(e) => setAnonymous(e.target.checked)}
              className="h-4 w-4 rounded border-gray-300"
            />
            익명으로 작성하기
          </label>

          {error && (
            <div className="rounded-2xl border border-red-100 bg-red-50 p-3 text-sm text-red-700">
              {error}
            </div>
          )}

          {/* 등록 */}
          <button
            type="submit"
            disabled={submitting}
            className="mt-2 inline-flex min-h-[52px] items-center justify-center rounded-2xl bg-blue-600 px-5 text-base font-semibold text-white shadow-sm transition-colors hover:bg-blue-700 disabled:opacity-60"
          >
            {submitting ? '등록 중…' : '등록'}
          </button>
        </form>
      )}
    </div>
  )
}
