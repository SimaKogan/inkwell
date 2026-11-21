import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'

const ru = {
  'nav.catalog': 'Каталог', 'nav.feed': 'Обсуждения', 'nav.studio': 'Почерк', 'nav.login': 'Войти',
  'nav.register': 'Регистрация', 'nav.logout': 'Выйти', 'nav.search': 'Поиск по каталогу и постам',
  'home.title': 'Каталог, о котором говорят', 'home.sub': 'Ищите по смыслу, обсуждайте находки, превращайте рукописные заметки в текст, а текст — в свой почерк.',
  'home.hint': 'Набрали «ghbdtn» или «akuo» не в той раскладке? Найдём «привет» и «שלום».',
  'home.new': 'Новое в каталоге', 'home.discuss': 'Свежие обсуждения', 'home.all': 'Всё',
  'search.placeholder': 'Что ищем?', 'search.go': 'Найти',
  'catalog.title': 'Каталог', 'catalog.add': 'Добавить позицию', 'catalog.filter.q': 'Название или категория',
  'catalog.filter.category': 'Категория', 'catalog.filter.all': 'Все категории', 'catalog.min': 'Цена от', 'catalog.max': 'до',
  'catalog.sort': 'Сортировка', 'sort.new': 'Сначала новые', 'sort.title': 'По названию', 'sort.priceAsc': 'Дешевле', 'sort.priceDesc': 'Дороже',
  'catalog.empty': 'Ничего не нашлось. Попробуйте изменить фильтры.', 'catalog.report': 'Отчёт', 'catalog.stock': 'В наличии: {n}',
  'item.edit': 'Изменить', 'item.delete': 'Удалить', 'item.confirmDelete': 'Удалить эту позицию?',
  'item.discussion': 'Обсуждение', 'item.noPosts': 'Пока никто не написал об этом. Будьте первым.', 'item.authors': 'Авторы',
  'form.title': 'Название', 'form.description': 'Описание', 'form.category': 'Категория', 'form.authors': 'Авторы',
  'form.authorsHint': 'Через запятую', 'form.price': 'Цена', 'form.stock': 'Количество', 'form.year': 'Год', 'form.isbn': 'ISBN',
  'form.save': 'Сохранить', 'form.cancel': 'Отмена',
  'feed.title': 'Обсуждения', 'feed.all': 'Все', 'feed.following': 'Подписки', 'feed.empty': 'Здесь пока тихо.',
  'feed.followingEmpty': 'Подпишитесь на кого-нибудь, и их посты появятся здесь.', 'feed.more': 'Показать ещё',
  'composer.title': 'Заголовок (необязательно)', 'composer.body': 'Что у вас на уме?', 'composer.publish': 'Опубликовать',
  'composer.fromPhoto': 'Из фото рукописи', 'composer.recognizing': 'Читаю почерк…', 'composer.handwritten': 'Текст распознан с рукописи',
  'post.like': 'Нравится', 'post.comments': 'Комментарии', 'post.delete': 'Удалить', 'post.confirmDelete': 'Удалить пост?',
  'post.handwritten': 'Рукописный', 'post.about': 'О позиции',
  'comments.placeholder': 'Ваш комментарий', 'comments.send': 'Отправить', 'comments.reply': 'Ответить', 'comments.empty': 'Комментариев пока нет.',
  'comments.delete': 'Удалить', 'comments.loginToComment': 'Войдите, чтобы комментировать.',
  'profile.followers': 'подписчиков', 'profile.following': 'подписок', 'profile.posts': 'постов', 'profile.follow': 'Подписаться',
  'profile.unfollow': 'Отписаться', 'profile.edit': 'Изменить профиль', 'profile.displayName': 'Имя', 'profile.bio': 'О себе',
  'profile.save': 'Сохранить', 'profile.noPosts': 'Постов пока нет.',
  'auth.login': 'Вход', 'auth.register': 'Создать аккаунт', 'auth.identifier': 'Логин или email', 'auth.username': 'Логин',
  'auth.email': 'Email', 'auth.password': 'Пароль', 'auth.displayName': 'Как вас называть', 'auth.noAccount': 'Нет аккаунта?',
  'auth.haveAccount': 'Уже есть аккаунт?', 'auth.passwordHint': 'Не короче 8 символов',
  'search.title': 'Результаты по запросу «{q}»', 'search.corrected': 'Показаны результаты для «{q}»: раскладка исправлена.',
  'search.original': 'Искать как набрано: «{q}»', 'search.empty': 'Ничего не найдено.', 'search.semantic': 'Смысловой поиск включён',
  'search.textOnly': 'Только текстовый поиск: ML-сервис недоступен', 'search.ask': 'Спросить ИИ по найденному', 'search.asking': 'Думаю…',
  'search.sources': 'Источники', 'search.askLogin': 'Войдите, чтобы спросить ИИ.', 'search.noAnswer': 'Не нашлось источников для ответа.',
  'search.type.catalog': 'Каталог', 'search.type.post': 'Пост',
  'studio.title': 'Студия почерка', 'studio.sub': 'Прочитаем ваш рукописный текст и научимся писать вашим почерком.',
  'studio.tab.recognize': 'Распознать', 'studio.tab.mine': 'Мой почерк',
  'studio.drop': 'Выберите фото или скан рукописного текста', 'studio.run': 'Распознать', 'studio.running': 'Читаю…',
  'studio.result': 'Распознанный текст', 'studio.copy': 'Копировать', 'studio.copied': 'Скопировано', 'studio.publish': 'Опубликовать как пост',
  'studio.engine': 'Движок', 'studio.lang': 'Язык',
  'studio.step1': '1. Распечатайте лист', 'studio.step1.text': 'Выберите алфавит, распечатайте и напишите каждый символ в своей клетке тёмной ручкой на синей линии. Четыре чёрных квадрата по углам должны остаться видны.',
  'studio.step2': '2. Загрузите заполненные листы', 'studio.step2.text': 'Сфотографируйте лист целиком при ровном свете или отсканируйте. Можно загрузить несколько листов сразу.',
  'studio.step3': '3. Напишите текст своим почерком', 'studio.download': 'Скачать PDF', 'studio.profileName': 'Название профиля',
  'studio.consent': 'Я загружаю свой собственный почерк и согласен(на) на его хранение для генерации текста. Профиль можно удалить в любой момент.',
  'studio.create': 'Создать профиль', 'studio.creating': 'Обрабатываю листы…', 'studio.profiles': 'Ваши профили', 'studio.noProfiles': 'Профилей пока нет.',
  'studio.covered': 'символов', 'studio.missing': 'Не хватает', 'studio.delete': 'Удалить', 'studio.confirmDelete': 'Удалить профиль почерка?',
  'studio.text': 'Текст', 'studio.paper': 'Бумага', 'paper.ruled': 'В линейку', 'paper.plain': 'Чистая', 'paper.transparent': 'Прозрачная',
  'studio.ink': 'Чернила', 'studio.size': 'Размер', 'studio.render': 'Написать', 'studio.rendering': 'Пишу…', 'studio.save': 'Скачать PNG',
  'studio.skipped': 'Нет в вашем почерке: {chars}', 'studio.pickProfile': 'Выберите профиль слева',
  'common.loading': 'Загрузка…', 'common.error': 'Что-то пошло не так', 'common.signin': 'Войти', 'common.loginRequired': 'Для этого нужно войти в аккаунт.',
  'common.cancel': 'Отмена', 'common.prev': 'Назад', 'common.next': 'Дальше', 'common.notFound': 'Страница не найдена', 'common.home': 'На главную',
} as const

