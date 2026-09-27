import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

import './index.css'
import { AuthProvider } from '@/auth/AuthProvider'
import { AppRouter } from '@/routes/AppRouter'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
      <AppRouter />
    </AuthProvider>
  </StrictMode>,
)
