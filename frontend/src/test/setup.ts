import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Desmonta lo renderizado entre pruebas para que no se contaminen entre sí.
afterEach(() => {
  cleanup()
})
