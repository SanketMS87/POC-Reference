# Mail Application

A Spring Boot–based REST service that sends templated email notifications for deals and policies
(e.g. underwriting/decline notifications). It runs as an executable Spring Boot jar on **embedded
Tomcat**, configured entirely through Spring profiles and `application-<profile>.properties`.

## Tech Stack

- **Java 17**
- **Spring Boot 3.5.14** (Web on embedded Tomcat 10.1, Security, Data JPA, Validation, Mail, OAuth2 Client/Resource Server, Actuator)
- **springdoc-openapi** (Swagger UI) — scoped to `com.rukamini.mail.controller`
- **Lombok**, **Apache POI**, **Gson**, **Apache Commons** (lang3, io, beanutils)
- **PostgreSQL JDBC** (`org.postgresql:postgresql` 42.7.x) for the `cwf` datasource (Hikari pool)
- **Azure Key Vault** (Spring Cloud Azure) for secrets
- Packaged as an executable **uber jar** with **embedded Tomcat** (`java -jar`)
- Build tool: **Maven** (`pom.xml`), with Spotless (Google Java Format / AOSP style) enforced on
  `validate`, and JaCoCo for coverage

## Project Structure

```
src/main/java/com/rukamini/mail/
├── configuration/   # Property-backed config beans (AppConfig), HTTP client, cache, OpenAPI
├── controller/      # REST controllers (APIController, MailHelperController)
├── dao/             # Business objects (MailBO, MailHelperBO)
├── exception/        # Global exception handling
├── filter/           # Servlet filters
├── integration/      # Outbound REST calls to downstream services (POM, Deal/UWB)
├── jpa/              # JPA entities/repositories
├── oauth/            # OAuth2 client config & token service for outbound calls
└── SpringSecurityConfig.java
```

## Key REST Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/health` | Health check |
| `POST` | `/api/initiate/{dealNum}/{policyNum}/{templateName}` | Send a mail notification for a deal/policy using the given template |
| `POST` | `/api/initiate/{dealOrPolicyNum}/{templateName}` | Send a decline mail notification for a deal or policy |

Interactive API documentation (Swagger UI) is available at `/swagger-ui.html` once the
application is running.

## Configuration Model

All configuration is property-based and bound to typed classes with `@ConfigurationProperties`
(`AppProperties` for `app.*`, `SecurityProperties` for `app.security.*`); `AppConfig` exposes them as
beans.

| File | Purpose |
|---|---|
| `application.properties` | Common settings: server (port `9080`, context path `/mail`), datasource pool, SMTP relay, Azure Key Vault, JWT validation, springdoc |
| `application-train1/2/3.properties`, `application-prod.properties` | Per-environment DB URL, downstream URLs, OAuth2 scopes, JWT audiences; Key Vault enabled |
| `application-local.properties` | Local defaults (localhost DB/SMTP/stubs); security is permit-all, Key Vault off |

Select an environment with `--spring.profiles.active=<profile>` or `SPRING_PROFILES_ACTIVE`.
When neither is supplied, the application defaults to the `local` profile, which uses console
logging and does not connect to Logstash.

### Secrets (Azure Key Vault)

Secrets are never stored in the files. Each secret property uses a placeholder that prefers a Key
Vault secret and falls back to an environment variable:

| Key Vault secret | Env fallback | Used for |
|---|---|---|
| `cwf-db-username` / `cwf-db-password` | `DB_CWF_USER` / `DB_CWF_PASSWORD` | PostgreSQL `cwf` datasource |
| `mail-tech-user` / `mail-tech-pass` | `MAIL_TECH_USER` / `MAIL_TECH_PASS` | SMTP relay credentials |
| `swisscom-oauth-client` / `swisscom-oauth-secret` | `SWISSCOM_OAUTH_CLIENT` / `SWISSCOM_OAUTH_SECRET` | OAuth2 client credentials for outbound calls |

Key Vault is configured in `application.properties`
(`spring.cloud.azure.keyvault.secret.property-sources[0].*`):

| Variable | Purpose |
|---|---|
| `AZURE_KEYVAULT_ENDPOINT` | Vault URI, e.g. `https://<name>.vault.azure.net/` |
| `AZURE_KEYVAULT_ENABLED` | `true` by default for `train*`/`prod`, `false` otherwise |

