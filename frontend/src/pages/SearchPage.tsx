import { Link, useSearchParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { api, qs } from '../api'
import { useAuth } from '../auth'
import { Empty, ErrorNote, Spinner } from '../components/ui'
import { useI18n } from '../i18n'
import type { AskResponse, SearchHit, SearchResponse } from '../types'

function Hit({ hit }: { hit: SearchHit }) {
  const { t } = useI18n()
  const to = hit.type === 'catalog' ? `/catalog/${hit.id}` : `/posts/${hit.id}`
  return (
    <li className="hit">
      <span className={`badge ${hit.type}`}>{t(hit.type === 'catalog' ? 'search.type.catalog' : 'search.type.post')}</span>
      <div>
        <Link to={to} className="hit-title" dir="auto">{hit.title}</Link>
        {hit.snippet && <p className="hit-snippet" dir="auto">{hit.snippet}</p>}
      </div>
    </li>
  )
}

export default function SearchPage() {
  const { t } = useI18n()
  const { user } = useAuth()
  const [params] = useSearchParams()
  const query = params.get('q')?.trim() ?? ''
  const exact = params.get('exact') === '1'

  const results = useQuery({
    queryKey: ['search', query, exact],
    queryFn: () => api.get<SearchResponse>(`/search${qs({ q: query, limit: 20, exact: exact ? 'true' : undefined })}`),
    enabled: query.length > 0,
  })
  const ask = useMutation({
    mutationFn: () => api.post<AskResponse>('/search/ask', { query }),
  })

  return (
    <div className="container narrow">
      <h1 className="search-title" dir="auto">{t('search.title', { q: query })}</h1>

      {results.isPending && query && <Spinner />}
      {results.isError && <ErrorNote error={results.error} />}
      {results.data && (
        <>
          {results.data.layoutCorrected && (
            <p className="notice info">
              {t('search.corrected', { q: results.data.effectiveQuery })}{' '}
              <Link to={`/search?q=${encodeURIComponent(query)}&exact=1`}>{t('search.original', { q: query })}</Link>
            </p>
          )}
          <p className="muted small">{results.data.semantic ? t('search.semantic') : t('search.textOnly')}</p>

          {results.data.hits.length === 0 ? <Empty>{t('search.empty')}</Empty> : (
            <>
              <section className="ask">
                {user ? (
                  <button className="btn ghost" onClick={() => ask.mutate()} disabled={ask.isPending}>
                    {ask.isPending ? t('search.asking') : t('search.ask')}
                  </button>
                ) : <p className="muted"><Link to="/login">{t('common.signin')}</Link>. {t('search.askLogin')}</p>}
                {ask.isError && <ErrorNote error={ask.error} />}
                {ask.data && (
                  <div className="answer" dir="auto">
                    <p>{ask.data.answer ?? t('search.noAnswer')}</p>
                    {ask.data.sources.length > 0 && (
                      <p className="muted small">{t('search.sources')}: {ask.data.sources.map((s, i) => `[${i + 1}] ${s.title}`).join('  ')}</p>
                    )}
                  </div>
                )}
              </section>
              <ul className="hits">{results.data.hits.map((h) => <Hit key={`${h.type}-${h.id}`} hit={h} />)}</ul>
            </>
          )}
        </>
      )}
    </div>
  )
}
