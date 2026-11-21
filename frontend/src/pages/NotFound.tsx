import { Link } from 'react-router-dom'
import { useI18n } from '../i18n'

export default function NotFound() {
  const { t } = useI18n()
  return (
    <div className="container auth">
      <h1>{t('common.notFound')}</h1>
      <p><Link to="/">{t('common.home')}</Link></p>
    </div>
  )
}
