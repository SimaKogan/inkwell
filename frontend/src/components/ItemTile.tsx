import { Link } from 'react-router-dom'
import { useI18n } from '../i18n'
import type { Item } from '../types'
import { categoryHue, useFormat } from './ui'

/** Catalog entry styled like a notebook with a coloured binding strip per category. */
export default function ItemTile({ item }: { item: Item }) {
  const { t } = useI18n()
  const fmt = useFormat()
  return (
    <Link to={`/catalog/${item.id}`} className="tile" style={{ ['--tab' as string]: `hsl(${categoryHue(item.category)} 55% 48%)` }}>
      <span className="tile-cat">{item.category}</span>
      <h3 dir="auto">{item.title}</h3>
      {item.authors.length > 0 && <p className="tile-authors" dir="auto">{item.authors.join(', ')}</p>}
      {item.description && <p className="tile-desc" dir="auto">{item.description}</p>}
      <div className="tile-foot">
        <strong>{fmt.price(item.price)}</strong>
        <span className="muted small">{t('catalog.stock', { n: item.stock })}</span>
      </div>
    </Link>
  )
}
