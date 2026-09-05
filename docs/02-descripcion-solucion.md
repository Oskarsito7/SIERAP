# 02 — Descripción de la solución

## Alcance

Sistema web para la gestión y evaluación de los proyectos de aula y RAP
institucionales de la UPC, con backend en Java y base de datos relacional
(PostgreSQL), que reemplaza el flujo actual gestionado en AULAWEB.

## Qué hace

Permite a estudiantes organizarse en grupos y subir entregas versionadas de su
proyecto de aula; a docentes definir grupos, fechas y evaluar; a empresarios y
egresados calificar los proyectos asignados; a un admin de facultad (decano)
configurar momentos y RAP por programa; y a un super_admin configurar los
parámetros globales del sistema. Genera reportes individuales, grupales y
comparativos entre semestres/años.

## Por qué (factor diferenciador frente a AULAWEB)

A diferencia de AULAWEB —genérico y no pensado para este flujo—, el sistema está
diseñado específicamente alrededor del ciclo de vida de un RAP (momento → nivel
→ programa → grupo → entrega → evaluación multi-rol → reporte), con
configuración flexible por facultad/programa y generación automática de
reportes comparativos, algo que hoy no existe.

## Para qué

Reducir el desgaste operativo de docentes y estudiantes, dar trazabilidad real
de entregas y calificaciones, y permitirle a la universidad tomar decisiones
curriculares basadas en datos consolidados entre momentos y semestres.

## Decisión de alcance tecnológico (más allá de la guía por defecto)

La guía oficial de la UPC pide, por defecto, una aplicación de consola/GUI
simple con persistencia en archivos. **El docente autorizó explícitamente** que
el proyecto se desarrolle como aplicación web con base de datos real, conociendo
la experiencia previa de +2 años en desarrollo web de uno de los integrantes del
equipo. El único requisito no negociable impuesto por el docente es que **el
backend esté en Java**.

## Stack tecnológico elegido

| Capa | Tecnología | Justificación |
|---|---|---|
| Backend | Java + Spring Boot (Spring Web, Spring Data JPA, Spring Security + JWT) | Estándar de facto para Java web; encaja con arquitectura por capas (controller/service/repository/entity) y POO completa (interfaces para roles/estrategias de evaluación, herencia Usuario→subtipos, manejo de excepciones con `@ControllerAdvice`) exigidas por la rúbrica. |
| Base de datos | PostgreSQL | Más robusto que MySQL para relaciones jerárquicas (facultad→programa→momento→RAP) y mejor soporte de agregaciones para reportería. MySQL es alternativa válida. |
| Frontend | React + TypeScript | Stack habitual del equipo, ya conocido y aprobado por el docente. |

Ver el detalle de esta decisión como ADR en
[`05-decisiones-tecnicas.md`](05-decisiones-tecnicas.md).

## Requerimientos funcionales

Ver el detalle completo, por rol, en
[`04-requerimientos-funcionales.md`](04-requerimientos-funcionales.md).
