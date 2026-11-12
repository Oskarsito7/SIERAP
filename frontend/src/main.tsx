import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

// Tipografías de la identidad UPC, servidas por la propia app (sin depender de internet).
import '@fontsource/open-sans/400.css'
import '@fontsource/open-sans/600.css'
import '@fontsource/open-sans/700.css'
import '@fontsource/montserrat/600.css'
import '@fontsource/montserrat/700.css'
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
