# NP Computers

Java 21 / Spring Boot / Maven / React / PostgreSQL enterprise demo. NP Computers is a portfolio storefront for selling new and used computers and new accessories to U.S. individuals and businesses.

Payments and email delivery are simulated. The application does not collect real payment credentials, and there is no support-ticket module.

## Quick Start — Evaluator Workflow

The recommended evaluation workflow uses published Docker images and an isolated Docker Compose project name so a clean evaluation does not reuse an existing NP Computers database volume on the same machine.

### Prerequisites

- Git
- Docker Desktop
- Windows PowerShell, macOS Terminal, or a Linux shell

No local Java, Maven, Node.js, PostgreSQL, OpenSSL, or PowerShell 7 installation is required for the Docker Compose evaluator workflow.

### Windows

From PowerShell:

```powershell
git clone https://github.com/newtoro111/NPComputersJ.git
cd "NPComputersJ"

.\scripts\setup.ps1

docker compose -p npcomputers-eval --env-file ".\.env" -f ".\infra\compose.yaml" config
docker compose -p npcomputers-eval --env-file ".\.env" -f ".\infra\compose.yaml" pull
docker compose -p npcomputers-eval --env-file ".\.env" -f ".\infra\compose.yaml" up
```

The steps above:

1. Clone the source repository.
2. Generate local-only secrets and credentials with the setup script.
3. Validate the Compose configuration.
4. Pull the published NP Computers API and web images plus PostgreSQL and Flyway.
5. Start the complete application using the isolated Compose project name `npcomputers-eval`.

Using `-p npcomputers-eval` is intentional. The Compose file declares a default project name for normal development, but an evaluator should use a separate project name so Docker creates separate networks, containers, and the PostgreSQL volume. This prevents a new `.env` from being paired with an older database volume that contains different PostgreSQL role passwords.

When the services report healthy, open:

