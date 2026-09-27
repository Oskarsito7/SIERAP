import { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { TokenResponse } from '@/types/auth'

import { httpClient, onSessionChange, refreshSession, setSession } from './httpClient'

function session(accessToken: string): TokenResponse {
  return {
    accessToken,
    expiresIn: 900,
    user: {
      id: 1,
      firstNames: 'Marta',
      lastNames: 'Díaz',
      fullName: 'Marta Díaz',
      email: 'marta@unicesar.edu.co',
      role: 'TEACHER',
      active: true,
      mustChangePassword: false,
    },
  }
}

function respond(config: InternalAxiosRequestConfig, status: number, data: unknown = {}): AxiosResponse {
  const response: AxiosResponse = { data, status, statusText: '', headers: {}, config }
  if (status >= 400) {
    throw new AxiosError(`Request failed with status code ${status}`, 'ERR_BAD_RESPONSE', config, null, response)
  }
  return response
}

type Handler = (config: InternalAxiosRequestConfig) => AxiosResponse | Promise<AxiosResponse>

const originalAdapter = httpClient.defaults.adapter
let requests: InternalAxiosRequestConfig[]

/** Reemplaza la red por `handler` y registra cada petición enviada. */
function useServer(handler: Handler) {
  requests = []
  httpClient.defaults.adapter = async (config) => {
    requests.push(config)
    return handler(config)
  }
}

const authorizationOf = (config: InternalAxiosRequestConfig) => config.headers.Authorization

describe('httpClient', () => {
  beforeEach(() => {
    setSession(null)
  })

  afterEach(() => {
    httpClient.defaults.adapter = originalAdapter
  })

  it('sends the access token of the current session', async () => {
    useServer((config) => respond(config, 200))
    setSession(session('token-1'))

    await httpClient.get('/users')

    expect(authorizationOf(requests[0])).toBe('Bearer token-1')
  })

  it('shares a single in-flight refresh between simultaneous calls', async () => {
    let resolveRefresh: () => void = () => {}
    useServer(
      (config) =>
        new Promise((resolve) => {
          resolveRefresh = () => resolve(respond(config, 200, session('token-2')))
        }),
    )

    // Carga de la app + doble efecto de StrictMode: la cookie se presenta una sola vez.
    const first = refreshSession()
    const second = refreshSession()
    // axios llama al adaptador de forma asíncrona: se espera a que llegue la petición.
    await vi.waitFor(() => expect(requests).toHaveLength(1))
    resolveRefresh()

    await expect(Promise.all([first, second])).resolves.toHaveLength(2)
    expect(requests).toHaveLength(1)
    expect(requests[0].url).toBe('/auth/refresh')
  })

  it('refreshes once and retries every call that answered 401', async () => {
    setSession(session('expired'))
    useServer(async (config) => {
      if (config.url === '/auth/refresh') {
        await Promise.resolve()
        return respond(config, 200, session('fresh'))
      }
      return authorizationOf(config) === 'Bearer fresh' ? respond(config, 200, config.url) : respond(config, 401)
    })

    const [users, groups] = await Promise.all([httpClient.get('/users'), httpClient.get('/groups')])

    expect(users.data).toBe('/users')
    expect(groups.data).toBe('/groups')
    expect(requests.filter((config) => config.url === '/auth/refresh')).toHaveLength(1)
  })

  it('ends the session and rejects the call when the refresh fails', async () => {
    const listener = vi.fn()
    const unsubscribe = onSessionChange(listener)
    setSession(session('expired'))
    useServer((config) => respond(config, 401))

    await expect(httpClient.get('/users')).rejects.toMatchObject({ response: { status: 401 } })

    expect(listener).toHaveBeenLastCalledWith(null)
    expect(requests.map((config) => config.url)).toEqual(['/users', '/auth/refresh'])
    unsubscribe()
  })

  it('does not refresh when the login answers 401 (wrong credentials)', async () => {
    useServer((config) => respond(config, 401))

    await expect(
      httpClient.post('/auth/login', { email: 'a@unicesar.edu.co', password: 'x' }, { skipAuthRefresh: true }),
    ).rejects.toBeInstanceOf(AxiosError)

    expect(requests).toHaveLength(1)
  })
})
