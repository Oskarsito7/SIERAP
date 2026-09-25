# D3 · Stage 1 draft — operations (`ProjectGroup`, `Submission`, `Evaluation`, `CriterionGrade`)

Ready-to-move code for task 3 of Stage 1 (`docs/06-plan-de-trabajo.md`). It lives
outside `src/` so Maven does not compile it until the user entities of D1 (`V3`)
are in `develop`.

## Contents

| Draft file | Goes to |
|---|---|
| `src/main/java/.../entity/ProjectGroup.java`, `Submission.java`, `Evaluation.java`, `CriterionGrade.java` | `backend/src/main/java/co/edu/unicesar/sierap/entity/` |
| `src/main/java/.../repository/*Repository.java` (4) | `backend/src/main/java/co/edu/unicesar/sierap/repository/` |
| `src/main/resources/db/migration/V4__operations.sql` | `backend/src/main/resources/db/migration/` |
| `src/test/java/.../entity/ProjectGroupTest.java`, `EvaluationTest.java` (unit) | `backend/src/test/java/co/edu/unicesar/sierap/entity/` |
| `src/test/java/.../entity/OperationsIntegrationTest.java` (Testcontainers) | same |

## How it was verified (2026-09-25, on top of `develop` 9c489f7)

With minimal stubs of `User` (abstract, `JOINED`, table `app_user`), `Teacher`,
`Student` and `ExternalEvaluator` and a stub `V3` creating those four tables:

- all unit tests pass (11 new + the existing ones);
- Flyway applies V1 → V4 and Hibernate `validate` accepts the four new entities
  (the app starts and `/api/status` answers);
- `OperationsIntegrationTest` passes (8/8) against a real PostgreSQL 16:
  scope queries per role, one group per moment, submission history, the unique
  version constraint, Moment 1 adding up to 5.00, correcting a grade in place
  and `RESTRICT` on a performance level already used in a grade.

## Assumptions about D1's code (check them when V3 is merged)

1. Classes `User` (abstract, with `getId()`), `Teacher`, `Student`,
   `ExternalEvaluator` in package `entity`.
2. Tables `app_user`, `teacher`, `student`, `external_evaluator`, each with a
   `BIGINT id` primary key (ADR-009, `JOINED`).
3. Each user class has the no-arg constructor JPA needs (the tests use it from
   the same package). If D1's users have required columns, change only the
   `teacher()`, `student()` and `externalEvaluator()` helpers at the end of
   `OperationsIntegrationTest`.

## Integration checklist

1. `git checkout develop && git pull` (V3 merged) and branch `feature/d3-operations`.
2. Move the files per the table above and delete `backend/drafts/`.
3. Adjust names if D1 used others (table names in `V4__operations.sql`, class
   names in the entities).
4. `Evaluator` interface (with D1): proposal below.
5. `./mvnw verify` (needs Docker for Testcontainers).
6. Update `docs/03-entidades-dominio.md` (new attributes: `Evaluation.evaluatedAt`,
   `CriterionGrade.level` / `score`) and the `.drawio` diagram.
7. PR to `develop` with the pending decisions below in the description.

## `Evaluator` proposal (to agree with D1)

```java
/** Quien puede calificar un grupo: el docente del grupo o un evaluador externo asignado. */
public interface Evaluator {

    Long getId();

    /** Indica si este evaluador puede calificar el grupo. */
    default boolean canEvaluate(ProjectGroup group) {
        return group.canBeGradedBy((User) this);
    }
}
```

`Teacher` and `ExternalEvaluator` would declare `implements Evaluator`.
`Evaluation.evaluator` stays typed as `User` because JPA cannot map an interface.

## Decisions taken in this draft (to confirm in the PR)

- **Delete policy:** whatever belongs to the group (members, assigned evaluators,
  submissions, evaluations, grades) is deleted with it (`ON DELETE CASCADE`).
  Moments, teachers, criteria and levels that have groups or grades **cannot** be
  deleted (`RESTRICT`), so a configuration change never erases past grades
  (ADR-010). Consequence: deleting them throws `DataIntegrityViolationException`,
  which `GlobalExceptionHandler` (D1) should turn into a 409/422 instead of a 500.
- **Score snapshot:** `CriterionGrade.score` copies the level score when grading.
- **One evaluation per evaluator per group** (unique constraint); correcting
  updates the same row.
- **One group per student per moment** is checked in the service
  (`existsByMomentIdAndMembersId`), not in the database: the join table has no
  `moment_id`.
- Entity comparisons use ids, not `equals`, because lazy relations may be
  Hibernate proxies.

## Still open (bring to the team)

- Where submission files are stored (disk / object storage) → needs an ADR.
- Launching with groups under the minimum; switching groups during group
  formation; how teacher and external grades are combined (`docs/04`).
