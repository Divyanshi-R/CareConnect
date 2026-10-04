# CareConnect: Patient-Provider EHR

CareConnect is an internship project for a web-based Electronic Health Record
(EHR) system. It brings together patient records, clinical documentation,
computerized provider order entry (CPOE), medication and prescription
management, appointments, and role-specific patient, doctor, and administrator
portals.

> **Project scope:** CareConnect is an academic/internship project. It is not
> presented as production-ready healthcare software and does not claim HIPAA or
> GDPR compliance.

## Technology stack

| Area | Technologies |
| --- | --- |
| Frontend | React, Vite, JavaScript, React Router, Axios, Tailwind CSS |
| Backend | Java 21, Spring Boot, Spring Web, Spring Data JPA, Hibernate, Spring Security, JWT, Bean Validation, Maven |
| Database | MySQL |
| Backend test database | H2 in-memory database |

## Architecture

```text
React
  ↓
REST JSON APIs
  ↓
Spring Boot Controllers
  ↓
Services
  ↓
Repositories
  ↓
JPA / Hibernate
  ↓
MySQL
```

The frontend talks to the backend through Axios service modules. The backend
uses controllers for HTTP concerns, services for application logic and access
checks, and Spring Data repositories for persistence.

## Roles and modules

The application defines three roles:

- **ADMIN** — user, doctor, patient, department, and appointment administration.
- **DOCTOR** — appointments, patient charts, encounters, diagnoses, notes,
  prescriptions, and clinical orders.
- **PATIENT** — access to their own profile, appointments, medical records,
  prescriptions, medications, and clinical orders.

Major modules include authentication, RBAC, user/patient/doctor/department
management, appointments, clinical encounters and documentation, diagnoses,
medications, prescriptions, CPOE, patient portal, doctor dashboard, admin
dashboard, audit logging, and security hardening.

## Frontend routes

The React Router configuration protects each portal by role.

| Portal | Routes |
| --- | --- |
| Patient | `/patient/dashboard`, `/patient/profile`, `/patient/appointments`, `/patient/medical-records`, `/patient/prescriptions`, `/patient/medications`, `/patient/orders` |
| Doctor | `/doctor/dashboard`, `/doctor/appointments`, `/doctor/patients`, `/doctor/patients/:id`, `/doctor/encounters/:id`, `/doctor/prescriptions`, `/doctor/orders` |
| Admin | `/admin/dashboard`, `/admin/users`, `/admin/doctors`, `/admin/patients`, `/admin/departments`, `/admin/appointments` |
| Shared | `/login`, `/unauthorized` |

## Project structure

```text
CareConnect/
├── backend/
│   ├── src/main/java/com/careconnect/
│   │   ├── config/          # Security, CORS, password encoder, admin bootstrap
│   │   ├── controller/      # REST controllers
│   │   ├── dto/             # Request and response DTOs
│   │   ├── entity/          # JPA entities and enums
│   │   ├── exception/       # Domain exceptions and API error handling
│   │   ├── repository/      # Spring Data repositories
│   │   ├── security/        # JWT filter, principal, user-details service
│   │   └── service/         # Authentication and business services
│   ├── src/main/resources/  # application.yml and database resources
│   ├── src/test/java/       # Backend integration and repository tests
│   ├── src/test/resources/  # H2 test configuration
│   └── pom.xml
└── frontend/
    ├── src/
    │   ├── components/      # Shared and portal UI components
    │   ├── context/         # Authentication and portal context
    │   ├── hooks/           # Shared asynchronous resource hook
    │   ├── layouts/         # Patient, doctor, and admin layouts
    │   ├── pages/           # Login and portal pages
    │   ├── routes/          # React Router configuration
    │   └── services/        # Axios client and API service modules
    ├── .env.example
    ├── package.json
    └── vite.config.js
```

## Prerequisites

- Java 21
- Maven
- MySQL
- Node.js and npm

The backend uses MySQL with the production configuration. Automated backend
tests use the H2 in-memory database configured under `backend/src/test/resources`.

## Database and backend configuration

The production backend configuration requires these environment variables:

