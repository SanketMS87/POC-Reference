# Mail Application - Assessment and Status Report

Date: 2026-10-14 | Branch: `feature/POC-code-quality` | Runtime: OpenJDK 17.0.20.1 (Microsoft), Maven 3.10.0

## 1. Executive Summary

| Area | Status | Notes |
|---|---|---|
| Build (`mvn clean verify`, Java 17) | PASS | Executable uber jar (`mail-0.3.0-SNAPSHOT.jar`) produced |
| Unit tests | PASS | 152 run, 0 failures, 0 errors, 0 skipped |
| Spotless format check | PASS | Google Java Format (AOSP) |
| Code coverage (JaCoCo) | 70.6% instruction / 74.7% branch | Gaps listed in section 4 |
| Run standalone, `local` profile | PASS | `java -jar target\mail-0.3.0-SNAPSHOT.jar --spring.profiles.active=local`; `/api/health` 200 |
| Run on embedded Tomcat, `train1` profile | PASS (startup + security) | With dummy OAuth2 secrets: `/mail/api/health` 200, protected endpoint 401 without/with invalid bearer token |
| Run against real PostgreSQL/SMTP/Entra/Key Vault | NOT VERIFIED | No PostgreSQL, SMTP relay, Key Vault or valid tokens available locally |
| PostgreSQL DB connectivity | NOT AVAILABLE | `localhost:5432` not running; DB-backed calls return 500 |

Overall: the code builds and its unit tests pass on Java 17. Liberty/JNDI configuration has been converted to embedded Tomcat with property-based configuration (see section 8). End-to-end behaviour (DB, SMTP, downstream OAuth2 services, real JWT validation) is unverified.

## 2. Build and Test Results

- The Oracle driver (`ojdbc8 12.2.0.1-pached`, not resolvable from public repositories) has been replaced by `org.postgresql:postgresql` 42.7.14, so the build no longer needs any local stand-in jar.
- Baseline: Java 17, Spring Boot 3.5.14 (Spring Framework 6.2, Spring Security 6.5, Hibernate 6.6, embedded Tomcat 10.1). Jackson pinned to 2.22.2 and logback to 1.5.38 (CVE overrides); springdoc 2.8.17.
- 19 test source files; `Oauth2TokenServiceIT` (integration) is not picked up by surefire's default naming rules.

## 3. Runtime Run (local profile, Java 17)

Command: `mvn spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments=--server.port=8080`

### Startup issues found and handled

| # | Issue | Cause | Action |
|---|---|---|---|
| 1 | `Unable to start web server: no ServletWebServerFactory` | `pom.xml` excluded `spring-boot-starter-tomcat` (WAR packaging for Liberty) | Resolved by the move to an uber jar: Tomcat exclusion removed |
| 2 | `NotificationBO.renewalOverviewUrl` required a single bean, 13 found | `RemoteConfig` defined a `renewalOverviewUrl` bean, `LocalConfig` did not, so the `local` profile could not start | Resolved by consolidating both into a single property-backed `AppConfig` (`app.urls.renewal-overview`) |

After both, the app started in 6.6 s: `Tomcat started on port 8080`, 3 JPA repositories found.

### Endpoint smoke test

| Request | Result | Comment |
|---|---|---|
| `GET /api/health` | 200 `Mail Application Working` | OK |
| `GET /v3/api-docs` | 200 | OpenAPI 3.1 doc served |
| `GET /swagger-ui.html` | redirect (not followed) | Expected |
| `GET /actuator/health` | 503 | DB health indicator down (PostgreSQL unreachable) |
| `GET /api/nonexistent` | 404 | Handled by `GlobalExceptionHandler` |
| `POST /api/initiate/D1/P1/T1` (no body/content type) | 500 | `HttpMediaTypeNotSupportedException` surfaced as 500, should be 415 |

