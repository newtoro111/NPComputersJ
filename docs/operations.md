# Operations

Configuration: base defaults are safe for secure deployment; local/demo enables synthetic catalog and developer mail sink and disables Secure cookies only for local HTTP. Test uses isolated PostgreSQL. Credentials and PEM keys live outside source control. Production mode is the base configuration (no demo/local profile); set APP_ORIGIN to the HTTPS origin and inject keys/credentials. Do not expose bootstrap credentials or mail-sink storage.

Readiness `/actuator/health/readiness` checks PostgreSQL; liveness `/actuator/health/liveness` checks process state. Compose gates API startup on successful Flyway migration; normal web traffic cannot access actuator endpoints. Structured JSON logs include bounded correlation IDs, method, route, status and duration; never log bodies or tokens. General logs are distinct from append-only business audit. Audit grants deny runtime UPDATE/DELETE; database administrators remain privileged.

Back up with PostgreSQL `pg_dump` using a controlled backup credential; encrypt copies and rehearse restore into an isolated database. Preserve signing keys through restarts to avoid unexpectedly rejecting sessions. Rotate key pairs with an explicit logout-all maintenance window in this v1 single-key implementation; overlapping multi-key rotation is a future hardening task. Revoke sessions after suspected compromise.

Only apply new Flyway scripts. Never edit a deployed migration or enable Hibernate automatic schema updates. Back up before migrations. Use expand/contract database changes so the prior image can run against the new schema; rollback is an image rollback only when schema compatible. Compose is not HA. RPO/RTO and latency/availability targets must be measured and accepted before claiming compliance.

For an uncertain checkout response, retry the same payload/key; do not generate a fresh key unless the customer changes the submission. For stock conflicts, refresh cart and quote. Investigate outbox backlog using a controlled DB connection; local delivery is transactional and rolls back on failure. External delivery must not be added inside that transaction.

Retention jobs are not enabled: approve legal/business retention policies before external launch. Password reset links expire after 15 minutes and are single-use. Consumed refresh digests are retained for replay detection; archive/purge expired families through controlled maintenance only after absolute expiry.