type Key = keyof typeof ru
type Dict = Record<Key, string>

const en: Dict = {
  'nav.catalog': 'Catalog', 'nav.feed': 'Discussions', 'nav.studio': 'Handwriting', 'nav.login': 'Sign in',
  'nav.register': 'Sign up', 'nav.logout': 'Sign out', 'nav.search': 'Search the catalog and posts',
  'home.title': 'A catalog people talk about', 'home.sub': 'Search by meaning, discuss your finds, turn handwritten notes into text, and text into your own handwriting.',
  'home.hint': 'Typed “ghbdtn” or “akuo” on the wrong layout? We will find “привет” and “שלום”.',
  'home.new': 'New in the catalog', 'home.discuss': 'Latest discussions', 'home.all': 'See all',
  'search.placeholder': 'What are you looking for?', 'search.go': 'Search',
  'catalog.title': 'Catalog', 'catalog.add': 'Add an item', 'catalog.filter.q': 'Title or category',
  'catalog.filter.category': 'Category', 'catalog.filter.all': 'All categories', 'catalog.min': 'Price from', 'catalog.max': 'to',
  'catalog.sort': 'Sort', 'sort.new': 'Newest first', 'sort.title': 'By title', 'sort.priceAsc': 'Cheapest', 'sort.priceDesc': 'Most expensive',
  'catalog.empty': 'Nothing found. Try changing the filters.', 'catalog.report': 'Report', 'catalog.stock': 'In stock: {n}',
  'item.edit': 'Edit', 'item.delete': 'Delete', 'item.confirmDelete': 'Delete this item?',
  'item.discussion': 'Discussion', 'item.noPosts': 'Nobody has written about this yet. Be the first.', 'item.authors': 'Authors',
  'form.title': 'Title', 'form.description': 'Description', 'form.category': 'Category', 'form.authors': 'Authors',
  'form.authorsHint': 'Comma separated', 'form.price': 'Price', 'form.stock': 'Stock', 'form.year': 'Year', 'form.isbn': 'ISBN',
  'form.save': 'Save', 'form.cancel': 'Cancel',
  'feed.title': 'Discussions', 'feed.all': 'Everyone', 'feed.following': 'Following', 'feed.empty': 'Quiet here for now.',
  'feed.followingEmpty': 'Follow someone and their posts will show up here.', 'feed.more': 'Show more',
  'composer.title': 'Title (optional)', 'composer.body': 'What is on your mind?', 'composer.publish': 'Publish',
  'composer.fromPhoto': 'From a handwriting photo', 'composer.recognizing': 'Reading the handwriting…', 'composer.handwritten': 'Text recognised from handwriting',
  'post.like': 'Like', 'post.comments': 'Comments', 'post.delete': 'Delete', 'post.confirmDelete': 'Delete this post?',
  'post.handwritten': 'Handwritten', 'post.about': 'About',
  'comments.placeholder': 'Your comment', 'comments.send': 'Send', 'comments.reply': 'Reply', 'comments.empty': 'No comments yet.',
  'comments.delete': 'Delete', 'comments.loginToComment': 'Sign in to comment.',
  'profile.followers': 'followers', 'profile.following': 'following', 'profile.posts': 'posts', 'profile.follow': 'Follow',
  'profile.unfollow': 'Unfollow', 'profile.edit': 'Edit profile', 'profile.displayName': 'Name', 'profile.bio': 'About you',
  'profile.save': 'Save', 'profile.noPosts': 'No posts yet.',
  'auth.login': 'Sign in', 'auth.register': 'Create an account', 'auth.identifier': 'Username or email', 'auth.username': 'Username',
  'auth.email': 'Email', 'auth.password': 'Password', 'auth.displayName': 'Display name', 'auth.noAccount': 'No account yet?',
  'auth.haveAccount': 'Already have an account?', 'auth.passwordHint': 'At least 8 characters',
  'search.title': 'Results for “{q}”', 'search.corrected': 'Showing results for “{q}”: keyboard layout corrected.',
  'search.original': 'Search as typed: “{q}”', 'search.empty': 'Nothing found.', 'search.semantic': 'Semantic search is on',
  'search.textOnly': 'Text search only: the ML service is unavailable', 'search.ask': 'Ask AI about these results', 'search.asking': 'Thinking…',
  'search.sources': 'Sources', 'search.askLogin': 'Sign in to ask the AI.', 'search.noAnswer': 'No sources to answer from.',
  'search.type.catalog': 'Catalog', 'search.type.post': 'Post',
  'studio.title': 'Handwriting studio', 'studio.sub': 'We read your handwritten text and learn to write in your hand.',
  'studio.tab.recognize': 'Recognise', 'studio.tab.mine': 'My handwriting',
  'studio.drop': 'Choose a photo or scan of handwritten text', 'studio.run': 'Recognise', 'studio.running': 'Reading…',
  'studio.result': 'Recognised text', 'studio.copy': 'Copy', 'studio.copied': 'Copied', 'studio.publish': 'Publish as a post',
  'studio.engine': 'Engine', 'studio.lang': 'Language',
  'studio.step1': '1. Print a sheet', 'studio.step1.text': 'Pick an alphabet, print it and write each character in its box with a dark pen, sitting on the blue line. Keep the four black corner squares visible.',
  'studio.step2': '2. Upload the filled sheets', 'studio.step2.text': 'Photograph the whole sheet in even light, or scan it. You can upload several sheets at once.',
  'studio.step3': '3. Write text in your handwriting', 'studio.download': 'Download PDF', 'studio.profileName': 'Profile name',
  'studio.consent': 'I am uploading my own handwriting and agree to it being stored to generate text. I can delete the profile at any time.',
  'studio.create': 'Create profile', 'studio.creating': 'Processing sheets…', 'studio.profiles': 'Your profiles', 'studio.noProfiles': 'No profiles yet.',
  'studio.covered': 'characters', 'studio.missing': 'Missing', 'studio.delete': 'Delete', 'studio.confirmDelete': 'Delete this handwriting profile?',
  'studio.text': 'Text', 'studio.paper': 'Paper', 'paper.ruled': 'Ruled', 'paper.plain': 'Plain', 'paper.transparent': 'Transparent',
  'studio.ink': 'Ink', 'studio.size': 'Size', 'studio.render': 'Write it', 'studio.rendering': 'Writing…', 'studio.save': 'Download PNG',
  'studio.skipped': 'Not in your handwriting: {chars}', 'studio.pickProfile': 'Pick a profile on the left',
  'common.loading': 'Loading…', 'common.error': 'Something went wrong', 'common.signin': 'Sign in', 'common.loginRequired': 'You need to sign in for this.',
  'common.cancel': 'Cancel', 'common.prev': 'Back', 'common.next': 'Next', 'common.notFound': 'Page not found', 'common.home': 'Back home',
}

