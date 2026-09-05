# 00 — Guía oficial y rúbrica del Momento 1 (fuente UPC)

> Transcripción fiel de la guía oficial entregada por la universidad
> (`Guía_de_proyecto_de_aula_-_Programacion_II_-_Momento_1__2023-1__V3.pdf`) y del
> documento `Rap_p2.pdf`. Úsala como fuente de verdad para verificar que cualquier
> entregable cumpla los criterios de evaluación.

## Datos generales

| Campo | Valor |
|---|---|
| Momento de evaluación | Momento 1 (tercer semestre) |
| Nombre del proyecto de aula | Proyecto de aula momento uno (1) |
| Asignatura donde se evalúa | Programación II (SS300) |
| Tipo de actividad | Grupal (parejas) |
| Peso evaluativo | 5 puntos |

## Los 5 RAP evaluados en todos los momentos

| RAP | Descripción | Momento 1 (bajo) | Momento 2 (medio) | Momento 3 (alto) |
|---|---|---|---|---|
| RAP1 | INTEGRA las ciencias básicas y las ciencias básicas de la ingeniería para la resolución de problemas reales, promoviendo el desarrollo sostenible. | Programación de Computadores II | Ingeniería de Software II | Electiva de profundización III |
| RAP2 | DESARROLLA productos de software que resuelven problemas reales, aplicando buenas prácticas y estándares de calidad. | Programación de Computadores II | Ingeniería de Software II | Electiva de profundización III |
| RAP3 | CONSTRUYE modelos computacionales basados en métodos analíticos y experimentales para el desarrollo de soluciones de software. | Programación de Computadores II | Ingeniería de Software II | Electiva de profundización III |
| RAP4 | GESTIONA proyectos de Tecnologías de la Información, para satisfacer las necesidades del entorno global. | Programación de Computadores II | Ingeniería de Software II | Electiva de profundización III |
| RAP5 | EMPRENDE aplicando ciencia, tecnología e innovación desde su profesión, con pensamiento crítico, responsabilidad ética, liderazgo y colaboración. | Programación de Computadores II | Ingeniería de Software II | Electiva de profundización III |

## Conocimientos previos requeridos

Pensamiento computacional, fundamentos de programación y construcción de algoritmos
computacionales. Dominio de al menos un paradigma de construcción de aplicaciones de
software (estructurado, orientado a objetos, orientado a eventos).

## Requisitos del producto de software (guía por defecto)

> **Nota:** SIERAP amplía deliberadamente estos requisitos por defecto (ver
> `docs/02-descripcion-solucion.md`, sección de stack), con autorización explícita
> del docente. Se documentan aquí igual porque la rúbrica de evaluación sigue
> aplicando sobre los mismos criterios de fondo (POO, arquitectura por capas,
> buenas prácticas), solo que implementados en un contexto web con base de datos
> en lugar de consola con archivos.

- Entorno gráfico (implementación de interfaces gráficas de usuario).
- Persistencia (la guía por defecto pide archivos; SIERAP usa base de datos real).
- Uso general de técnicas y/o mecanismos de POO: abstracción, encapsulamiento,
  sobrecarga, relaciones entre clases, herencia, polimorfismo, colecciones de
  objetos, manejo de excepciones.
- Implementación de arquitectura por capas (entidades, datos, servicios,
  presentación).
- Aplicación de estándares de nomenclatura para paquetes, clases, métodos y
  atributos.
- Cada clase debidamente documentada.
- Buenas prácticas de escritura de código.

## Fases del proyecto de aula

**Fase 1 — Identificación, formulación, comprensión y especificación del problema:**
- Identificación y formulación del problema/necesidad (causas, consecuencias, área
  de aplicación).
- Estado del arte / vigilancia tecnológica (revisión de soluciones existentes,
  funcionalidades comparadas, fuentes citadas).
- Descripción de la solución (alcance, qué/porqué/para qué, factor diferenciador,
  requerimientos funcionales incluyendo control de acceso).
- Diseño preliminar: identificar entidades (listado + descripción breve), diagrama
  UML por clase (nombre + atributos, métodos opcionales), diagrama de clases
  preliminar (relaciones de agregación y composición, notación UML), implementación
  de código (avance mostrando funcionamiento de objetos).

