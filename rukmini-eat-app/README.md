# Rukmini Eat App

Simple Swiggy/Zomato-style ordering app.

- **Backend:** Java 17, Spring Boot 4.0.0, Spring Security (JWT, HS256), Spring Data JPA, PostgreSQL
- **Frontend:** Angular 20 (standalone components, signals)

## Run

Build the backend from the repository root:

```
mvn clean install
```

1. Database (PostgreSQL 16 via Docker):

   ```
   docker compose up -d
   ```

2. Backend (port 8080). From the repository root, run:

   ```
   mvn -f .\rukmini-eat-backend\pom.xml spring-boot:run
   ```

   To run without PostgreSQL or Docker, use the in-memory local profile:

   ```
   mvn -f .\rukmini-eat-backend\pom.xml spring-boot:run "-Dspring-boot.run.profiles=local"
   ```

   Tables are created and demo data is seeded on first start.

3. Frontend (port 4200, needs Node 20.19+). From the repository root, run:

   ```
   npm --prefix .\rukmini-eat-frontend install
   npm --prefix .\rukmini-eat-frontend start
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

| Method | Path | Access |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login` | Public |
| GET | `/api/restaurants?q&cuisine&vegOnly&topRated&fast&sort` | Authenticated |
| GET | `/api/restaurants/{id}` | Authenticated |
| POST | `/api/orders` | Authenticated |
| GET | `/api/orders` | Authenticated (own orders) |
| POST | `/api/admin/restaurants`, `/api/admin/restaurants/{id}/menu-items` | `ADMIN` |

Send the token as `Authorization: Bearer <jwt>`.

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
- Config is read from environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (32+ bytes). Set a real `JWT_SECRET` outside local development.
- The token is kept in `localStorage` for simplicity; use an HttpOnly cookie flow for production.
- Schema is managed with `ddl-auto=update`; use Flyway or Liquibase for production.