const he: Dict = {
  'nav.catalog': 'קטלוג', 'nav.feed': 'דיונים', 'nav.studio': 'כתב יד', 'nav.login': 'התחברות',
  'nav.register': 'הרשמה', 'nav.logout': 'יציאה', 'nav.search': 'חיפוש בקטלוג ובפוסטים',
  'home.title': 'קטלוג שמדברים עליו', 'home.sub': 'חפשו לפי משמעות, דונו בממצאים, הפכו פתקים בכתב יד לטקסט, וטקסט לכתב היד שלכם.',
  'home.hint': 'הקלדתם «akuo» או «ghbdtn» בפריסת מקלדת שגויה? נמצא «שלום» ו«привет».',
  'home.new': 'חדש בקטלוג', 'home.discuss': 'דיונים אחרונים', 'home.all': 'הכול',
  'search.placeholder': 'מה מחפשים?', 'search.go': 'חיפוש',
  'catalog.title': 'קטלוג', 'catalog.add': 'הוספת פריט', 'catalog.filter.q': 'שם או קטגוריה',
  'catalog.filter.category': 'קטגוריה', 'catalog.filter.all': 'כל הקטגוריות', 'catalog.min': 'מחיר מ־', 'catalog.max': 'עד',
  'catalog.sort': 'מיון', 'sort.new': 'החדשים קודם', 'sort.title': 'לפי שם', 'sort.priceAsc': 'הזול ביותר', 'sort.priceDesc': 'היקר ביותר',
  'catalog.empty': 'לא נמצא דבר. נסו לשנות את המסננים.', 'catalog.report': 'דוח', 'catalog.stock': 'במלאי: {n}',
  'item.edit': 'עריכה', 'item.delete': 'מחיקה', 'item.confirmDelete': 'למחוק את הפריט?',
  'item.discussion': 'דיון', 'item.noPosts': 'עדיין אף אחד לא כתב על זה. היו הראשונים.', 'item.authors': 'מחברים',
  'form.title': 'שם', 'form.description': 'תיאור', 'form.category': 'קטגוריה', 'form.authors': 'מחברים',
  'form.authorsHint': 'מופרדים בפסיק', 'form.price': 'מחיר', 'form.stock': 'כמות', 'form.year': 'שנה', 'form.isbn': 'ISBN',
  'form.save': 'שמירה', 'form.cancel': 'ביטול',
  'feed.title': 'דיונים', 'feed.all': 'כולם', 'feed.following': 'במעקב', 'feed.empty': 'שקט כאן בינתיים.',
  'feed.followingEmpty': 'עקבו אחרי מישהו והפוסטים שלו יופיעו כאן.', 'feed.more': 'עוד',
  'composer.title': 'כותרת (לא חובה)', 'composer.body': 'מה על הלב?', 'composer.publish': 'פרסום',
  'composer.fromPhoto': 'מתמונת כתב יד', 'composer.recognizing': 'קורא את כתב היד…', 'composer.handwritten': 'הטקסט זוהה מכתב יד',
  'post.like': 'אהבתי', 'post.comments': 'תגובות', 'post.delete': 'מחיקה', 'post.confirmDelete': 'למחוק את הפוסט?',
  'post.handwritten': 'כתב יד', 'post.about': 'על',
  'comments.placeholder': 'התגובה שלכם', 'comments.send': 'שליחה', 'comments.reply': 'תגובה', 'comments.empty': 'אין תגובות עדיין.',
  'comments.delete': 'מחיקה', 'comments.loginToComment': 'התחברו כדי להגיב.',
  'profile.followers': 'עוקבים', 'profile.following': 'במעקב', 'profile.posts': 'פוסטים', 'profile.follow': 'מעקב',
  'profile.unfollow': 'הפסקת מעקב', 'profile.edit': 'עריכת פרופיל', 'profile.displayName': 'שם', 'profile.bio': 'על עצמכם',
  'profile.save': 'שמירה', 'profile.noPosts': 'אין פוסטים עדיין.',
  'auth.login': 'התחברות', 'auth.register': 'יצירת חשבון', 'auth.identifier': 'שם משתמש או אימייל', 'auth.username': 'שם משתמש',
  'auth.email': 'אימייל', 'auth.password': 'סיסמה', 'auth.displayName': 'שם תצוגה', 'auth.noAccount': 'אין לכם חשבון?',
  'auth.haveAccount': 'כבר יש חשבון?', 'auth.passwordHint': 'לפחות 8 תווים',
  'search.title': 'תוצאות עבור «{q}»', 'search.corrected': 'מוצגות תוצאות עבור «{q}»: פריסת המקלדת תוקנה.',
  'search.original': 'חיפוש כפי שהוקלד: «{q}»', 'search.empty': 'לא נמצא דבר.', 'search.semantic': 'חיפוש סמנטי פעיל',
  'search.textOnly': 'חיפוש טקסט בלבד: שירות ה־ML אינו זמין', 'search.ask': 'שאלו את ה־AI על התוצאות', 'search.asking': 'חושב…',
  'search.sources': 'מקורות', 'search.askLogin': 'התחברו כדי לשאול את ה־AI.', 'search.noAnswer': 'אין מקורות למענה.',
  'search.type.catalog': 'קטלוג', 'search.type.post': 'פוסט',
  'studio.title': 'סטודיו כתב יד', 'studio.sub': 'נקרא את הטקסט בכתב ידכם ונלמד לכתוב בכתב ידכם.',
  'studio.tab.recognize': 'זיהוי', 'studio.tab.mine': 'כתב היד שלי',
  'studio.drop': 'בחרו תמונה או סריקה של טקסט בכתב יד', 'studio.run': 'זיהוי', 'studio.running': 'קורא…',
  'studio.result': 'הטקסט שזוהה', 'studio.copy': 'העתקה', 'studio.copied': 'הועתק', 'studio.publish': 'פרסום כפוסט',
  'studio.engine': 'מנוע', 'studio.lang': 'שפה',
  'studio.step1': '1. הדפסת דף', 'studio.step1.text': 'בחרו אלפבית, הדפיסו וכתבו כל תו בתוך התא שלו בעט כהה, על הקו הכחול. ארבעת הריבועים השחורים בפינות חייבים להישאר גלויים.',
  'studio.step2': '2. העלאת הדפים המלאים', 'studio.step2.text': 'צלמו את הדף כולו באור אחיד או סרקו. אפשר להעלות כמה דפים יחד.',
  'studio.step3': '3. כתיבת טקסט בכתב ידכם', 'studio.download': 'הורדת PDF', 'studio.profileName': 'שם הפרופיל',
  'studio.consent': 'אני מעלה את כתב היד שלי ומסכים/ה לשמירתו לצורך יצירת טקסט. אפשר למחוק את הפרופיל בכל עת.',
  'studio.create': 'יצירת פרופיל', 'studio.creating': 'מעבד דפים…', 'studio.profiles': 'הפרופילים שלכם', 'studio.noProfiles': 'אין פרופילים עדיין.',
  'studio.covered': 'תווים', 'studio.missing': 'חסרים', 'studio.delete': 'מחיקה', 'studio.confirmDelete': 'למחוק את פרופיל כתב היד?',
  'studio.text': 'טקסט', 'studio.paper': 'נייר', 'paper.ruled': 'שורות', 'paper.plain': 'חלק', 'paper.transparent': 'שקוף',
  'studio.ink': 'דיו', 'studio.size': 'גודל', 'studio.render': 'כתיבה', 'studio.rendering': 'כותב…', 'studio.save': 'הורדת PNG',
  'studio.skipped': 'לא בכתב ידכם: {chars}', 'studio.pickProfile': 'בחרו פרופיל',
  'common.loading': 'טוען…', 'common.error': 'משהו השתבש', 'common.signin': 'התחברות', 'common.loginRequired': 'צריך להתחבר כדי לעשות זאת.',
  'common.cancel': 'ביטול', 'common.prev': 'הקודם', 'common.next': 'הבא', 'common.notFound': 'העמוד לא נמצא', 'common.home': 'לדף הבית',
}

