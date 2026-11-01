# HORA Watch Store

A watch e-commerce application with an English React storefront and a Java REST API.
Customers browse automatic watches and smartwatches, manage a shopping cart and place
Cash on Delivery orders. Administrators manage products, orders and payments.

## Features

- **Customers:** product search, type/price filters, registration/login, cart quantities,
  checkout, order history and purchase-time prices.
- **Administrators:** product creation/editing, stock and availability, soft discontinuation,
  delivery statuses, cancellation with stock restoration, and COD payment confirmation.
- **Security:** Argon2id password hashing, HttpOnly sessions, CSRF/origin checks,
  server-side roles and ownership, prepared statements, and transactional checkout.

## Stack and architecture

Java 17+, Maven 3.9+, PostgreSQL 14+, JDBC, Gson, Password4j and Java HttpServer.
The frontend uses React, React Router, Vite, JavaScript/JSX and plain CSS.
Use Node.js 22.12+ (or 20.19+) and npm, matching `frontend/package.json`.

```text
React -> REST API -> Handler -> Service -> DAO -> PostgreSQL
```

The API boundary authenticates and authorizes requests. Handlers accept DTOs, services
apply business rules and DAOs execute SQL. Entities and DTOs remain separate. Checkout
creates the order, purchase-time items and unpaid COD payment, deducts stock and clears
the cart in one transaction. Eligible cancellation restores stock in one transaction.

| OOP principle | Actual example |
| --- | --- |
| Abstraction | Abstract `BaseEntity` supplies identity and creation time; `PaymentService` defines the payment operation. |
| Encapsulation | `Watch` validates private stock fields; `Cart.getItems()` returns a defensive copy. |
| Inheritance | `AutomaticWatch` and `SmartWatch` extend `Watch`, which extends `BaseEntity`. |
| Polymorphism | `DtoMapper.watch(Watch)` calls the subtype override of `getWarrantyInfo()`; `CODPaymentService` implements `PaymentService`. |

## Database setup

Use a **new, empty database**. From the repository root, replace `app_user` with your
local PostgreSQL role; the tools prompt for your own database password when needed.
Use a role with permission to create databases, or ask an administrator to create the
database with your role as owner.

```sh
createdb -h localhost -p 5432 -U app_user watch_store
psql -X -h localhost -p 5432 -U app_user -d watch_store -v ON_ERROR_STOP=1 -f database/schema.sql
psql -X -h localhost -p 5432 -U app_user -d watch_store -v ON_ERROR_STOP=1 -f database/seed.sql
```

`schema.sql` reproduces the existing `app` schema: eight tables, generated IDs, foreign
keys, uniqueness rules, checks and `NUMERIC(12,0)` amounts. It initializes a database;
it is not a migration and must not be applied over an existing installation.
`seed.sql` adds fictional categories/products without overwriting them on repeat runs.
It contains no users, passwords, orders or private records. Missing product images use
the bundled illustrations.

Register customer accounts through the UI. To enable an administrator, register your own
account, have the database owner promote that account, then log in again:

```sql
UPDATE app.users SET role = 'ADMIN' WHERE email = 'your-address@example.com';
```

There is no default administrator or shared password.

## Backend configuration and startup

Set these environment variables in the terminal that starts Java:

| Variable | Purpose / default |
| --- | --- |
| `DB_URL` | Set your JDBC URL explicitly. Default: `jdbc:postgresql://localhost:5433/watch_store`. |
| `DB_USER` | PostgreSQL role; default `postgres`. |
| `DB_PASSWORD` | Required; your local database password. Never commit it. |
| `API_HOST` | Bind address; default `127.0.0.1`. |
| `PORT` | API port; default `8080`. |
| `API_ORIGIN` | Exact API origin; default `http://localhost:8080`. |
| `SESSION_TTL_SECONDS` | Default `1800`; allowed range 1–86400. |

For example, in PowerShell, enter the password interactively:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/watch_store'
$env:DB_USER = 'app_user'
$credential = Get-Credential -UserName $env:DB_USER -Message 'Local PostgreSQL credentials'
$env:DB_PASSWORD = $credential.GetNetworkCredential().Password
Remove-Variable credential
```

From the repository root, build and copy runtime dependencies:

```sh
mvn clean package
mvn dependency:copy-dependencies -DincludeScope=runtime
```

Start the REST API in Windows PowerShell:

```powershell
java -cp "target/classes;target/dependency/*" com.mycompany.watch_store.ApiServer
```

On macOS/Linux, use `:` as the classpath separator:

```sh
java -cp "target/classes:target/dependency/*" com.mycompany.watch_store.ApiServer
```

Run `ApiServer`, not `java -jar`: the JAR manifest still targets the original console
`Main`. Maven has no automated test classes; package verifies compilation and packaging.

## Frontend startup

In another terminal:

```sh
cd frontend
npm ci
npm run dev
```

Open **http://localhost:5173**. Keep Java at its default local origin and port: Vite
proxies `/api` to `127.0.0.1:8080` while preserving origin and CSRF checks. The browser
handles the session cookie; session IDs are never stored in localStorage. Prices use
English number formatting in VND.

```sh
npm run build
npm run preview
```

Preview uses the same port and proxy; stop dev before starting preview. `dist/` still
requires the Java API. Production hosting is not configured. Sessions are in memory
and end when Java restarts. Google Fonts uses the internet, with local fallback fonts.

## Repository contents

- `src/main/java/`: handlers, services, DAOs, entities, DTOs and backend configuration.
- `frontend/`: React source, Vite configuration, package/lock files and public assets.
- `database/`: reproducible schema and safe sample catalog.
- `pom.xml`: backend build and pinned dependencies.

Install dependencies locally. Demos, development notes, tools, IDE settings, generated
outputs and local credentials are excluded from the tracked project.