**Fase 2 — Definición de la arquitectura del proyecto:**
- Diagrama de clases mejorado (herencia, clases abstractas, interfaces).
- Definición de la arquitectura (diagrama de paquetes).
- Diseño preliminar de interfaces gráficas de usuario (mockup o wireframe).
- Funcionalidad completa del aplicativo en modo consola y colecciones de objetos
  (ArrayList).

**Fase 3 — Desarrollo del producto final de software:**
- Software final funcional (todas las funcionalidades, GUI, persistencia).

## Estructura del documento de entrega

Preliminares (hoja de presentación, tabla de contenidos) + desarrollo del tema:
1. Planteamiento del problema
2. Descripción de la solución
3. Diseño preliminar
4. Arquitectura del proyecto (diagrama de paquetes)
5. Diseño de GUI
6. Código desarrollado (funcionalidad completa en modo consola y colecciones de
   objetos — ArrayList)

**Sustentación:** todos los integrantes deben sustentar el documento y demostrar el
aplicativo. Máximo 10 minutos de pitch. La calificación por sustentación es
**individual**.

## Rúbrica de evaluación completa (peso total: 5 puntos)

| RAP | Ítem evaluado | Bajo (0) | Básico (0.36) | Alto (0.48) | Sobresaliente (0.6 / 0.8) |
|---|---|---|---|---|---|
| RAP1 | Identificación del problema | No identifica problemática real, ni causas ni consecuencias | Identifica problemática real, sin causas ni consecuencias | Identifica problemática, presenta causas O consecuencias (no ambas) | Identifica y describe problema central, causas y consecuencias |
| RAP5 | Descripción de la solución | No describe solución, alcance, factor diferenciador ni requerimientos | Describe solución, sin alcance/factor diferenciador/requerimientos | Describe solución, alcance y requerimientos, sin factor diferenciador | Describe solución, alcance, factor diferenciador y requerimientos funcionales |
| RAP3 | Análisis y diseño (diagrama de clases UML) | No presenta diagrama de clases UML | Identifica clases/atributos, elabora diagrama pero sin notación UML adecuada | Identifica clases/atributos, diagrama con notación UML adecuada | Identifica clases, atributos, relaciones (asociación, agregación, composición, herencia), diagrama con notación UML adecuada |
| RAP2 | Implementación arquitectura por capas | No aplica arquitectura por capas | Implementa solo 1 capa | Implementa entre 2 y 3 capas | Implementa en Java bajo arquitectura por capas completa (datos, lógica, presentación, entidades) |
| RAP4 | Funcionalidad de la solución desarrollada | No cumple requerimientos funcionales | Soluciona mínimamente la problemática | Soluciona medianamente la problemática | Soluciona completamente la problemática identificada |
| RAP2 | Uso de técnicas generales de POO | No incluye mecanismos ni principios de POO | Usa encapsulamiento y sobrecarga de métodos | Usa encapsulamiento, herencia, polimorfismo/interfaces | Usa encapsulamiento, herencia, polimorfismo/interfaces y gestión de excepciones |
| RAP4 | Uso de buenas prácticas en escritura de código | No aplica buenas prácticas | Código debidamente identado | + notación camelCase para clases/métodos/variables | + estándares de nombres de paquetes y documentación por comentarios |
| RAP5 | Sustentación y/o nivel de dominio (0/0.48/0.64/0.8) | No realiza sustentación | Demuestra dominio del problema propuesto | + dominio de la solución diseñada | + dominio del código desarrollado |

## Recomendaciones / observaciones oficiales

- Proyecto copiado de otro ya elaborado: motivo de anulación, sin discusión.
- La calificación por sustentación es individual — determina el grado de
  participación, conocimiento y dominio de cada estudiante sobre el proyecto.
- Código que no compile no será evaluado.
- Centrar la atención en los requerimientos básicos del sistema, sin complicar
  las soluciones aunque el proyecto pueda ser ambicioso.
- Administrar adecuadamente el tiempo libre para desarrollar el proyecto sin
  interrupciones.
