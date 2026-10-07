# Verification report

Validated October 6 2026 on Windows with Java 21, Node 22.23.2 and portable PostgreSQL 17.6. Spring Boot 3.5.16, React 19.2.8, Vite 7.3.7 and Vitest 5.0.3 are pinned; the npm lockfile records exact transitive dependencies.

- Backend Maven verification and executable JAR packaging passed.
- All 20 JUnit tests passed: 17 PostgreSQL integration tests and 3 unit tests, including Mockito fault injection.
- Both frontend API-client tests passed and the React production build passed.
- Local browser smoke test passed registration, login, profile/address copy and save, catalog/cart, shipping, approved payment, confirmation/history reload and 375px layout overflow checks. Desktop and mobile screenshots were visually inspected.
- Final npm audit reports zero known vulnerabilities. This is a point-in-time registry result, not a guarantee that future advisories or application flaws are absent.
- Flyway clean schema setup and V1-to-V2 migration ran against PostgreSQL. Runtime database audit deletion was rejected. Checkout racing for one unit yielded one confirmation; duplicate submission decremented stock once; refresh reuse revoked the family; simulated failure left no partial order.

Docker is not installed on the authoring host. Compose files, Dockerfiles and CI workflow are supplied; container startup, image scans and GitHub CI have not been executed here. Load/availability targets, backup restoration and a formal accessibility/security audit remain unverified. Generated OpenAPI is available in local/demo; reviewed static examples do not replace live contract tests.

The database, generated credentials and temporary preview used for verification are local test assets and are excluded from the delivered source package. Setup scripts generate fresh secrets for the user's checkout. The requested source destination retains the original Word documents.
