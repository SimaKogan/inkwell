export interface PageResult<T> {
  content: T[]
  page: { size: number; number: number; totalElements: number; totalPages: number }
}
export interface UserBrief { id: number; username: string; displayName: string }
export interface User extends UserBrief { bio?: string; role: 'USER' | 'ADMIN'; createdAt: string }
export interface AuthResponse { accessToken: string; refreshToken: string; user: User }
export interface Profile { user: User; followers: number; following: number; posts: number; followedByMe: boolean }

export interface Item {
  id: number; title: string; description?: string; category: string; isbn?: string
  price?: number; stock: number; publishedYear?: number; authors: string[]
  owner: UserBrief; createdAt: string
}
export interface ItemInput {
  title: string; description?: string; category: string; isbn?: string
  price?: number; stock?: number; publishedYear?: number; authors: string[]
}

export type PostSource = 'TYPED' | 'HANDWRITTEN'
export interface Post {
  id: number; title?: string; body: string; source: PostSource; createdAt: string
  author: UserBrief; item?: { id: number; title: string }
  likeCount: number; commentCount: number; likedByMe: boolean
}
export interface Comment {
  id: number; postId: number; parentId?: number; body: string; createdAt: string; author: UserBrief
}

export interface SearchHit { type: 'catalog' | 'post'; id: number; title: string; snippet?: string; score: number }
export interface SearchResponse {
  query: string; effectiveQuery: string; layoutCorrected: boolean; language: string
  semantic: boolean; hits: SearchHit[]
}
export interface AskResponse { answer?: string; effectiveQuery: string; sources: SearchHit[] }

export interface HwProfile { id: number; name: string; coveredChars: string; missingChars: string; createdAt: string }
export interface SheetReport { sheet: string; found: number; total: number; missing: string }
export interface ProfileCreated { profile: HwProfile; sheets: SheetReport[] }
export interface RenderResult { image: string; width: number; height: number; missing: string }
export interface RecognizeResult { text: string; language: string; engine: string }
