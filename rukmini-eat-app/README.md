# Rukmini Eat App

Simple Swiggy/Zomato-style ordering app.

- **Backend:** Java 17, Spring Boot 4.0.8, Spring Security (JWT, HS256), Spring Data JPA, H2
- **Frontend:** Angular 20 (standalone components, signals)

## Run

Run these commands from the `rukmini-eat-app` directory (from the Git repository
root, first run `Set-Location .\rukmini-eat-app`).

Build and test the backend:

```
mvn clean install
```

1. Database: H2 runs in memory; no Docker or external database is needed.
   Data is reset when the backend process stops.

2. Backend (port 8080, context path `/rukmini-eat`). For local development use the
   `local` profile, which seeds the demo data and enables Swagger UI:

   ```
   mvn -f .\rukmini-eat-backend\pom.xml spring-boot:run "-Dspring-boot.run.profiles=local"
   ```

   Without the `local` profile the application starts empty (no demo users or
   restaurants, no Swagger UI, no H2 console) and **requires** a `JWT_SECRET`
   environment variable of at least 32 bytes; it refuses to start otherwise.

   ```
   $env:JWT_SECRET = "<random string of 32+ characters>"
   mvn -f .\rukmini-eat-backend\pom.xml spring-boot:run
   ```

3. Frontend (port 4200, needs Node 20.19+):

   ```
   npm.cmd --prefix .\rukmini-eat-frontend install
   npm.cmd --prefix .\rukmini-eat-frontend start
   ```

Open http://localhost:4200.

Demo logins (local only, seeded by `DataSeeder`):

| Role | Email | Password |
|---|---|---|
| Customer | demo@rukmini.com | Demo@123 |
| Admin | admin@rukmini.com | Admin@123 |

## Screens

1. **Login / Create account** - tabbed form with validation and server error messages.
2. **Restaurant dashboard** - greeting, stats, cuisine strip, search, filters (pure veg, rating, fast), sorting.
3. **Restaurant menu** - category-grouped dishes, veg/non-veg markers, add to cart.
4. **Cart** - quantity steppers, free-delivery progress, bill breakdown, place order.
5. **My orders** - order history with status and totals.

## API

Backend origin and context: **http://localhost:8080/rukmini-eat**.
Every API path below is relative to that URL.

### Swagger / OpenAPI

| Resource | URL |
|---|---|
| Swagger UI | http://localhost:8080/rukmini-eat/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/rukmini-eat/v3/api-docs |
| OpenAPI YAML | http://localhost:8080/rukmini-eat/v3/api-docs.yaml |

Documentation is publicly readable; application APIs remain secured.
Swagger generates request/response schemas and validation constraints from the
backend DTOs. The Swagger description also lists the frontend routes and the
backend operations each screen calls.

A static copy is saved at [docs/openapi.yaml](docs/openapi.yaml) for import into
other tools. It is a snapshot; regenerate it after changing endpoints with the
backend running, then rebuild the HTML pages:

```
Invoke-WebRequest http://localhost:8080/rukmini-eat/v3/api-docs.yaml -OutFile .\docs\openapi.yaml
.\docs\build-html.ps1
```

Static HTML documentation, built from `openapi.yaml` (open directly in a browser,
no server needed):

| Page | Purpose |
|---|---|
| [docs/index.html](docs/index.html) | Overview: base URLs, backend endpoints, frontend routes, authentication, errors |
| [docs/swagger.html](docs/swagger.html) | Offline Swagger reference for every endpoint and schema (read-only; use the live Swagger UI to execute requests) |

`docs/assets/` holds the bundled Swagger UI 5.32.2 files (Apache License 2.0) and the
generated `openapi-spec.js`. Do not edit `openapi-spec.js`; rerun `build-html.ps1`.

Using the live Swagger UI:

1. Open Swagger UI and expand **Authentication**.
2. Use **Try it out** on `POST /api/auth/login` with a demo login from above,
   or register a new customer with `POST /api/auth/register`.
3. Copy the `token` from the response.
4. Click **Authorize** and paste only the token (without the `Bearer ` prefix).
5. Execute the restaurant and order operations. Admin operations require the
   admin login; registration always creates a customer.

Swagger UI and the OpenAPI endpoints are available with the `local` profile only.
To enable them elsewhere, set `SPRINGDOC_API_DOCS_ENABLED=true` and
`SPRINGDOC_SWAGGER_UI_ENABLED=true`; they are disabled by default.

### All REST endpoints

All 9 application operations are documented in Swagger under these tags.
Paths are relative to `http://localhost:8080/rukmini-eat`.

