import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { api, qs } from '../api'
import { useAuth } from '../auth'
import ItemForm from '../components/ItemForm'
import ItemTile from '../components/ItemTile'
import { Empty, ErrorNote, Spinner } from '../components/ui'
import { useI18n } from '../i18n'
import type { Item, PageResult } from '../types'

const SORTS: Record<string, string> = {
  new: 'createdAt,desc', title: 'title,asc', priceAsc: 'price,asc', priceDesc: 'price,desc',
}

export default function Catalog() {
  const { t } = useI18n()
  const { user } = useAuth()
  const [params, setParams] = useSearchParams()
  const [creating, setCreating] = useState(false)

  const q = params.get('q') ?? ''
  const category = params.get('category') ?? ''
  const minPrice = params.get('min') ?? ''
  const maxPrice = params.get('max') ?? ''
  const sort = params.get('sort') ?? 'new'
  const page = Number(params.get('page') ?? 0)

  const update = (patch: Record<string, string>) => {
    const next = new URLSearchParams(params)
    Object.entries({ page: '0', ...patch }).forEach(([k, v]) => (v ? next.set(k, v) : next.delete(k)))
    setParams(next, { replace: true })
  }

  const categories = useQuery({ queryKey: ['categories'], queryFn: () => api.get<string[]>('/catalog/categories') })
  const list = useQuery({
    queryKey: ['catalog', { q, category, minPrice, maxPrice, sort, page }],
    queryFn: () => api.get<PageResult<Item>>(`/catalog${qs({ q, category, minPrice, maxPrice, sort: SORTS[sort], page, size: 12 })}`),
    placeholderData: (prev) => prev,
  })

  return (
    <div className="container">
      <div className="page-head">
        <h1>{t('catalog.title')}</h1>
        <div className="row">
          <a className="btn ghost" href="/api/catalog/report?format=html" target="_blank" rel="noreferrer">{t('catalog.report')} HTML</a>
          <a className="btn ghost" href="/api/catalog/report?format=csv">CSV</a>
          {user && <button className="btn primary" onClick={() => setCreating(true)}>{t('catalog.add')}</button>}
        </div>
      </div>

      <div className="filters">
        <input className="input" defaultValue={q} placeholder={t('catalog.filter.q')} dir="auto"
               onKeyDown={(e) => { if (e.key === 'Enter') update({ q: e.currentTarget.value }) }}
               onBlur={(e) => { if (e.currentTarget.value !== q) update({ q: e.currentTarget.value }) }} />
        <select className="select" value={category} onChange={(e) => update({ category: e.target.value })} aria-label={t('catalog.filter.category')}>
          <option value="">{t('catalog.filter.all')}</option>
          {categories.data?.map((c) => <option key={c} value={c}>{c}</option>)}
        </select>
        <div className="range">
          <input className="input" type="number" min="0" defaultValue={minPrice} placeholder={t('catalog.min')} aria-label={t('catalog.min')}
                 onBlur={(e) => update({ min: e.currentTarget.value })} />
          <input className="input" type="number" min="0" defaultValue={maxPrice} placeholder={t('catalog.max')} aria-label={t('catalog.max')}
                 onBlur={(e) => update({ max: e.currentTarget.value })} />
        </div>
        <select className="select" value={sort} onChange={(e) => update({ sort: e.target.value })} aria-label={t('catalog.sort')}>
          <option value="new">{t('sort.new')}</option><option value="title">{t('sort.title')}</option>
          <option value="priceAsc">{t('sort.priceAsc')}</option><option value="priceDesc">{t('sort.priceDesc')}</option>
        </select>
      </div>

      {list.isPending && <Spinner />}
      {list.isError && <ErrorNote error={list.error} />}
      {list.data && (list.data.content.length === 0
        ? <Empty>{t('catalog.empty')}</Empty>
        : <div className="tiles">{list.data.content.map((i) => <ItemTile key={i.id} item={i} />)}</div>)}

      {list.data && list.data.page.totalPages > 1 && (
        <nav className="pager" aria-label="Pages">
          <button className="btn ghost" disabled={page <= 0} onClick={() => update({ page: String(page - 1) })}>{t('common.prev')}</button>
          <span className="muted">{page + 1} / {list.data.page.totalPages}</span>
          <button className="btn ghost" disabled={page + 1 >= list.data.page.totalPages} onClick={() => update({ page: String(page + 1) })}>{t('common.next')}</button>
        </nav>
      )}
      {creating && <ItemForm onClose={() => setCreating(false)} />}
    </div>
  )
}
