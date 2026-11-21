import { useInfiniteQuery } from '@tanstack/react-query'
import { api, qs } from '../api'
import { useI18n } from '../i18n'
import type { PageResult, Post } from '../types'
import PostCard from './PostCard'
import { Empty, ErrorNote, Spinner } from './ui'

interface Props { authorId?: number; itemId?: number; feed?: boolean; emptyText?: string }

export default function PostList({ authorId, itemId, feed, emptyText }: Props) {
  const { t } = useI18n()
  const q = useInfiniteQuery({
    queryKey: ['posts', { authorId, itemId, feed }],
    initialPageParam: 0,
    queryFn: ({ pageParam }) =>
      api.get<PageResult<Post>>(`${feed ? '/posts/feed' : '/posts'}${qs({ authorId, itemId, page: pageParam, size: 8 })}`),
    getNextPageParam: (last) => (last.page.number + 1 < last.page.totalPages ? last.page.number + 1 : undefined),
  })

  if (q.isPending) return <Spinner />
  if (q.isError) return <ErrorNote error={q.error} />
  const posts = q.data.pages.flatMap((p) => p.content)
  if (posts.length === 0) return <Empty>{emptyText ?? t('feed.empty')}</Empty>
  return (
    <div className="post-list">
      {posts.map((p) => <PostCard key={p.id} post={p} />)}
      {q.hasNextPage && (
        <button className="btn ghost wide" onClick={() => q.fetchNextPage()} disabled={q.isFetchingNextPage}>
          {q.isFetchingNextPage ? t('common.loading') : t('feed.more')}
        </button>
      )}
    </div>
  )
}
