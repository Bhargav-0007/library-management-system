# Library Management SaaS

A multi-tenant library management app built with **Spring Boot**. Any library can sign up and get its own
private workspace with a catalog, members, loans and a staff team. Each workspace's data is isolated
from every other workspace.

Billing and deployment are intentionally out of scope for now.

---

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4 (Web MVC, Data JPA, Validation) |
| Security | Spring Security + stateless JWT (jjwt 0.12.6), BCrypt passwords |
| Database | H2 (file-based, dev) |
| Frontend | Dependency-free single-page app served from `src/main/resources/static` |
| Build | Maven |

---

## Features

- **Self-serve signup**: creating an account creates a new organization (tenant) with you as its owner.
- **Tenant isolation**: books, members, loans and staff all belong to one organization. Every query is
  scoped to the signed-in user's organization, so another tenant's IDs return `404`.
- **Team & roles**:
  - `OWNER` created the workspace and cannot be removed.
  - `ADMIN` manages staff and settings.
  - `LIBRARIAN` handles books, members and loans.
- **Catalog**: multi-copy books with live availability and search by title, author or ISBN. ISBNs are
  unique per library.
- **Members**: add, edit and delete members. Emails are unique per library.
- **Loans**: issue and return books. Due dates come from the library's loan duration. Per-member loan
  limits are enforced, overdue loans are tracked, and copies are row-locked so concurrent borrows
  can't overbook.
- **Dashboard**: titles, copies, members, active loans, overdue count and the loans due next.
- **Workspace settings**: library name, loan duration and the per-member loan limit.

---

## Running locally

```bash
cd library
./mvnw spring-boot:run
```

Open **http://localhost:8088**, click **Create a library workspace**, and you're in.

To see tenant isolation, sign up a second workspace in a private window. It starts empty.

The H2 console (dev only) is at `http://localhost:8088/h2-console` (JDBC URL `jdbc:h2:file:./data/library-saas`,
user `sa`, empty password). Disable it with `spring.h2.console.enabled=false` outside development.

### Configuration

| Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | dev-only key | **Must be overridden** outside development. Base64-encoded, at least 256 bits. |
| `JWT_EXPIRATION_MS` | `86400000` | Token lifetime (24 h). |

### Tests

```bash
./mvnw test
```

`MultiTenancyIntegrationTests` covers signup/login, cross-tenant isolation, copy availability and
loan rules, and role-based access to team and settings.

---

## API

All endpoints except signup and login need `Authorization: Bearer <token>`. Errors are JSON:
`{"error": "message"}`. Validation errors also include a `fields` map.

### Auth

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/signup` | `{organizationName, fullName, email, password}`. Creates an org and its owner, then returns a session. |
| POST | `/api/auth/login` | `{email, password}`. Returns `{token, user, organization}`. |
| GET | `/api/auth/me` | Current user and organization. |

### Library data (any staff role)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/dashboard` | Workspace stats and loans due next |
| GET | `/api/books?q=` | List or search books (includes `availableCopies`) |
| POST / PUT | `/api/books`, `/api/books/{id}` | `{title, author, isbn, copies}` |
| GET / DELETE | `/api/books/{id}` | Get or delete a book. Delete is blocked while copies are on loan. |
| GET / POST | `/api/members` | List or add members, `{name, email}` |
| GET / PUT / DELETE | `/api/members/{id}` | Get, update or delete a member |
| GET | `/api/loans?status=active\|overdue\|returned\|all` | List loans |
| POST | `/api/loans` | `{bookId, memberId}`. Issue a book. |
| POST | `/api/loans/{id}/return` | Return a book |
| GET | `/api/loans/overdue` | Overdue loans |
| GET | `/api/organization` | Workspace settings |

### Administration (OWNER / ADMIN)

| Method | Endpoint | Description |
|---|---|---|
| PUT | `/api/organization` | `{name, loanDurationDays, maxActiveLoansPerMember}` |
| GET | `/api/team` | List staff |
| POST | `/api/team` | `{fullName, email, password, role}`. Role is `ADMIN` or `LIBRARIAN`. |
| DELETE | `/api/team/{id}` | Remove staff. Only the owner can remove admins. |

### Example

```bash
TOKEN=$(curl -s -X POST http://localhost:8088/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"organizationName":"Riverside Library","fullName":"Sam","email":"sam@example.com","password":"password123"}' \
  | sed -E 's/.*"token":"([^"]+)".*/\1/')

curl -X POST http://localhost:8088/api/books -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Dune","author":"Frank Herbert","isbn":"9780441013593","copies":2}'
```

---

## Project structure

```
library/src/main/java/com/mini_project/library/
├── controller/   # REST controllers + GlobalExceptionHandler
├── dto/          # Validated request records and response records
├── entity/       # Organization (tenant), AppUser (staff), Role, Book, Member, Loan
├── exception/    # ApiException (carries the HTTP status)
├── repository/   # Spring Data repositories; every query takes an organization id
├── security/     # JWT service, auth filter, security config, AuthUser principal
└── service/      # Business logic, always scoped to the caller's organization
library/src/main/resources/static/   # Web app (index.html, app.js, styles.css)
```

## Not yet included

- Billing, plans and usage limits
- Deployment (Docker, CI/CD, production database profile)
- Email invitations and password reset (staff currently get a temporary password from an admin)
