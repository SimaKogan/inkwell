import { useRef, useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../api'
import { useI18n } from '../i18n'
import type { Post, PostSource, RecognizeResult } from '../types'
import { ErrorNote } from './ui'

export default function Composer({ itemId, onDone }: { itemId?: number; onDone?: (p: Post) => void }) {
  const { t } = useI18n()
  const qc = useQueryClient()
  const fileRef = useRef<HTMLInputElement>(null)
  const [title, setTitle] = useState('')
  const [body, setBody] = useState('')
  const [source, setSource] = useState<PostSource>('TYPED')

  const publish = useMutation({
    mutationFn: () => api.post<Post>('/posts', { title: title.trim() || undefined, body, catalogItemId: itemId, source }),
    onSuccess: (p) => {
      setTitle(''); setBody(''); setSource('TYPED')
      qc.invalidateQueries({ queryKey: ['posts'] })
      onDone?.(p)
    },
  })

  const recognize = useMutation({
    mutationFn: (file: File) => {
      const fd = new FormData()
      fd.append('image', file)
      return api.post<RecognizeResult>('/handwriting/recognize', fd)
    },
    onSuccess: (r) => { setBody((b) => (b ? b + '\n' : '') + r.text); setSource('HANDWRITTEN') },
  })

  const submit = (e: FormEvent) => { e.preventDefault(); if (body.trim()) publish.mutate() }

  return (
    <form className="composer" onSubmit={submit}>
      <input className="input" value={title} maxLength={200} onChange={(e) => setTitle(e.target.value)} placeholder={t('composer.title')} />
      <textarea className="textarea" rows={4} value={body} maxLength={10000} dir="auto"
                onChange={(e) => setBody(e.target.value)} placeholder={t('composer.body')} required />
      {source === 'HANDWRITTEN' && <p className="muted small">{t('composer.handwritten')}</p>}
      {(publish.isError || recognize.isError) && <ErrorNote error={publish.error ?? recognize.error} />}
      <div className="row between">
        <div>
          <input ref={fileRef} type="file" accept="image/*" hidden
                 onChange={(e) => { const f = e.target.files?.[0]; if (f) recognize.mutate(f); e.target.value = '' }} />
          <button type="button" className="btn ghost" onClick={() => fileRef.current?.click()} disabled={recognize.isPending}>
            {recognize.isPending ? t('composer.recognizing') : t('composer.fromPhoto')}
          </button>
        </div>
        <button className="btn primary" disabled={publish.isPending || !body.trim()}>{t('composer.publish')}</button>
      </div>
    </form>
  )
}
