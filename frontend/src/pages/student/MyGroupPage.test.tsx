import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { AuthContext } from '@/auth/authContext'
import { groupService } from '@/services/api/groupService'
import { momentService } from '@/services/api/momentService'
import { submissionService } from '@/services/api/submissionService'
import type { Group } from '@/types/group'
import type { MomentSummary } from '@/types/moment'

import { groupFixture, momentFixture } from '../groups/testData'
import { MyGroupPage } from './MyGroupPage'
import { studentUser, submissionFixture } from './testData'

vi.mock('@/services/api/momentService', () => ({ momentService: { list: vi.fn(), get: vi.fn() } }))
vi.mock('@/services/api/groupService', () => ({
  groupService: { myGroups: vi.fn(), updateProjectTitle: vi.fn() },
}))
vi.mock('@/services/api/submissionService', () => ({
  submissionService: { history: vi.fn(), upload: vi.fn(), downloadZip: vi.fn(), downloadPresentation: vi.fn() },
}))

const myGroups = vi.mocked(groupService.myGroups)
const updateTitle = vi.mocked(groupService.updateProjectTitle)
const listMoments = vi.mocked(momentService.list)
const history = vi.mocked(submissionService.history)
const upload = vi.mocked(submissionService.upload)

const MB = 1024 * 1024
const me = { id: 40, fullName: 'Juan Rojas', studentCode: '2026001', email: 'juan@unicesar.edu.co' }

/** Momento lanzado con la ventana de entregas abierta hasta el 31/10/2026. */
function launched(overrides: Partial<MomentSummary> = {}): MomentSummary {
  return momentFixture({ status: 'LAUNCHED', groupFormationOpen: false, submissionOpen: true, ...overrides })
}

function group(overrides: Partial<Group> = {}): Group {
  return groupFixture({ projectTitle: 'SIERAP', members: [me], memberCount: 1, myGroup: true,
    submissionDeadline: '2026-10-25', evaluationDate: '2026-11-10', ...overrides })
}

function file(name: string, size: number): File {
  const created = new File(['PK'], name)
  Object.defineProperty(created, 'size', { value: size })
  return created
}