| Variable | Purpose |
| --- | --- |
| `DB_URL` | JDBC URL for the MySQL database |
| `DB_USERNAME` | MySQL account name |
| `DB_PASSWORD` | MySQL account password |
| `JWT_SECRET` | Secret used to sign JWTs; configure at least 32 UTF-8 bytes |

Optional variables include:

| Variable | Default / purpose |
| --- | --- |
| `SERVER_PORT` | Backend HTTP port; defaults to `8081` |
| `JWT_EXPIRATION_MS` | JWT lifetime in milliseconds; defaults to `86400000` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated exact origins; defaults to local frontend origins |
| `HIBERNATE_DDL_AUTO` | Hibernate schema mode; defaults to `update` |
| `SHOW_SQL` | Hibernate SQL output; defaults to `false` |
| `DB_INIT_FAIL_TIMEOUT` | Hikari initialization fail timeout |
| `ADMIN_INITIAL_EMAIL` | Optional email for initial ADMIN bootstrap |
| `ADMIN_INITIAL_PASSWORD` | Optional initial ADMIN password; it is BCrypt-hashed before storage |

Create a MySQL database and a least-privilege database account before starting
the backend. Supply the real values through the environment or your deployment
secret manager; do not commit credentials or signing keys.

PowerShell example (replace every placeholder locally; do not use these
placeholder strings as actual credentials):

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/careconnect?createDatabaseIfNotExist=true&serverTimezone=UTC"
$env:DB_USERNAME = "<your-mysql-user>"
$env:DB_PASSWORD = "<your-mysql-password>"
$env:JWT_SECRET = "<at-least-32-random-characters>"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:5173"
```

For deployed environments, configure the JDBC URL and database transport
security to match the deployment and use a secret manager for credentials.
The optional admin bootstrap values should only be set when initial admin
creation is intended.

## Run the backend

From the repository root, set the required environment variables as described
above, then run:

```powershell
mvn.cmd -f backend\pom.xml spring-boot:run
```

The default backend port is `8081`; the health check is `GET /api/health`.

## Run the frontend

The frontend reads its API base URL from `VITE_API_BASE_URL`. The default is
`http://localhost:8081/api`. For local setup, copy the example environment file:

```powershell
Copy-Item frontend\.env.example frontend\.env
```

Then install dependencies and start Vite:

```powershell
Push-Location frontend
npm.cmd install
npm.cmd run dev
Pop-Location
```

Vite prints the local development URL when it starts; the usual development
port is `5173`.

## API overview

The backend exposes REST JSON endpoints under `/api`. The following paths are
defined by the current controllers:

| Area | Implemented paths |
| --- | --- |
| Authentication | `POST /api/auth/register`, `POST /api/auth/login` |
| Current user and administration | `GET /api/users/me`, `GET /api/users` |
| Patient records and patient-specific APIs | `GET /api/patients/me`, `GET /api/patients/me/medications`, `GET /api/patients/me/notes`, `GET /api/patients`, `GET /api/patients/{id}`, `POST /api/patients`, `PUT /api/patients/{id}` |
| Doctors | `GET /api/doctors`, `GET /api/doctors?departmentId={id}`, `GET /api/doctors/{id}`, `POST /api/doctors`, `PUT /api/doctors/{id}`, `DELETE /api/doctors/{id}` |
| Departments | `GET /api/departments`, `GET /api/departments/{id}`, `POST /api/departments`, `PUT /api/departments/{id}`, `DELETE /api/departments/{id}` |
| Appointments | `GET /api/appointments`, `GET /api/appointments/{id}`, `GET /api/appointments/patient/{patientId}`, `GET /api/appointments/doctor/{doctorId}`, `POST /api/appointments`, `PUT /api/appointments/{id}`, `DELETE /api/appointments/{id}` |
| Encounters | `POST /api/encounters`, `GET /api/encounters/{id}`, `GET /api/encounters/patient/{patientId}`, `GET /api/encounters/doctor/{doctorId}`, `PUT /api/encounters/{id}` |
| Clinical notes | `POST /api/encounters/{encounterId}/notes`, `GET /api/encounters/{encounterId}/notes`, `PUT /api/notes/{id}`, `DELETE /api/notes/{id}` |
| Diagnoses | `POST /api/encounters/{encounterId}/diagnoses`, `GET /api/encounters/{encounterId}/diagnoses`, `GET /api/patients/{patientId}/diagnoses`, `PUT /api/diagnoses/{id}` |
| Medications | `GET /api/medications`, `GET /api/medications/{id}`, `POST /api/medications`, `PUT /api/medications/{id}`, `DELETE /api/medications/{id}` |
| Prescriptions | `POST /api/prescriptions`, `GET /api/prescriptions/{id}`, `GET /api/patients/{patientId}/prescriptions`, `GET /api/doctors/{doctorId}/prescriptions`, `PUT /api/prescriptions/{id}` |
| Clinical orders | `POST /api/orders`, `GET /api/orders/{id}`, `GET /api/patients/{patientId}/orders`, `GET /api/doctors/{doctorId}/orders`, `PUT /api/orders/{id}`, `POST /api/orders/{id}/cancel` |

