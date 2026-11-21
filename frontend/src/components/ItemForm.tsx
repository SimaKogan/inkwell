import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api'
import { useI18n } from '../i18n'
import type { Item, ItemInput } from '../types'
import Modal from './Modal'
import { ErrorNote } from './ui'

export default function ItemForm({ item, onClose, onSaved }: { item?: Item; onClose: () => void; onSaved?: (i: Item) => void }) {
  const { t } = useI18n()
  const qc = useQueryClient()
  const categories = useQuery({ queryKey: ['categories'], queryFn: () => api.get<string[]>('/catalog/categories') })
  const [f, setF] = useState({
    title: item?.title ?? '', description: item?.description ?? '', category: item?.category ?? '',
    authors: item?.authors.join(', ') ?? '', price: item?.price?.toString() ?? '', stock: item?.stock?.toString() ?? '0',
    year: item?.publishedYear?.toString() ?? '', isbn: item?.isbn ?? '',
  })
  const set = (k: keyof typeof f) => (e: { target: { value: string } }) => setF((s) => ({ ...s, [k]: e.target.value }))

  const save = useMutation({
    mutationFn: () => {
      const body: ItemInput = {
        title: f.title, description: f.description || undefined, category: f.category,
        authors: f.authors.split(',').map((a) => a.trim()).filter(Boolean),
        price: f.price ? Number(f.price) : undefined, stock: f.stock ? Number(f.stock) : 0,
        publishedYear: f.year ? Number(f.year) : undefined, isbn: f.isbn || undefined,
      }
      return item ? api.put<Item>(`/catalog/${item.id}`, body) : api.post<Item>('/catalog', body)
    },
    onSuccess: (saved) => {
      qc.invalidateQueries({ queryKey: ['catalog'] }); qc.invalidateQueries({ queryKey: ['item', saved.id] })
      qc.invalidateQueries({ queryKey: ['categories'] })
      onSaved?.(saved); onClose()
    },
  })
  const submit = (e: FormEvent) => { e.preventDefault(); save.mutate() }

  return (
    <Modal title={item ? t('item.edit') : t('catalog.add')} onClose={onClose}>
      <form onSubmit={submit} className="form-grid">
        <label className="field span2">{t('form.title')}
          <input className="input" value={f.title} onChange={set('title')} maxLength={200} required dir="auto" /></label>
        <label className="field">{t('form.category')}
          <input className="input" list="cats" value={f.category} onChange={set('category')} maxLength={80} required dir="auto" />
          <datalist id="cats">{categories.data?.map((c) => <option key={c} value={c} />)}</datalist></label>
        <label className="field">{t('form.authors')}
          <input className="input" value={f.authors} onChange={set('authors')} placeholder={t('form.authorsHint')} dir="auto" /></label>
        <label className="field span2">{t('form.description')}
          <textarea className="textarea" rows={4} value={f.description} onChange={set('description')} maxLength={5000} dir="auto" /></label>
        <label className="field">{t('form.price')}
          <input className="input" type="number" min="0" step="0.01" value={f.price} onChange={set('price')} /></label>
        <label className="field">{t('form.stock')}
          <input className="input" type="number" min="0" value={f.stock} onChange={set('stock')} /></label>
        <label className="field">{t('form.year')}
          <input className="input" type="number" min="0" max="2100" value={f.year} onChange={set('year')} /></label>
        <label className="field">{t('form.isbn')}
          <input className="input" value={f.isbn} onChange={set('isbn')} maxLength={20} /></label>
        {save.isError && <div className="span2"><ErrorNote error={save.error} /></div>}
        <div className="span2 row end">
          <button type="button" className="btn ghost" onClick={onClose}>{t('form.cancel')}</button>
          <button className="btn primary" disabled={save.isPending}>{t('form.save')}</button>
        </div>
      </form>
    </Modal>
  )
}
