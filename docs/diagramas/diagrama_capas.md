# Diagrama de capas (arquitectura por paquetes)

Cliente web React + API REST en Spring Boot organizada en **presentación, lógica de
negocio, datos y entidades** —las cuatro capas que pide la rúbrica (RAP2)—, con
componentes transversales. GitHub renderiza este diagrama directamente a partir del
código Mermaid.

> Versión con diseño detallado: [`diagrama_capas.png`](diagrama_capas.png) · editable en
> [`diagrama_capas.drawio`](diagrama_capas.drawio).

```mermaid
flowchart TB
  subgraph FE["PRESENTACIÓN — cliente web · sierap-frontend (React + TypeScript)"]
    direction LR
    pages["<b>pages</b><br/>vistas por rol"]
    components["<b>components</b><br/>formularios, tablas, gráficas"]
    routes["<b>routes</b><br/>rutas protegidas por rol"]
    auth["<b>context / auth</b><br/>sesión y token JWT"]
    api["<b>services / api</b><br/>clientes HTTP tipados"]
  end

  subgraph BE["co.edu.unicesar.sierap — backend (Java 21 · Spring Boot 3)"]
    direction TB
    subgraph P["PRESENTACIÓN — API REST"]
      direction LR
      controller["<b>controller</b><br/>@RestController<br/>GrupoController, EvaluacionController …"]
      dto["<b>dto</b><br/>request / response<br/>validación con @Valid"]
      mapper["<b>mapper</b><br/>entidad ⇄ DTO"]
    end
    subgraph L["LÓGICA DE NEGOCIO"]
      direction LR
      service["<b>service</b><br/>«interface» GrupoService,<br/>EvaluacionService, ReporteService …"]
      impl["<b>service.impl</b><br/>@Service · @Transactional<br/>reglas: 3–5 integrantes, fechas UPC,<br/>alcance por rol"]
    end
    subgraph DA["DATOS"]
      repository["<b>repository</b><br/>extends JpaRepository&lt;T, Long&gt;<br/>consultas de reportería con @Query"]
    end
    subgraph EN["ENTIDADES"]
      direction LR
      entity["<b>entity</b><br/>Usuario {abstract} + 6 roles, Facultad, Programa,<br/>Momento, RAP, Grupo, ProyectoAula, Evaluacion …"]
      enums["<b>entity.enums</b><br/>RolUsuario · NivelExigencia<br/>NivelDesempeno"]
    end
    subgraph T["TRANSVERSALES"]
      direction TB
      security["<b>security</b><br/>SecurityConfig · JwtAuthFilter<br/>@PreAuthorize por rol"]
      exception["<b>exception</b><br/>GlobalExceptionHandler «@ControllerAdvice»<br/>SierapException y subclases"]
      config["<b>config</b><br/>CorsConfig · StorageConfig<br/>application-dev / prod"]
    end
  end

  subgraph INFRA["INFRAESTRUCTURA"]
    direction LR
    db[("<b>PostgreSQL</b><br/>migraciones Flyway")]
    files[("<b>Almacenamiento de archivos</b><br/>.zip · .pdf · .docx")]
  end

  api -- "HTTP / JSON · Bearer JWT" --> controller
  controller -. «use» .-> dto
  mapper -. «use» .-> dto
  controller -. «use» .-> service
  impl -. implementa .-> service
  service -. «use» .-> repository
  repository -. «use» .-> entity
  security -- filtra cada petición --> controller
  exception -. maneja errores .-> controller
  repository -- Spring Data JPA · JDBC --> db
  impl -- guarda / lee archivos --> files

  classDef pres fill:#DBEAFE,stroke:#3B6FB6,color:#1F2937
  classDef log fill:#EDE4FB,stroke:#7E57C2,color:#1F2937
  classDef dat fill:#FEF3C7,stroke:#B7791F,color:#1F2937
  classDef ent fill:#DCFCE7,stroke:#2F855A,color:#1F2937
  classDef tr fill:#F1F5F9,stroke:#64748B,color:#1F2937
  classDef inf fill:#E0F2FE,stroke:#0369A1,color:#1F2937
  class pages,components,routes,auth,api,controller,dto,mapper pres
  class service,impl log
  class repository dat
  class entity,enums ent
  class security,exception,config tr
  class db,files inf
```

## Reglas de la arquitectura

| Regla | Por qué |
|---|---|
| Cada capa solo usa la inmediatamente inferior (`controller → service → repository → entity`). | Es la arquitectura por capas completa que evalúa la rúbrica (RAP2). |
| Las entidades JPA nunca salen de la API: el controller responde siempre con DTOs. | Evita exponer `passwordHash` o relaciones internas y desacopla la API del modelo. |
| Sin lógica de negocio en los controllers. | Las reglas (3–5 integrantes, fechas dentro del rango UPC, alcance por rol) viven en `service.impl` y se prueban con JUnit + Mockito. |
| Sin `try/catch` disperso: los errores se lanzan como `SierapException` y los traduce `exception`. | Respuestas de error uniformes y manejo de excepciones centralizado (rúbrica RAP2). |
| Nada de valores de negocio hardcodeados. | RAP, pesos y rangos de semestre se leen de la base de datos (`ParametroGlobal`, `Momento`, `RAP`). |

## Correspondencia con la rúbrica

| Capa que pide la guía UPC | Paquete en SIERAP |
|---|---|
| Presentación | `frontend` (React) + `controller`, `dto`, `mapper` |
| Lógica | `service`, `service.impl` |
| Datos | `repository` + PostgreSQL |
| Entidades | `entity`, `entity.enums` |