Authorization depends on role and, for patient/doctor chart data, resource
ownership. The API's role restrictions are enforced by Spring Security in
addition to the frontend route guards. Audit logging is internal service
functionality; no audit-log retrieval endpoint is currently exposed.

## Authentication and security

- Login uses Spring Security authentication and returns a signed JWT.
- Protected frontend API calls use the shared Axios client, which sends the
  stored JWT as a `Bearer` authorization header.
- The frontend route guard redirects unauthenticated users to `/login` and
  users with the wrong role to `/unauthorized`.
- Backend endpoint and service checks enforce RBAC and patient/doctor ownership;
  frontend navigation is not treated as an authorization boundary.
- Passwords are hashed with BCrypt. Privileged role registration is restricted
  to authenticated administrators.
- Request DTO validation uses Bean Validation where defined.
- Audit records cover login outcomes and selected patient/clinical create or
  update events; passwords and tokens are not audit fields.
- Error responses avoid returning unexpected exception details.
- JWT signing and database credentials are externally configured. CORS uses an
  explicit configurable origin allowlist.

## Tests and build

Run the complete backend test suite:

```powershell
mvn.cmd -f backend\pom.xml clean test
```

The current backend suite contains **69 tests**, covering authentication,
authorization, patient/doctor/department/appointment management, encounters,
clinical notes, diagnoses, medications, prescriptions, clinical orders,
patient APIs, audit events, and security cases. The most recent full run passed
with **69 tests, 0 failures, and 0 errors**.

Build the frontend for production:

```powershell
Push-Location frontend
npm.cmd run build
Pop-Location
```

The current frontend package has a production build script but does not define
a separate automated frontend test runner. Route and role guard definitions,
API token handling, loading/error/empty-state components, and portal logout
behavior are implemented in the source; the production build verifies
compilation and bundling. A local browser smoke check verified the
unauthenticated redirect, patient denial from doctor/admin routes, doctor
denial from admin routes, doctor/admin access to their respective portals, and
logout redirect with client token removal. The smoke check used an ephemeral
intercepted current-user response; it did not persist mock records. No backend
server was available for live frontend-to-API workflow checks, so data
loading/empty/error behavior was source-reviewed rather than exercised against
a live API.

## Project limitations and future enhancements

CareConnect is an internship-scale application. It does not claim healthcare
regulatory compliance or production readiness. Further work before any
real-world deployment would require, among other things, independent security
and privacy review, operational controls, deployment-specific database/TLS and
secret management, monitoring, backup/recovery planning, and validation against
applicable legal and organizational requirements.

Potential future enhancements include:

- Add automated frontend component and route tests for authenticated and
  unauthorized role scenarios.
- Add API documentation and a versioned OpenAPI specification.
- Add a carefully authorized and tested audit review workflow if an audit
  viewer is required.
- Expand audit coverage and retention controls based on approved requirements.
- Add production-grade deployment configuration, observability, and backup
  procedures.
- Continue improving accessibility, responsive behavior, and patient-facing
  workflows.