Startup warnings: deprecated Hibernate settings (`hibernate.temp.use_jdbc_metadata_defaults`, explicit `hibernate.dialect`), deprecated `@Temporal` on `TUser` and `TNotificationPreferences`, `spring.jpa.open-in-view` enabled by default, bean post-processor eligibility warning for `RestApplication`.

## 4. Coverage Analysis (JaCoCo)

| Package | Instruction | Branch |
|---|---|---|
| `dao` | 96% | 89% |
| `user` | 100% | 94% |
| `exception` | 92% | 88% |
| `util` | 89% | 100% |
| `oauth` | 84% | 80% |
| `integration` | 57% | 30% |
| `configuration` | 27% | n/a |
| `controller` | 25% | n/a |
| `jpa.entity` | 8% | 0% |
| `filter` | 0% | 0% |
| **Total** | **70.6%** | **74.7%** |

Classes with 0% coverage: `ResponseCorsFilter`, `APIController`, `MailHelperController`, `LocalConfig` (+ `LocalCwfDbConfig`), `HttpClientConfig`, `SpringFoxConfig`, `MailConstants`, `TNotificationPreferences`, `BeanValidatorPluginsConfiguration`. Low: `DealServiceClient` 3%, `TUser` 8%, `RemoteConfig` 33%.

## 5. Assessment Findings

