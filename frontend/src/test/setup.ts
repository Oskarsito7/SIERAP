import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Unmounts what was rendered between tests so they do not affect each other.
afterEach(() => {
  cleanup()
})
