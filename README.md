# NP Computers

Java 21 / Spring Boot / Maven / React / PostgreSQL enterprise demo. This storefront sells new and used computers and new accessories to U.S. individuals and businesses. All payments and email delivery are simulated. No real payment credentials are collected. There is no support ticket module.

## Quick Start — Windows

### Prerequisites

- Git
- Docker Desktop
- Windows PowerShell

No local Java, Maven, Node.js, PostgreSQL, OpenSSL, or PowerShell 7
installation is required when using the Docker Compose workflow.

### 1. Clone the repository

```powershell
git clone <repository-url>
cd "NP ComputersJ"

## Start in VS Code

Use the project directory **`C:\Users\ronnn\Documents\Development\Java Dev\NP Computers`**. Install Docker Desktop with Linux containers, VS Code and PowerShell 7. Java 21 and Node 22.12+ are needed only for native development.

Open this folder in VS Code and run:

```powershell
pwsh -File scripts/setup.ps1
docker compose --env-file .env -f infra/compose.yaml up --build
```

Open http://localhost:8080. Create a customer account with a 12–128 character password, log in, complete the profile, shop and check out. Pick Approve or Decline on the payment screen. Read the generated ADMIN credentials from the ignored `.env` file locally; they are never committed. Staff can create products, adjust stock, change user roles, view orders and audit events, and inspect the local mail sink for password reset links. SUPPORT can only read orders; SALES can fulfill them. To test another role, register an account and assign its role through ADMIN.

On macOS or Linux, run `sh scripts/setup.sh` instead. Database startup files and migrations initialize only a new volume. `docker compose --env-file .env -f infra/compose.yaml down` retains data. Removing the `db-data` volume destroys data and requires deliberately reinitializing the database; do not do that for routine restarts. Existing database credentials do not change when `.env` is edited.

## Native development

```powershell
docker compose --env-file .env -f infra/compose.yaml -f infra/compose.local.yaml up -d db
docker compose --env-file .env -f infra/compose.yaml run --rm migrate
pwsh -File scripts/dev-backend.ps1
# in a second terminal
cd frontend
npm ci
npm run dev
```

Frontend: http://localhost:5173. API: http://localhost:8081. OpenAPI JSON: `/v3/api-docs`; Swagger UI: `/swagger-ui/index.html` on the API in local/demo. Vite proxies API requests and the authentication origin is set to the frontend. Configure `JAVA_HOME` to Java 21; the Maven wrapper downloads its pinned distribution on first use.

## Build and test

```powershell
cd backend
./mvnw.cmd verify
# integration tests need an initialized PostgreSQL database and generated keys
./mvnw.cmd -Dnp.integration=true test
cd ../frontend
npm ci
npm test
npm run build
```

Integration test environment: `DB_URL`, `DB_PASSWORD`, `MIGRATION_PASSWORD`, `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`; use an isolated empty test database with roles `np_app` and `np_migrate`. Tests write synthetic data. Do not point tests at a database containing useful data. CI initializes these dependencies and enables the integration suite. See `docs/verification.md` for locally executed checks and limitations.

## Structure

- `backend`: API contracts/controllers, domain entities, Spring Data repositories, application services and security configuration; Flyway SQL.
- `frontend`: accessible customer/store/staff interface; API client holds access tokens only in memory.
- `infra`: private database network, migration job, API and SPA proxy; PostgreSQL data volume.
- `scripts`: local key/secret setup and development startup.
- `docs`: implementation decisions, requirement coverage and operations notes.

## Important implementation decisions

JWT access life is five minutes; every request checks the stored session. Opaque refresh tokens rotate with seven day idle and 30 day absolute limits. Reuse revokes the family, including a racing second refresh. Cookie endpoints enforce CSRF and exact Origin. Browser refresh uses one in-flight promise and the Web Locks API when available; response loss may require login. Production defaults require secure cookies and private signing keys. Passwords use salted PBKDF2, not reversible encryption.

Orders use a customer lock, deterministic product locks, persisted quotes and idempotency keys. Price, stock, shipping and tax are server controlled. Shipping is a demo flat $10 and tax is zero. Successful local simulated checkout commits order/payment/stock/audit together. A decline leaves cart and stock unchanged. A real payment gateway requires reservation and reconciliation work before use.

This implementation uses one role per account (CUSTOMER, ADMIN, SALES or SUPPORT), rather than multiple simultaneous roles. Product stock is held on the product aggregate to share the lock with price and activity checks. The local outbox adapters do no network calls; a real adapter must add leases, external-call retry handling and independent delivery transactions. See `docs/architecture-decisions.md` for these intentional differences from the design proposal.

Compose binds only localhost and is a development/demo distribution. An external launch needs TLS, secure cookies, approved contact/tax/shipping policies, mail delivery, monitoring, retention and tested backup/recovery. Production OpenAPI and the mail sink are disabled by default. No commercial readiness or measured availability target is implied.

## Browser smoke test

With the local frontend and backend running, install the browser once and run from the project root:

```powershell
cd frontend
npx playwright install chromium
cd ..
node scripts/browser-smoke.cjs
```

For Compose, set `BASE_URL=http://localhost:8080` before running. For an installed Chrome, set `PLAYWRIGHT_CHROME_PATH` to its executable path. The test creates a disposable customer and order and writes ignored screenshots/results under `test-results/browser`. It refuses non-local targets.
