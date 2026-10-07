# Requirements and verification map

The Word requirements remain the business baseline. IDs appear in backend test display names.

| IDs | Implementation and evidence |
| --- | --- |
| BUS01 UX01 UX02 UX03 | U.S. addresses, new-only accessories, branding/navigation, contact page and saved inquiries. Server validation and accessory test. |
| UX04 | Labeled forms, focus styles and responsive layouts. 375px browser overflow check passes; formal accessibility audit is unperformed. |
| ACC01 ACC02 ACC03 | CUSTOMER-only registration, email uniqueness, account sequence, salted PBKDF2, credential verification, throttling, reset tokens and local mail sink. Registration injection/reset tests and browser login pass. |
| ACC04 ACC05 ACC06 | Business/individual profiles, primary/alternate contacts, mailing/shipping records and optimistic version. Browser profile copy/save and stale-version test pass. |
| SEC01 SEC02 SEC03 SEC04 | Four exclusive roles, ownership checks, short JWTs, rotating opaque refresh tokens, session revocation, CSRF/Origin checks. Ownership, privilege, replay/race and logout tests pass. |
| CAT01 CAT02 CAT03 | Specs/SKU/condition validation, search/filters/pagination and 12 deterministic demo products. Constraint tests and browser browsing pass. |
| ORD01 ORD02 ORD03 | Persisted cart, stock validation, order shipping override, ten minute quotes and server-controlled totals. Browser journey and changed-price test pass. |
| ORD04 ORD05 | Idempotent atomic checkout with deterministic stock locks. Retry, payload conflict, decline, final-stock race and rollback tests pass. |
| ORD06 ORD07 ORD08 | Cart cancellation flows, snapshots, history, confirmation and staff fulfillment. Browser confirmation/reload and API ownership checks pass; legal transitions are enforced in service. |
| PAY01 INV01 | Pure payment simulator, stock history, durable local outbox and explicit replenishment receipt. Payment and replenishment tests pass. |
| TECH01 TECH02 TECH03 DEV01 | Spring Boot/JPA/Security/Maven, React, PostgreSQL/Flyway, OpenAPI, Compose and VS Code scripts. Builds/migrations tested. Docker is unavailable locally, so Compose execution remains unverified. |
| OPS01 AUD01 | Profiles, external credentials/keys, JSON logging, correlation, liveness/readiness, protected metrics and append-only audit. Runtime audit-delete denial is tested. |
| TEST01 | JUnit/Mockito, real PostgreSQL integration, frontend API tests and browser journey. See verification.md. |
| NFR01 NFR02 NFR03 | Targets and runbooks provided; load, availability and backup-restore targets have not been measured. CI builds/tests/scans/tagged image builds are configured but not executed on GitHub. No external deployment occurred. |

Intentional differences from the design proposal: one role per account; catalog/stock share a row lock; cart writes serialize through a customer lock instead of a client cart-version field; local outbox adapters perform no remote calls; one RSA key uses controlled session revocation/restart for rotation. Demo shipping remains $10 and tax zero. Expired-record cleanup and commercial retention policies await approval. See architecture-decisions.md and operations.md.
