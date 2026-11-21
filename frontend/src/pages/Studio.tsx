import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api'
import { useAuth } from '../auth'
import { ErrorNote, Spinner } from '../components/ui'
import { useI18n } from '../i18n'
import type { HwProfile, Post, ProfileCreated, RecognizeResult, RenderResult } from '../types'

const SHEETS = [
  { key: 'ru', label: 'Русский' },
  { key: 'en', label: 'English' },
  { key: 'he', label: 'עברית' },
]

function SignInNote() {
  const { t } = useI18n()
  return <p className="notice info"><Link to="/login">{t('common.signin')}</Link>. {t('common.loginRequired')}</p>
}

function Recognize() {
  const { t } = useI18n()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const fileRef = useRef<HTMLInputElement>(null)
  const [file, setFile] = useState<File | null>(null)
  const [text, setText] = useState('')
  const [copied, setCopied] = useState(false)
  const preview = useMemo(() => (file ? URL.createObjectURL(file) : null), [file])
  useEffect(() => () => { if (preview) URL.revokeObjectURL(preview) }, [preview])

  const run = useMutation({
    mutationFn: () => {
      const fd = new FormData()
      fd.append('image', file as File)
      return api.post<RecognizeResult>('/handwriting/recognize', fd)
    },
    onSuccess: (r) => setText(r.text),
  })
  const publish = useMutation({
    mutationFn: () => api.post<Post>('/posts', { body: text, source: 'HANDWRITTEN' }),
    onSuccess: (p) => { qc.invalidateQueries({ queryKey: ['posts'] }); navigate(`/posts/${p.id}`) },
  })

  return (
    <div className="studio-grid">
      <div>
        <input ref={fileRef} type="file" accept="image/*" hidden onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
        <button type="button" className="drop" onClick={() => fileRef.current?.click()}>
          {preview ? <img src={preview} alt="" /> : <span>{t('studio.drop')}</span>}
        </button>
        <button className="btn primary wide" disabled={!file || run.isPending} onClick={() => run.mutate()}>
          {run.isPending ? t('studio.running') : t('studio.run')}
        </button>
        {run.isError && <ErrorNote error={run.error} />}
      </div>
      <div>
        <label className="field">{t('studio.result')}
          <textarea className="textarea tall" value={text} onChange={(e) => setText(e.target.value)} dir="auto" rows={10} /></label>
        {run.data && <p className="muted small">{t('studio.engine')}: {run.data.engine} · {t('studio.lang')}: {run.data.language}</p>}
        <div className="row">
          <button className="btn ghost" disabled={!text} onClick={async () => { await navigator.clipboard.writeText(text); setCopied(true); setTimeout(() => setCopied(false), 1500) }}>
            {copied ? t('studio.copied') : t('studio.copy')}
          </button>
          <button className="btn primary" disabled={!text.trim() || publish.isPending} onClick={() => publish.mutate()}>{t('studio.publish')}</button>
        </div>
        {publish.isError && <ErrorNote error={publish.error} />}
      </div>
    </div>
  )
}