[http://localhost:8080](http://localhost:8080)

Normal day-to-day evaluator shutdown should use the following:
```powershell
docker compose -p npcomputers-eval --env-file ".\.env" -f ".\infra\compose.yaml" down
```

The following should be used when you want a fresh evaluator rerun with newly generated credentials:
```powershell
docker compose -p npcomputers-eval --env-file ".\.env" -f ".\infra\compose.yaml" down -v
```


### macOS and Linux

```bash
git clone https://github.com/newtoro111/NPComputersJ.git
cd NPComputersJ

sh scripts/setup.sh

docker compose -p npcomputers-eval --env-file .env -f infra/compose.yaml config
docker compose -p npcomputers-eval --env-file .env -f infra/compose.yaml pull
docker compose -p npcomputers-eval --env-file .env -f infra/compose.yaml up
```

Open:

[http://localhost:8080](http://localhost:8080)

## What Setup Creates

The setup script creates local development files that are excluded from source control:

- `.env`
- `secrets/private.pem`
- `secrets/public.pem`

The `.env` file contains generated local database credentials and the demo administrator credentials.

These files must remain local. They are intentionally excluded from Git.

## Published Docker Images

The evaluator workflow uses these public Docker Hub application images:

- `newtoro1/npcomputers-api:1.0.0`
- `newtoro1/npcomputers-web:1.0.0`

PostgreSQL and Flyway are pulled from their public registries through `infra/compose.yaml`.

The source Dockerfiles remain in `backend` and `frontend` for development, inspection, and rebuilding new image versions.

## First Startup

On first startup, Docker Compose:

1. Starts PostgreSQL.
2. Initializes database roles and permissions.
3. Runs all Flyway migrations.
4. Starts the Spring Boot API.
5. Starts the React/Nginx web application.

The `migrate` container is expected to exit successfully after migrations complete.

## Open the Application

Open:

[http://localhost:8080](http://localhost:8080)

Create a customer account with a 12–128 character password, log in, complete the customer profile, browse the catalog, add products to the cart, and complete checkout.

The payment screen supports simulated **Approve** and **Decline** outcomes.

## Demo Administrator

Read the locally generated administrator credentials from `.env`.

On Windows:

```powershell
Get-Content ".\.env"
```

Look for:

```text
ADMIN_EMAIL=...
ADMIN_PASSWORD=...
```

These values are generated locally and are never committed to Git.

Administrator capabilities include:

- Create and maintain products.
- Adjust inventory.
- Change user roles.
- View customer orders.
- View audit events.
- Inspect the local mail sink for simulated password-reset messages.

Role behavior:

- `CUSTOMER` — customer storefront and account functionality.
- `ADMIN` — administrative functionality.
- `SALES` — order fulfillment functionality.
- `SUPPORT` — read-only order access.

To test another staff role, register an account and assign the desired role while logged in as `ADMIN`.

## Check Container Status

For the evaluator project:

```powershell
docker compose -p npcomputers-eval --env-file ".\.env" -f ".\infra\compose.yaml" ps
```

A healthy environment should show approximately:

- `db` — running and healthy.
- `migrate` — exited successfully with code `0`.
- `api` — running and healthy.
- `web` — running and healthy.

## Stop the Evaluator Environment

Press `Ctrl+C` in the terminal running Docker Compose, or run:

```powershell
docker compose -p npcomputers-eval --env-file ".\.env" -f ".\infra\compose.yaml" down
```

A normal `down` retains the evaluator PostgreSQL data volume.

To deliberately remove only the evaluator environment and its database volume:

```powershell
docker compose -p npcomputers-eval --env-file ".\.env" -f ".\infra\compose.yaml" down -v
```

Because the evaluator uses the separate project name `npcomputers-eval`, this command targets the evaluator resources rather than the normal NP Computers development project.

Do not use `down -v` against the normal development project unless you intentionally want to delete its database.

## Troubleshooting

### `.env` file not found

If Compose reports that `.env` cannot be found, run the setup script before any `docker compose` command:

```powershell
.\scripts\setup.ps1
```

Then verify:

```powershell
Get-Item ".\.env"
Get-ChildItem ".\secrets"
```

You should see `.env`, `private.pem`, and `public.pem`.

### Compose YAML or configuration error

Validate the file before pulling or starting containers:

```powershell
docker compose -p npcomputers-eval --env-file ".\.env" -f ".\infra\compose.yaml" config
```

Do not continue until `config` completes successfully.

### `password authentication failed for user "np_migrate"`

This typically means a PostgreSQL data volume was initialized with different credentials than the current `.env`.

For evaluator testing, always use:

```text
-p npcomputers-eval
```

If the evaluator environment itself was previously initialized with an older `.env` and can be discarded, remove only the evaluator environment and volume:

```powershell
docker compose -p npcomputers-eval --env-file ".\.env" -f ".\infra\compose.yaml" down -v
```

Then rerun the setup sequence. If `.env` already exists and is still the intended evaluator configuration, keep it; the setup script should not be used to replace working credentials unnecessarily.

### API signing-key errors

The API requires the locally generated RSA files:

```text
secrets/private.pem
secrets/public.pem
```

If they are missing, rerun the setup script before starting Compose.

## Build From Source / Native Development

The published-image workflow above is the recommended way to evaluate the application.

Developers who want to modify and run the source locally can use the native-development workflow below.

### Native Development Requirements

- Java 21
- Node.js 22.12+
- Docker Desktop
- Maven wrapper supplied by the repository

PowerShell 7 is not required for the standard Windows setup.

### Start PostgreSQL and Run Migrations

For normal development, use the default Compose project defined by the repository rather than the evaluator project name:

```powershell
docker compose --env-file ".\.env" -f ".\infra\compose.yaml" -f ".\infra\compose.local.yaml" up -d db

docker compose --env-file ".\.env" -f ".\infra\compose.yaml" run --rm migrate
```

### Start the Backend

```powershell
.\scripts\dev-backend.ps1
```

### Start the Frontend

In a second terminal:

```powershell
cd frontend
npm ci
npm run dev
```

Development endpoints:

- Frontend: [http://localhost:5173](http://localhost:5173)
- API: [http://localhost:8081](http://localhost:8081)
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`
- Swagger UI: `http://localhost:8081/swagger-ui/index.html`

Vite proxies API requests during native frontend development, and the authentication origin is set to the frontend.

Configure `JAVA_HOME` to Java 21. The Maven wrapper downloads its pinned Maven distribution on first use.

## Build and Test

### Backend

```powershell
cd backend
.\mvnw.cmd verify
```

Integration tests require an initialized PostgreSQL database and generated RSA keys:

```powershell
.\mvnw.cmd -Dnp.integration=true test
```

### Frontend

```powershell
cd frontend
npm ci
npm test
npm run build
```

The integration-test environment uses:

- `DB_URL`
- `DB_PASSWORD`
- `MIGRATION_PASSWORD`
- `JWT_PRIVATE_KEY`
- `JWT_PUBLIC_KEY`
- `ADMIN_EMAIL`
- `ADMIN_PASSWORD`

Use an isolated, empty test database with the `np_app` and `np_migrate` roles. Integration tests write synthetic data. Do not point tests at a database containing useful data.

CI initializes the required dependencies and enables the integration suite.

See `docs/verification.md` for locally executed checks and known limitations.

## Browser Smoke Test

With the local frontend and backend running, install Chromium for Playwright once:

```powershell
cd frontend
npx playwright install chromium
cd ..
```

Then run from the repository root:

```powershell
node scripts/browser-smoke.cjs
```

For the Docker Compose deployment, set:

```powershell
$env:BASE_URL="http://localhost:8080"
node scripts/browser-smoke.cjs
```

For an installed Chrome browser, set `PLAYWRIGHT_CHROME_PATH` to the Chrome executable path.

The smoke test:

- Creates a disposable customer.
- Creates a disposable order.
- Writes ignored screenshots and results under `test-results/browser`.
- Refuses non-local targets.

## Project Structure

- `backend` — Spring Boot API contracts/controllers, domain entities, Spring Data repositories, application services, security configuration, and Flyway migrations.
- `frontend` — React customer/store/staff interface. Access tokens are held only in memory.
- `infra` — Docker Compose topology, private database network, migration job, API/web services, and PostgreSQL data volume.
- `scripts` — local key/secret setup, development startup, and test utilities.
- `docs` — implementation decisions, requirement coverage, verification, and operations notes.
- `secrets` — locally generated RSA signing keys. The contents are excluded from source control.
- `.env` — locally generated environment credentials. Excluded from source control.

## Container Distribution

The primary Docker Compose configuration uses published application images for the API and web tiers rather than requiring evaluators to compile the project locally.

Evaluation path:

```text
clone repository
      ↓
run setup script
      ↓
validate Compose configuration
      ↓
pull published images
      ↓
start isolated evaluator project
      ↓
open http://localhost:8080
```

The evaluator project name is deliberately separate from the normal development project so that credentials and PostgreSQL volumes cannot accidentally cross between environments.

## Important Implementation Decisions

JWT access-token lifetime is five minutes, and every authenticated request checks the stored server-side session.

Opaque refresh tokens rotate with:

- Seven-day idle expiration.
- Thirty-day absolute expiration.

Refresh-token reuse revokes the token family, including a racing second refresh.

Cookie-backed authentication endpoints enforce CSRF protection and exact Origin validation. Browser refresh uses one in-flight promise and the Web Locks API when available. Response loss may require the user to log in again.

Production defaults require secure cookies and private signing keys.

Passwords use salted PBKDF2 and are never stored using reversible encryption.

Orders use:

- A customer lock.
- Deterministic product locks.
- Persisted checkout quotes.
- Idempotency keys.
- Server-controlled price, inventory, shipping, and tax calculations.

For this demo:

- Shipping is a flat `$10`.
- Tax is `$0`.
- Payment processing is simulated.

A successful simulated checkout commits the order, payment, stock movement, and audit data together. A declined payment leaves the cart and stock unchanged.

A real payment gateway would require reservation, reconciliation, provider-token handling, and additional failure-recovery design before production use.

The application intentionally uses one role per account:

- `CUSTOMER`
- `ADMIN`
- `SALES`
- `SUPPORT`

Product stock is maintained on the product aggregate so price, activity status, and inventory can share the same locking boundary.

The local outbox adapters make no network calls. A real adapter would require leases, independent delivery transactions, external-call retry handling, and operational monitoring.

See `docs/architecture-decisions.md` for intentional differences between the implementation and the original architecture proposal.

## Security and Secrets

The following files are local-only and must not be committed:

```text
.env
secrets/private.pem
secrets/public.pem
```

The repository `.gitignore` excludes these files.

Before committing, developers can verify this with:

```powershell
git check-ignore -v .env
git check-ignore -v secrets/private.pem
git check-ignore -v secrets/public.pem
```

The RSA private key is mounted into the API container at runtime. It is not embedded in the application image.

## Database Migrations and Demo Data

Database changes are managed with Flyway migrations under:

```text
backend/src/main/resources/db/migration
```

The migration job runs before the API starts.

The repository includes demo product data for computers, laptops, tablets, accessories, storage, networking, monitors, peripherals, and related catalog items.

Do not manually modify an existing Flyway migration after it has been applied. Add a new versioned migration for schema or seed-data changes.

## Operational Scope

Docker Compose binds the application only to localhost and is intended as a development/demo distribution.

An external production launch would require additional work, including:

- TLS termination.
- Secure-cookie configuration.
- Approved contact, tax, and shipping policies.
- Real mail delivery.
- Production payment integration.
- Monitoring and alerting.
- Data-retention controls.
- Backup and recovery procedures.
- Security review and secrets management.

Production OpenAPI and the local mail sink are disabled by default.

No commercial readiness or measured availability target is implied.

If you are still with me here, please consider liking and subscribing... JK.