| Tag | Method | Path | Access | Request | Responses |
|---|---|---|---|---|---|
| Authentication | POST | `/api/auth/register` | Public | `{name, email, password}` | 201 token + user; 400; 409 duplicate email; 429 |
| Authentication | POST | `/api/auth/login` | Public | `{email, password}` | 200 token + user; 400; 401; 429 |
| Restaurants | GET | `/api/restaurants` | Authenticated | Query: `q`, `cuisine`, `vegOnly`, `topRated`, `fast`, `sort` | 200 summaries; 401 |
| Restaurants | GET | `/api/restaurants/{id}` | Authenticated | Path: `id` | 200 restaurant + menu; 401; 404 |
| Orders | POST | `/api/orders` | Authenticated | `{items: [{menuItemId, quantity}]}` | 201 order with server-calculated totals; 400; 401 |
| Orders | GET | `/api/orders` | Authenticated | None | 200 own orders, newest first; 401 |
| Administration | POST | `/api/admin/restaurants` | `ADMIN` | `{name, cuisines, rating, etaMinutes, costForTwo, pureVeg, emoji?, description?}` | 201 summary; 400; 401; 403 |
| Administration | POST | `/api/admin/restaurants/{id}/menu-items` | `ADMIN` | `{name, category, price, veg, bestseller, emoji?}` | 201 menu item; 400; 401; 403; 404 |
| Provider integrations | GET | `/api/integrations/providers` | Authenticated | None | 200 static readiness status; 401 |

Swagger also shows each request/response schema, validation limits, and examples.

### Other backend URLs

| URL | Purpose | Access |
|---|---|---|
| `/swagger-ui/index.html` | Swagger UI (`/swagger-ui.html` redirects to it) | Public, `local` profile only |
| `/v3/api-docs` and `/v3/api-docs.yaml` | OpenAPI JSON / YAML | Public, `local` profile only |
| `/h2-console` | Enabled only in the `local` profile, but currently returns 401 because `SecurityConfig` requires a JWT for it; not usable from a browser unless explicitly permitted for local development | Blocked |

Restaurant search accepts optional `q` (name, cuisine, or dish), `cuisine`
(substring), `vegOnly`, `topRated` (rating >= 4.3), `fast` (ETA <= 25 minutes),
and `sort` (`relevance`, `rating`, `eta`, `costLow`, `costHigh`).
Boolean filters default to `false`; the default sort is `relevance`.