function MyHandwriting() {
  const { t } = useI18n()
  const qc = useQueryClient()
  const profiles = useQuery({ queryKey: ['hwProfiles'], queryFn: () => api.get<HwProfile[]>('/handwriting/profiles') })
  const [selected, setSelected] = useState<number | null>(null)
  const [name, setName] = useState('')
  const [files, setFiles] = useState<File[]>([])
  const [consent, setConsent] = useState(false)
  const [text, setText] = useState('')
  const [paper, setPaper] = useState<'ruled' | 'plain' | 'transparent'>('ruled')
  const [ink, setInk] = useState('#1b2a6b')
  const [size, setSize] = useState(64)

  const active = profiles.data?.find((p) => p.id === selected) ?? profiles.data?.[0]

  const create = useMutation({
    mutationFn: () => {
      const fd = new FormData()
      fd.append('name', name); fd.append('consent', String(consent))
      files.forEach((f) => fd.append('sheets', f))
      return api.post<ProfileCreated>('/handwriting/profiles', fd)
    },
    onSuccess: (r) => { setName(''); setFiles([]); setConsent(false); setSelected(r.profile.id); qc.invalidateQueries({ queryKey: ['hwProfiles'] }) },
  })
  const remove = useMutation({
    mutationFn: (id: number) => api.del(`/handwriting/profiles/${id}`),
    onSuccess: () => { setSelected(null); qc.invalidateQueries({ queryKey: ['hwProfiles'] }) },
  })
  const render = useMutation({
    mutationFn: () => api.post<RenderResult>(`/handwriting/profiles/${active?.id}/render`, { text, paper, inkColor: ink, size }),
  })
  const submit = (e: FormEvent) => { e.preventDefault(); create.mutate() }

  return (
    <div className="steps">
      <section className="step">
        <h2>{t('studio.step1')}</h2>
        <p>{t('studio.step1.text')}</p>
        <div className="row wrap">
          {SHEETS.map((s) => (
            <a key={s.key} className="btn ghost" href={`/api/handwriting/template?sheets=${s.key}&format=pdf`} target="_blank" rel="noreferrer">
              {s.label} · {t('studio.download')}
            </a>
          ))}
        </div>
      </section>

      <section className="step">
        <h2>{t('studio.step2')}</h2>
        <p>{t('studio.step2.text')}</p>
        <form className="card-form" onSubmit={submit}>
          <label className="field">{t('studio.profileName')}
            <input className="input" value={name} maxLength={80} required onChange={(e) => setName(e.target.value)} /></label>
          <input className="input" type="file" accept="image/*" multiple required
                 onChange={(e) => setFiles(Array.from(e.target.files ?? []))} />
          <label className="check"><input type="checkbox" checked={consent} onChange={(e) => setConsent(e.target.checked)} required /><span>{t('studio.consent')}</span></label>
          {create.isError && <ErrorNote error={create.error} />}
          <button className="btn primary" disabled={create.isPending || files.length === 0 || !consent}>
            {create.isPending ? t('studio.creating') : t('studio.create')}
          </button>
          {create.data && (
            <ul className="report">
              {create.data.sheets.map((s, i) => <li key={i}>{s.sheet.toUpperCase()}: {s.found} / {s.total}</li>)}
            </ul>
          )}
        </form>
      </section>

      <section className="step">
        <h2>{t('studio.step3')}</h2>
        {profiles.isPending && <Spinner />}
        {profiles.data && profiles.data.length === 0 && <p className="muted">{t('studio.noProfiles')}</p>}
        {profiles.data && profiles.data.length > 0 && (
          <div className="studio-grid">
            <div>
              <h3>{t('studio.profiles')}</h3>
              <ul className="profiles">
                {profiles.data.map((p) => (
                  <li key={p.id} className={p.id === active?.id ? 'on' : ''}>
                    <button className="profile-pick" onClick={() => setSelected(p.id)}>
                      <strong>{p.name}</strong>
                      <span className="muted small">{[...p.coveredChars].length} {t('studio.covered')}</span>
                    </button>
                    {p.missingChars && <p className="muted small" dir="auto">{t('studio.missing')}: {p.missingChars}</p>}
                    <button className="link danger" onClick={() => { if (confirm(t('studio.confirmDelete'))) remove.mutate(p.id) }}>{t('studio.delete')}</button>
                  </li>
                ))}
              </ul>
            </div>
            <div>
              {active ? (
                <>
                  <label className="field">{t('studio.text')}
                    <textarea className="textarea" rows={5} value={text} maxLength={2000} dir="auto" onChange={(e) => setText(e.target.value)} /></label>
                  <div className="row wrap">
                    <label className="field inline">{t('studio.paper')}
                      <select className="select" value={paper} onChange={(e) => setPaper(e.target.value as typeof paper)}>
                        <option value="ruled">{t('paper.ruled')}</option><option value="plain">{t('paper.plain')}</option><option value="transparent">{t('paper.transparent')}</option>
                      </select></label>
                    <label className="field inline">{t('studio.ink')}
                      <input type="color" value={ink} onChange={(e) => setInk(e.target.value)} /></label>
                    <label className="field inline">{t('studio.size')}
                      <input type="range" min={40} max={100} value={size} onChange={(e) => setSize(Number(e.target.value))} /></label>
                  </div>
                  <button className="btn primary" disabled={!text.trim() || render.isPending} onClick={() => render.mutate()}>
                    {render.isPending ? t('studio.rendering') : t('studio.render')}
                  </button>
                  {render.isError && <ErrorNote error={render.error} />}
                  {render.data && (
                    <figure className="rendered">
                      <img src={render.data.image} alt={text.slice(0, 80)} width={render.data.width} height={render.data.height} />
                      <figcaption className="row between">
                        {render.data.missing
                          ? <span className="muted small" dir="auto">{t('studio.skipped', { chars: render.data.missing })}</span> : <span />}
                        <a className="btn ghost" href={render.data.image} download="inkwell-handwriting.png">{t('studio.save')}</a>
                      </figcaption>
                    </figure>
                  )}
                </>
              ) : <p className="muted">{t('studio.pickProfile')}</p>}
            </div>
          </div>
        )}
      </section>
    </div>
  )
}

export default function Studio() {
  const { t } = useI18n()
  const { user } = useAuth()
  const [tab, setTab] = useState<'recognize' | 'mine'>('recognize')
  return (
    <div className="container">
      <div className="page-head">
        <div><h1>{t('studio.title')}</h1><p className="muted">{t('studio.sub')}</p></div>
      </div>
      <div className="tabs" role="tablist">
        <button role="tab" aria-selected={tab === 'recognize'} className={`tab${tab === 'recognize' ? ' on' : ''}`} onClick={() => setTab('recognize')}>{t('studio.tab.recognize')}</button>
        <button role="tab" aria-selected={tab === 'mine'} className={`tab${tab === 'mine' ? ' on' : ''}`} onClick={() => setTab('mine')}>{t('studio.tab.mine')}</button>
      </div>
      {tab === 'mine' && !user ? (
        <>
          <MyHandwritingPublic />
          <SignInNote />
        </>
      ) : !user ? <SignInNote /> : tab === 'recognize' ? <Recognize /> : <MyHandwriting />}
    </div>
  )
}

/** Signed-out visitors can still print the template sheets. */
function MyHandwritingPublic() {
  const { t } = useI18n()
  return (
    <section className="step">
      <h2>{t('studio.step1')}</h2>
      <p>{t('studio.step1.text')}</p>
      <div className="row wrap">
        {SHEETS.map((s) => (
          <a key={s.key} className="btn ghost" href={`/api/handwriting/template?sheets=${s.key}&format=pdf`} target="_blank" rel="noreferrer">
            {s.label} · {t('studio.download')}
          </a>
        ))}
      </div>
    </section>
  )
}
