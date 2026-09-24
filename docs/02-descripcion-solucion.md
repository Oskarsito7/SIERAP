# 02 — Solution description

## Scope

Web system to manage and assess the UPC's classroom projects and institutional RAP
(program learning outcomes), with a Java backend and a relational database
(PostgreSQL), replacing the current flow managed in AULAWEB.

## What it does

It lets students organize themselves into groups and upload versioned submissions of
their classroom project; teachers define groups and dates and grade; business people
and alumni grade the projects assigned to them; a faculty admin (dean) configure
moments and RAP per program; and a super_admin configure the global parameters of
the system. It produces individual, group and comparative reports across
semesters/years.

## Why (differentiating factor compared with AULAWEB)

Unlike AULAWEB —generic and not designed for this flow— the system is designed
specifically around the lifecycle of a RAP (moment → level → program → group →
submission → multi-role evaluation → report), with flexible configuration per
faculty/program and automatic generation of comparative reports, something that does
not exist today.

## What for

To reduce the operational burden on teachers and students, provide real
traceability of submissions and grades, and allow the university to make
curricular decisions based on data consolidated across moments and semesters.

## Technology scope decision (beyond the default guide)

The official UPC guide asks, by default, for a simple console/GUI application with
file persistence. **The teacher explicitly authorized** the project to be built as a
web application with a real database, knowing that one of the team members has 2+
years of web development experience. The only non-negotiable requirement set by the
teacher is that **the backend must be in Java**.

## Chosen technology stack

| Layer | Technology | Rationale |
|---|---|---|
| Backend | Java + Spring Boot (Spring Web, Spring Data JPA, Spring Security + JWT) | De facto standard for Java web; fits the layered architecture (controller/service/repository/entity) and the full OOP required by the rubric (interfaces for roles/evaluation strategies, User→subtypes inheritance, exception handling with `@ControllerAdvice`). |
| Database | PostgreSQL | More robust than MySQL for hierarchical relationships (faculty→program→moment→RAP) and better support for reporting aggregations. MySQL is a valid alternative. |
| Frontend | React + TypeScript | The team's usual stack, already known and approved by the teacher. |

See this decision as an ADR in
[`05-decisiones-tecnicas.md`](05-decisiones-tecnicas.md).

## Functional requirements

See the full detail, by role, in
[`04-requerimientos-funcionales.md`](04-requerimientos-funcionales.md).