Authentication uses `DefaultAzureCredential` (managed identity in Azure; `AZURE_CLIENT_ID` /
`AZURE_TENANT_ID` / `AZURE_CLIENT_SECRET` or the Azure CLI elsewhere). The identity needs `get` and
`list` permission on secrets.

Other variables:

| Variable | Used for |
|---|---|
| `DB_CWF_URL` | PostgreSQL JDBC URL (required for `train*`/`prod`), e.g. `jdbc:postgresql://host:5432/cwf?sslmode=require` |
| `LOGSTASH_URL` | `host:port` of the logstash TCP appender (all profiles except `local`) |
| `PORT` | Overrides the HTTP port (default `9080`) |

Inbound authentication validates Entra ID bearer tokens in Spring Security (JWT resource server,
`app.security.jwt.*`). Spring extracts authorities from the token's `roles` claim
(`oauth2ResourceServer()`). Set `app.security.grant-all-roles-to-authenticated=true` to grant every
role to any authenticated caller instead.

## Running Locally

### 1. Prerequisites

- JDK 17
- Maven 3.8+
- Network access to any downstream services you want to hit (SMTP relay, POM, Deal/UWB APIs), or
  point `application-local.properties` at stubs/mocks.

### 2. Adjust local settings (optional)

`src/main/resources/application-local.properties` already contains working localhost defaults.
Override individual values with environment variables (for example `DB_CWF_URL`,
`DB_CWF_USER`, `DB_CWF_PASSWORD`, `MAIL_TECH_USER`, `SWISSCOM_OAUTH_CLIENT`) or command-line
arguments, e.g.:

```powershell
$env:DB_CWF_USER = "your-db-user"
$env:DB_CWF_PASSWORD = "your-db-password"
```

> Do not commit real secrets to any `application-*.properties` file.

### 3. Run locally

```powershell
mvn spring-boot:run
```

The `local` profile is selected by default. The application starts on port `9080` with context path `/mail` (e.g.
`http://localhost:9080/mail/api/health`) and exposes the endpoints listed above, plus Swagger UI.

## Building

```powershell
mvn clean package
```

This produces an executable Spring Boot uber jar (`target/mail-<version>.jar`). Run it with:

```powershell
java -jar target\mail-0.3.0-SNAPSHOT.jar
```

### Code style & quality

- **Spotless** enforces Google Java Format (AOSP style) and runs automatically during the
  `validate` phase — run `mvn spotless:apply` to auto-format before committing.
- **JaCoCo** collects unit test coverage into `target/jacoco.exec`.

## Deployment (embedded Tomcat)

Deploy `target/mail-<version>.jar` to any Java 17 runtime and select the environment with
`SPRING_PROFILES_ACTIVE` (`train1`, `train2`, `train3`, `prod`), supplying the secrets listed in
*Configuration Model* as environment variables.

Downstream calls from the corporate network go through the proxy (`gate.swissre.com:8080`). The
JVM proxy settings cannot be expressed in `application.properties`; pass them as JVM options:

```
-Duser.timezone=Europe/Zurich
-Dhttp.proxyHost=gate.swissre.com -Dhttp.proxyPort=8080
-Dhttps.proxyHost=gate.swissre.com -Dhttps.proxyPort=8080
-Dhttp.nonProxyHosts=localhost|127.0.0.1|*.corp.gwpnet.com|*.swissreapps.com|*.swissreapps-np.com|fda-ite.swissre.com|fda.swissre.com|*.swissreapps-ee.com|*.csc.com|*.exh-dev1.swissre.com|.exh-ite.swissre.com|*exh-ite.swissre.com|.exh.swissre.com|*exh.swissre.com|10.*|cip.int.swissre.com:443|*.mon.swissre.com
```

## OAuth2 Outbound Calls

Outbound calls to downstream services (POM, Deal/UWB APIs) use OAuth2 Bearer tokens obtained via
`Oauth2TokenService` / `OAuth2AuthorizedClientManager`. See
[`docs/oauth2-migration-registry.md`](docs/oauth2-migration-registry.md) for the current
migration status of each outbound endpoint. The SMTP relay is explicitly excluded from OAuth2
migration since it uses SMTP AUTH credentials instead.
