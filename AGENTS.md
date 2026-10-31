# Project rules

- Implement one milestone at a time. Stop after completing the requested milestone.
- Use Java 17, Maven, JDBC, Gson, and built-in HttpServer.
- Do not create automated test classes or restore test dependencies unless the user
  explicitly requests them. Verify builds with Maven clean package.
- Do not introduce Spring Boot, Hibernate, JPA, Lombok, or unnecessary frameworks.
- Keep entity and DTO packages separate. Follow Handler -> Service -> DAO -> PostgreSQL.
- Use English source names and comments; teach and explain decisions in Vietnamese.
- Explain the objective, class responsibilities, key decisions, and a concrete execution example.
- End each milestone with three technical questions and update docs/MILESTONES.md.
- Never claim verification passed unless actually executed. Record environment limitations.
- Do not silently change previously completed code; explain changes first.
- Before database-dependent implementation, inspect or request the actual schema.
- Never recreate, reset, drop, or modify existing database tables without explicit permission.
- Database: watch_store, schema app. Do not commit credentials or local configuration.
- Later business logic must use secure password hashing, server-side authorization,
  prepared statements, and transactional checkout.
- Demonstrate meaningful inheritance, polymorphism, abstraction, and encapsulation.
- Keep documentation consistent with implemented code; mark future designs as planned.

## Milestone Git commits

- After completing and successfully verifying each milestone, create a Git commit.
- Commit only implemented source code and necessary project configuration files.
- Do not commit documentation (including AGENTS.md), build artifacts, logs, database
  credentials, .env files, or other sensitive information.
- Run the relevant build and tests before committing. If verification fails, explain
  the failure and resolve it before creating a commit.
- Review git status and git diff before staging files.
- Stage intended files explicitly; never use git add . to stage everything.
- Use clear commit messages, such as feat: implement BaseEntity.
- Do not push unless explicitly requested by the user.
- Do not amend or rewrite existing commits without permission.
- After committing, report the commit message, commit hash, and files committed.
