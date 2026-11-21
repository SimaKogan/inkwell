import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api'
import CommentThread from '../components/CommentThread'
import PostCard from '../components/PostCard'
import { ErrorNote, Spinner } from '../components/ui'
import type { Post } from '../types'

export default function PostPage() {
  const postId = Number(useParams().id)
  const q = useQuery({ queryKey: ['post', postId], queryFn: () => api.get<Post>(`/posts/${postId}`) })
  return (
    <div className="container narrow">
      {q.isPending && <Spinner />}
      {q.isError && <ErrorNote error={q.error} />}
      {q.data && <><PostCard post={q.data} full /><CommentThread postId={postId} /></>}
    </div>
  )
}
