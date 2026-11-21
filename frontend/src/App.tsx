import { Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import AuthPage from './pages/AuthPage'
import Catalog from './pages/Catalog'
import Feed from './pages/Feed'
import Home from './pages/Home'
import ItemDetail from './pages/ItemDetail'
import NotFound from './pages/NotFound'
import PostPage from './pages/PostPage'
import Profile from './pages/Profile'
import SearchPage from './pages/SearchPage'
import Studio from './pages/Studio'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Home />} />
        <Route path="catalog" element={<Catalog />} />
        <Route path="catalog/:id" element={<ItemDetail />} />
        <Route path="feed" element={<Feed />} />
        <Route path="posts/:id" element={<PostPage />} />
        <Route path="u/:username" element={<Profile />} />
        <Route path="search" element={<SearchPage />} />
        <Route path="studio" element={<Studio />} />
        <Route path="login" element={<AuthPage mode="login" />} />
        <Route path="register" element={<AuthPage mode="register" />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  )
}
