/** Estado general de la API (espejo de `EstadoSistemaResponse` del backend). */
export interface EstadoSistema {
  aplicacion: string
  version: string
  perfiles: string[]
  baseDatosDisponible: boolean
  fechaHora: string
}