Order request example (replace `1` with an ID from a restaurant's `menu`):

```json
{
  "items": [{ "menuItemId": 1, "quantity": 2 }]
}
```

Common errors: `400` invalid input, `401` missing/invalid token or bad login,
`403` customer attempting admin operations, `404` unknown restaurant, `409`
duplicate registration, and `429` too many login/registration attempts (see
`Retry-After`). Application validation/business errors return
`{"message":"..."}`; security-layer errors may have an empty body.

### Frontend routes and API connections

The Angular frontend runs at **http://localhost:4200**. It is not a REST server;
its routes render screens that call the backend.

| Frontend route | Screen / behavior | Backend calls |
|---|---|---|
| `/login` | Login and registration tabs; guest-only | `POST /api/auth/login`, `POST /api/auth/register` |
| `/restaurants` | Dashboard, search, filters, sorting | `GET /api/restaurants` |
| `/restaurants/:id` | Restaurant details and menu | `GET /api/restaurants/{id}` |
| `/cart` | In-browser cart; authenticated checkout | `POST /api/orders` |
| `/orders` | Own order history | `GET /api/orders` |
| `/` or unknown route | Redirect to `/restaurants`, then `/login` if signed out | None directly |

All routes except `/login` require a signed-in user. There are no dedicated
admin or provider-integration screens; those APIs can be exercised in Swagger.
Cart contents are held in frontend memory, not persisted by a separate cart API.

The shared [frontend environment](rukmini-eat-frontend/src/environments/environment.ts)
sets `apiUrl` to `http://localhost:8080/rukmini-eat/api`. Both the authentication
service and restaurant/order API client use this value; the HTTP interceptor
attaches the JWT to protected requests. If the backend host, port, or context
changes, update this value and rebuild. For a different frontend origin, also
update backend `app.cors.allowed-origins` (default `http://localhost:4200`).

Send the token as `Authorization: Bearer <jwt>`.

## Tests

### Backend

JUnit 5 with an in-memory H2 database; no external services. Run from `rukmini-eat-app`:

```
mvn -f .\rukmini-eat-backend\pom.xml verify
```

`verify` runs all tests, writes the **JaCoCo coverage report** to
`rukmini-eat-backend/target/site/jacoco/index.html` and **fails the build if line
coverage drops below 90%** (override with `-Djacoco.min.line.coverage=0.8`). Use `test`
instead of `verify` to skip the coverage gate. Current result: 89 tests, about 99% line coverage.

| Test class | Covers |
|---|---|
| `AuthApiIntegrationTest` | Registration/login validation, duplicate emails, error messages, demo roles, JWT claims |
| `RestaurantApiIntegrationTest` | Search, every filter and sort option, boundaries, detail/menu, 404 and bad IDs |
| `OrderApiIntegrationTest` | Pricing totals, line merging, quantity limits, invalid requests, one-restaurant rule, per-customer history |
| `AdminApiIntegrationTest` | Creating restaurants and menu items, validation, 403/401 for non-admins |
| `SecurityApiIntegrationTest` | Expired, forged, tampered and wrong-issuer tokens, auth schemes, CORS, 404/405/415 handling, stateless responses |
| `RateLimitIntegrationTest`, `AuthRateLimiterTest` | Login/registration throttling (429, `Retry-After`, CORS headers, window reset) |
| `NonLocalProfileIntegrationTest`, `SecurityConfigTest`, `DataSeederIntegrationTest` | No demo data, Swagger or H2 console outside `local`; JWT secret rules; idempotent seeding |
| `OpenApiIntegrationTest` | Swagger/OpenAPI document, public docs, full register-to-order flow |
| `PricingTest`, `JwtServiceTest`, `GlobalExceptionHandlerTest`, `ProviderIntegrationControllerTest` | Unit tests |

Integration test classes share cached application contexts and create uniquely named
data, so they can run in any order.

### Frontend

Jest with `jest-preset-angular` and jsdom (no browser needed). Run from `rukmini-eat-app`:

```
npm.cmd --prefix .\rukmini-eat-frontend test
npm.cmd --prefix .\rukmini-eat-frontend run test:ci
```

`test:ci` also writes coverage to `rukmini-eat-frontend/coverage/index.html` and fails
below the thresholds in `jest.config.js` (80% lines/statements/functions, 70% branches).
Current result: 91 tests, about 98% line coverage. Specs cover `AuthService`, the route
guards and routes, the auth interceptor, `CartService`, `ApiService`, the app shell and
the login, dashboard, restaurant detail, cart and orders pages.

## Security

- **Profiles:** demo accounts, sample restaurants, Swagger UI and the H2 console exist
  only with the `local` profile.
- **JWT:** HS256, 60-minute lifetime, issuer `rukmini-eat` is validated. `JWT_SECRET`
  (32+ bytes) is mandatory outside `local`; the public development key is rejected there.
- **Throttling:** `POST /api/auth/login` and `/register` allow 20 attempts per client address
  per endpoint per 60 seconds, then return 429. Configure with
  `app.security.rate-limit.max-attempts` (0 disables) and `app.security.rate-limit.window-seconds`.
  Counters are in memory and keyed by the connection address, so behind a reverse proxy
  enable `server.forward-headers-strategy` and use a shared store for multiple instances.
- **Dependencies:** Spring Boot 4.0.8; `npm audit` reports 0 vulnerabilities. To scan the
  backend against the NVD with OWASP dependency-check (opt-in; needs a free NVD API key,
  otherwise the first download is extremely slow):

  ```
  $env:NVD_API_KEY = "<your key>"
  mvn -f .\rukmini-eat-backend\pom.xml -Psecurity verify
  ```

  The build fails on CVSS 7 or higher; the report is written to
  `rukmini-eat-backend/target/dependency-check-report.html`. This scan was not run
  as part of this change because no key was available.
- The frontend uses an npm `overrides` entry for `js-yaml` so that the Jest toolchain
  installs a patched version.

## Zomato / Swiggy integration preparation

Neither provider is connected. The standalone prototype continues using mock
restaurants and demo checkout. No outbound provider requests are made.

`GET /api/integrations/providers` is authenticated using the existing bearer-token
flow. It reports `AWAITING_APPROVED_API_CONTRACT`, `liveData: false`, and
`realOrdering: false` for both providers. This is an implementation-readiness
status, not an external provider health check. The standalone prototype displays
a static disclosure; it does not call this endpoint or use its demo-login flag as
a backend JWT.

Reserved Spring configuration reads these optional server environment variables:

| Provider | Base URL | Credential |
|---|---|---|
| Zomato | `ZOMATO_API_BASE_URL` | `ZOMATO_API_KEY` |
| Swiggy | `SWIGGY_API_BASE_URL` | `SWIGGY_API_KEY` |

These values are not consumed by an HTTP client yet. Setting them does not
activate an integration. Do not put credentials in frontend JavaScript, browser
storage, committed configuration, or shared chat. Configure secrets in the
backend process environment or a deployment secret store. Do not assume that
either provider authenticates using an API key: the approved contract must
determine the eventual authentication mechanism.

Before implementation, obtain approved partner access and documentation covering
permitted restaurant discovery, menu endpoints, authentication, rate limits,
attribution and data retention. Zomato's
[developer documentation](https://www.zomato.com/developer/integration/docs/overview/)
describes POS integration; this does not establish access to general consumer
restaurant discovery. Confirm the supported product directly with each provider.
No undocumented consumer endpoints or scraping are used.

Next, implement and test provider-specific server adapters against the approved
response schemas, map data into the application's restaurant/menu contracts,
and connect the prototype through an authenticated backend flow. Real provider
ordering is a separate integration requiring explicit approval, documented
checkout/payment flows, and end-to-end tests; existing local orders are not sent
to Zomato or Swiggy.

## Backend implementation notes

- Order prices are recalculated on the server from database prices; the cart total in the UI is only an estimate.
- Pricing: delivery ₹40 (free from ₹399), GST 5%, platform fee ₹5.
- The current database configuration uses H2 (override with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; an external database also needs its JDBC driver). `JWT_SECRET` (32+ bytes) is required outside the `local` profile.
- The token is kept in `localStorage` for simplicity; use an HttpOnly cookie flow for production.
- Schema is managed with `ddl-auto=update`; use Flyway or Liquibase for production.