function renderPage(path = '/my-group') {
  render(
    <AuthContext.Provider
      value={{ state: { status: 'authenticated', user: studentUser }, user: studentUser, logout: vi.fn() }}
    >
      <MemoryRouter initialEntries={[path]}>
        <MyGroupPage />
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('MyGroupPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 9, 14, 10, 0))
    myGroups.mockReset()
    updateTitle.mockReset()
    listMoments.mockReset()
    history.mockReset()
    upload.mockReset()
    return () => vi.useRealTimers()
  })

  it('shows the group, its dates and the history of versions with their status', async () => {
    myGroups.mockResolvedValue([group()])
    listMoments.mockResolvedValue([launched()])
    history.mockResolvedValue({
      versions: [
        submissionFixture({ id: 92, versionNumber: 2, late: true, toGrade: false, zipFileName: 'sierap-v2.zip' }),
        submissionFixture({ id: 91, versionNumber: 1, presentationFileName: 'sustentacion.pdf',
          presentationSize: MB }),
      ],
      currentPresentation: submissionFixture({ id: 91, versionNumber: 1, presentationFileName: 'sustentacion.pdf' }),
      maxVersions: 20,
    })
    renderPage()

    expect(await screen.findByText('sierap-v2.zip')).toBeInTheDocument()
    expect(screen.getByText('Juan Rojas (tú)')).toBeInTheDocument()
    expect(screen.getByLabelText('Título del proyecto')).toHaveValue('SIERAP')
    expect(screen.getByText('25/10/2026')).toBeInTheDocument()
    const rows = screen.getAllByRole('row')
    expect(within(rows[1]).getByText('Tardía')).toBeInTheDocument()
    expect(within(rows[2]).getByText('A tiempo')).toBeInTheDocument()
    expect(within(rows[2]).getByText('Se califica')).toBeInTheDocument()
    expect(within(rows[2]).getByText('14/10/2026 22:41')).toBeInTheDocument()
    expect(screen.getByText('La presentación vigente es sustentacion.pdf, de la versión 1.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Subir versión 3' })).toBeInTheDocument()
    expect(screen.getByText(/Será la versión 3 de 20\./)).toBeInTheDocument()
    expect(screen.getByText(/Tu fecha límite es el 25\/10\/2026/)).toBeInTheDocument()
  })

  it('uploads a new version and adds it to the history', async () => {
    myGroups.mockResolvedValue([group()])
    listMoments.mockResolvedValue([launched()])
    history.mockResolvedValue({ versions: [], currentPresentation: null, maxVersions: 20 })
    upload.mockResolvedValue(submissionFixture({ id: 95, zipFileName: 'final.zip', presentationFileName: 'final.pdf',
      presentationSize: MB }))
    renderPage()

    const zip = file('final.zip', 41 * MB)
    const presentation = file('final.pdf', MB)
    await userEvent.upload(await screen.findByLabelText('Proyecto (.zip)'), zip)
    await userEvent.upload(screen.getByLabelText('Presentación (opcional)'), presentation)
    expect(screen.getByText('final.zip · 41 MB')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Subir versión 1' }))

    expect(upload).toHaveBeenCalledWith(7, zip, presentation, expect.any(Function), expect.any(AbortSignal))
    expect(await screen.findByRole('status')).toHaveTextContent('Se subió la versión 1.')
    expect(screen.getByRole('button', { name: 'Descargar final.zip' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Subir versión 2' })).toBeInTheDocument()
  })

  it('checks the size and type of the files before uploading', async () => {
    myGroups.mockResolvedValue([group()])
    listMoments.mockResolvedValue([launched()])
    history.mockResolvedValue({ versions: [], currentPresentation: null, maxVersions: 20 })
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Subir versión 1' }))
    expect(screen.getByText('Adjunta el archivo .zip con el proyecto.')).toBeInTheDocument()

    await userEvent.upload(screen.getByLabelText('Proyecto (.zip)'), file('enorme.zip', 150 * MB))
    await userEvent.upload(screen.getByLabelText('Presentación (opcional)'), file('sustentacion.pdf', 40 * MB))
    await userEvent.click(screen.getByRole('button', { name: 'Subir versión 1' }))

    expect(screen.getByText('El .zip pesa 150 MB: el máximo es 100 MB.')).toBeInTheDocument()
    expect(screen.getByText('La presentación pesa 40 MB: el máximo es 30 MB.')).toBeInTheDocument()
    expect(upload).not.toHaveBeenCalled()
  })

  it('accepts a dropped file, validates it at once and lets the student remove it', async () => {
    myGroups.mockResolvedValue([group()])
    listMoments.mockResolvedValue([launched()])
    history.mockResolvedValue({ versions: [], currentPresentation: null, maxVersions: 20 })
    renderPage()

    const dropzone = await screen.findByTestId('dropzone-Proyecto (.zip)')
    fireEvent.drop(dropzone, { dataTransfer: { files: [file('notas.rar', MB)] } })
    expect(screen.getByText('El proyecto debe ser un archivo .zip.')).toBeInTheDocument()

    fireEvent.drop(dropzone, { dataTransfer: { files: [file('final.zip', 2 * MB)] } })
    expect(screen.queryByText('El proyecto debe ser un archivo .zip.')).not.toBeInTheDocument()
    expect(screen.getByText('final.zip · 2,0 MB')).toBeInTheDocument()

    await userEvent.upload(screen.getByLabelText('Presentación (opcional)'), file('equivocada.pdf', MB))
    await userEvent.click(screen.getByRole('button', { name: 'Quitar equivocada.pdf' }))
    expect(screen.queryByText(/equivocada\.pdf/)).not.toBeInTheDocument()
  })

  it('shows the progress of the upload and lets the student cancel it', async () => {
    myGroups.mockResolvedValue([group()])
    listMoments.mockResolvedValue([launched()])
    history.mockResolvedValue({ versions: [], currentPresentation: null, maxVersions: 20 })
    upload.mockImplementation((_group, _zip, _presentation, onProgress, signal) => {
      onProgress?.(40)
      return new Promise((_resolve, reject) =>
        signal?.addEventListener('abort', () => reject(Object.assign(new Error('canceled'), { __CANCEL__: true }))),
      )
    })
    renderPage()

    await userEvent.upload(await screen.findByLabelText('Proyecto (.zip)'), file('final.zip', 10 * MB))
    await userEvent.click(screen.getByRole('button', { name: 'Subir versión 1' }))

    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '40')
    expect(screen.getByText('40 % · 4,0 MB de 10 MB')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Cancelar subida' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Se canceló la subida.')
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument()
    expect(screen.getByText('final.zip · 10 MB')).toBeInTheDocument()
  })

  it('shows how many versions are left and stops offering the upload at the limit', async () => {
    myGroups.mockResolvedValue([group()])
    listMoments.mockResolvedValue([launched()])
    history.mockResolvedValue({
      versions: [submissionFixture({ id: 92, versionNumber: 2 }), submissionFixture({ id: 91, versionNumber: 1 })],
      currentPresentation: null,
      maxVersions: 2,
    })
    renderPage()

    expect(await screen.findByText('Tu grupo ha subido 2 de 2 versiones permitidas.')).toBeInTheDocument()
    expect(screen.getByText(/ya subió las 2 versiones permitidas/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Subir versión/ })).not.toBeInTheDocument()
  })

  it('warns that the version will be late after the group deadline', async () => {
    myGroups.mockResolvedValue([group({ submissionDeadline: '2026-10-10' })])
    listMoments.mockResolvedValue([launched()])
    history.mockResolvedValue({ versions: [], currentPresentation: null, maxVersions: 20 })
    renderPage()

    expect(
      await screen.findByText(/Tu fecha límite fue el 10\/10\/2026: esta versión quedará marcada como tardía\./),
    ).toBeInTheDocument()
  })

  it('does not offer to upload outside the submission window', async () => {
    myGroups.mockResolvedValue([group()])
    listMoments.mockResolvedValue([launched({ submissionOpen: false, submissionEnd: '2026-10-10' })])
    history.mockResolvedValue({ versions: [], currentPresentation: null, maxVersions: 20 })
    renderPage()

    expect(await screen.findByText('La ventana de entregas cerró el 10/10/2026.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Subir versión/ })).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Título del proyecto')).not.toBeInTheDocument()
  })

  it('saves the project title', async () => {
    myGroups.mockResolvedValue([group({ projectTitle: null })])
    listMoments.mockResolvedValue([launched()])
    history.mockResolvedValue({ versions: [], currentPresentation: null, maxVersions: 20 })
    updateTitle.mockResolvedValue(group({ projectTitle: 'SIERAP 2' }))
    renderPage()

    await userEvent.type(await screen.findByLabelText('Título del proyecto'), ' SIERAP 2 ')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar título' }))

    expect(updateTitle).toHaveBeenCalledWith(7, 'SIERAP 2')
    expect(await screen.findByText('Título guardado.')).toBeInTheDocument()
  })

  it('invites the student to join a group when they have none', async () => {
    myGroups.mockResolvedValue([])
    listMoments.mockResolvedValue([launched()])
    renderPage()

    expect(await screen.findByText('Todavía no estás en un grupo')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver los grupos del momento' })).toHaveAttribute('href', '/moment-groups')
  })
})