export type Lang = 'ru' | 'en' | 'he'
const dicts: Record<Lang, Dict> = { ru, en, he }
const LOCALES: Record<Lang, string> = { ru: 'ru-RU', en: 'en-GB', he: 'he-IL' }

interface I18n {
  lang: Lang
  locale: string
  setLang: (l: Lang) => void
  t: (key: Key, vars?: Record<string, string | number>) => string
}

const Ctx = createContext<I18n | null>(null)

function initialLang(): Lang {
  const saved = localStorage.getItem('inkwell.lang')
  if (saved === 'ru' || saved === 'en' || saved === 'he') return saved
  const nav = navigator.language.slice(0, 2)
  return nav === 'en' ? 'en' : nav === 'he' ? 'he' : 'ru'
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [lang, setLangState] = useState<Lang>(initialLang)
  useEffect(() => {
    document.documentElement.lang = lang
    document.documentElement.dir = lang === 'he' ? 'rtl' : 'ltr'
  }, [lang])
  const setLang = useCallback((l: Lang) => { localStorage.setItem('inkwell.lang', l); setLangState(l) }, [])
  const value = useMemo<I18n>(() => ({
    lang, setLang, locale: LOCALES[lang],
    t: (key, vars) => {
      let s = dicts[lang][key] ?? key
      if (vars) for (const [k, v] of Object.entries(vars)) s = s.replace(`{${k}}`, String(v))
      return s
    },
  }), [lang, setLang])
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>
}

export function useI18n(): I18n {
  const v = useContext(Ctx)
  if (!v) throw new Error('useI18n must be used inside I18nProvider')
  return v
}

export type TranslationKey = Key