| Sev | Finding | Recommendation |
|---|---|---|
| High | Local and remote bean sets could drift apart (this caused the missing `renewalOverviewUrl` bean) | Fixed structurally: one `AppConfig` for all profiles; `AppConfigTest` covers it |
| High | Oracle driver `ojdbc8:12.2.0.1-pached` was not resolvable from public repositories | Resolved: migrated to PostgreSQL JDBC 42.7.14 |
| Medium | Controllers (`APIController`, `MailHelperController`) and `ResponseCorsFilter` have 0% coverage; HTTP error mapping (500 vs 415) is not tested | Add MockMvc slice tests, including content-type and validation errors |
| Medium | Unsupported media type returns 500 | Map `HttpMediaTypeNotSupportedException` to 415 in `GlobalExceptionHandler` |
| Medium | CORS regex `.*localhost` (no leading dot) also matches hosts such as `evillocalhost`, combined with `Allow-Credentials: true` and reflected origin | Use `localhost` exactly or `(.*\.)?localhost`; also `Access-Control-Request-Method` is a request header, not a response header |
| Low | `.github/agents/*.agent.md` (OpenLiberty/JNDI-oriented Copilot agent definitions) and `docs/oauth2-migration-registry.md` still describe the Liberty/JNDI setup; `wlp/lib/dd-java-agent.jar` (untracked Datadog agent) remains | Review and remove if no longer needed |
| Low | `commons-io` 2.15.1 and `httpclient` 4.5.14 (HttpClient 4.x is end-of-life) are pinned alongside HttpClient 5 | Remove or upgrade; confirm 4.x is still needed |
| Low | `local` profile sets `permitAll()` on every request | Acceptable for dev, ensure it can never be activated in train/prod (profiles/*.properties) |
| Low | Naming leftovers: `SpringFoxConfig` and `springfox.bean.validators...` package although the app uses springdoc | Rename or remove |
| Low | Deprecated Hibernate settings and `@Temporal` | Remove `hibernate.dialect`, replace `use_jdbc_metadata_defaults`, drop `@Temporal` on `java.sql`/`java.time` types |
| Low | `application.properties` contains corrupted characters in comments (encoding) | Re-save as UTF-8 |
| Low | `springdoc` UI and `/v3/api-docs` enabled by default (warned at startup) | Disable in prod via `springdoc.*.enabled=false` |
| Info | README listed Gson/POI/commons-beanutils and `idam` datasources that are no longer in the pom/code | Update README |

## 6. Not Verified / Next Steps

1. Deploy and run in a train environment with the real secrets (`DB_CWF_*`, `MAIL_TECH_*`, `SWISSCOM_OAUTH_*`) and corporate proxy JVM options.
2. Run against a real PostgreSQL schema (migrated user, template and notification-preference tables and the `SEQ_T_NOTIFICATION_PREF` sequence) and SMTP relay; verify `POST /api/initiate/...` end to end.
3. Verify OAuth2 token flow against Entra ID and the downstream POM/Deal APIs (`Oauth2TokenServiceIT` exists but needs credentials).
4. Run a dependency vulnerability scan (OWASP Dependency-Check or Sonatype); the pom comments cite CVE fixes that were not re-validated here.
5. Verify Azure Key Vault end to end against a real vault (secret names `cwf-db-username`, `cwf-db-password`, `mail-tech-user`, `mail-tech-pass`, `swisscom-oauth-client`, `swisscom-oauth-secret`; identity needs get + list).

## 7. Changes Made During This Run

- Liberty to embedded Tomcat conversion: see section 8.
- This report.
- Local only (outside the repo): logs in `C:\Users\v-sankesingh\Downloads\POC\` (`build*.log`, `run*.log`, `jar-*.log`, `kv-*`).

## 8. Liberty to Embedded Tomcat Conversion

| Liberty / JNDI item | Replacement |
|---|---|
| `RemoteConfig` (JNDI lookups) and `LocalConfig` | Single `AppConfig` driven by typed `@ConfigurationProperties` classes `AppProperties` (`app.*`) and `SecurityProperties` (`app.security.*`); same bean names |
| `jdbc/cwf` Liberty datasource | PostgreSQL (`org.postgresql:postgresql` 42.7.14) via a Hikari pool bound from `app.datasource.cwf.*` (pool values taken from the Liberty `connectionManager`); Hibernate `PostgreSQLDialect`; resource-local JPA transactions instead of the WebSphere JTA platform; native query `ROWNUM=1` changed to `LIMIT 1` |
| `jndiEntry` values (env, URLs, schema, scopes, sender address) | `app.*` properties in `application.properties` and `application-{train1,train2,train3,prod}.properties` |
| Vault/DBaaS bound secrets (`${cloud.services...}`) | Azure Key Vault (`spring-cloud-azure-starter-keyvault-secrets` 5.25.0) configured in `application.properties`; each secret property is a placeholder such as `${cwf-db-password:${DB_CWF_PASSWORD:}}` so it falls back to an environment variable when Key Vault is off |
| `mpJwt` (issuer, JWKS, audiences, `roles` claim) | Spring Security JWT resource server (`app.security.jwt.*`) replacing `.jee()` container authentication; roles come from the JWT `roles` claim, and `app.security.grant-all-roles-to-authenticated=true` reproduces `ALL_AUTHENTICATED_USERS` |
| `httpEndpoint` port 9080, context root `/mail`, gzip | `server.port=${PORT:9080}`, `server.servlet.context-path=/mail`, `server.compression.*` |
| `logging.url` jndiEntry | `logging.url=${LOGSTASH_URL:localhost:5000}` |
| `web.xml`, `SpringBootServletInitializer`, `jakarta.servlet-api` (provided) | Removed; `ResponseCorsFilter` is registered as a `@Component` |
| `jvm.options` proxy and timezone settings | Not expressible as properties; documented as JVM options in the README |

Behaviour changes to be aware of:

- All URLs now carry the `/mail` context path (for example `/mail/api/health`).
- Liberty effectively granted every role to any authenticated user. Roles are now read from the JWT `roles` claim (`grant-all-roles-to-authenticated=false`); tokens must carry the `roles` claim, or set the property to `true` to restore the old behaviour.
- `liberty_message` log type renamed to `mail_message` in `logback-spring.xml`; update any log queries that filter on it.
- Removed: `wlp/`, `manifest.yml`, `pom-deploy.xml`, `assembly.xml`, `profiles/`, the springfox stub class and the assembly/antrun Maven plugins. The removed `wlp/prod/server.xml` contained hard-coded Service Bus access keys; those keys should be rotated.
- Without secrets set, a non-local profile fails fast at startup (empty OAuth2 client id).
