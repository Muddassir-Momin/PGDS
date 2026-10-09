# PGDS Backend - Sprints 1-5

AI-Powered Public Grain Distribution System. Java 21, Spring Boot 3.3, MySQL 8, JWT.

## Run
Option A - everything in Docker:
    docker compose up --build

Option B - local:
    docker compose up mysql -d
    mvn spring-boot:run

## Try it
    curl -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
         -d '{"username":"admin","password":"Pgds@12345"}'
    curl localhost:8080/api/users/me -H "Authorization: Bearer <token>"

Demo users (password Pgds@12345): admin, officer, warehouse, dealer, auditor, beneficiary.
Set SEED_DEMO=false and a real JWT_SECRET in production.

## Sprint plan
| Sprint | Scope |
|---|---|
| 1 | Project setup, Docker, entities, JWT + RBAC (done) |
| 2 | Beneficiary and ration card CRUD, entitlement calculation, warehouse/FPS ledger APIs (done) |
| 3 | Distribution workflow with validation + receipts, complaints, officer dashboard APIs (done) |
| 4 | AI service: anomaly detection, demand prediction, stock-exhaustion forecast, complaint classifier (done) |
| 5 | React app: login, role dashboards (GrainGuard layout), Chart.js (done, see pgds-frontend) |
| 6 | Analytics/reports, testing, hardening, deployment, documentation |

## Sprint 2 API summary
| Method + path | Roles | Purpose |
|---|---|---|
| POST/GET /api/admin/warehouses, /api/admin/fps | SUPER_ADMIN | Master data |
| POST/GET/PUT /api/officer/ration-cards | ADMIN, OFFICER | Card CRUD; response includes monthly entitlement |
| PATCH /api/officer/ration-cards/{id}/eligibility?eligible=false | ADMIN, OFFICER | Deactivate / reactivate a card |
| POST/GET/DELETE /api/officer/beneficiaries | ADMIN, OFFICER | Members; blocks duplicates and over-capacity cards |
| GET /api/beneficiary/me, /api/beneficiary/shops | BENEFICIARY | Own card, entitlement, family, shops in district |
| GET /api/warehouse/{id}/stock, /transactions | ADMIN, OFFICER, AUDITOR, WH MANAGER (own) | Balances and ledger |
| POST /api/warehouse/{id}/receipts, /damage, /transfers | ADMIN, WH MANAGER (own) | Ledger entries; transfer writes OUT + IN atomically |
| GET /api/fps/{id}/stock, /transactions | ADMIN, OFFICER, AUDITOR, FPS DEALER (own) | Shop balances and ledger |
| POST /api/fps/{id}/damage | ADMIN, FPS DEALER (own) | Report damaged stock |

Entitlement (configurable in application.yml): AAY 35 kg per family, PHH 5 kg per person, split by `rice-share`.
Link a beneficiary login to a profile by passing `linkUsername` when adding the beneficiary
(e.g. register a user, then officer adds them with that username).

## Sprint 3 API summary
| Method + path | Roles | Purpose |
|---|---|---|
| GET /api/fps/{id}/cards/{cardNo}/balance | ADMIN, FPS DEALER (own shop) | Entitled / issued / remaining this month |
| POST /api/fps/{id}/distributions | ADMIN, FPS DEALER (own shop) | Issue grain; body `{"cardNumber":"MH-PUN-000001","items":[{"grainType":"RICE","quantityKg":5}]}` returns a receipt |
| GET /api/fps/{id}/distributions | ADMIN, OFFICER, AUDITOR, DEALER (own) | Shop distribution history (paged) |
| GET /api/beneficiary/entitlement, /distributions | BENEFICIARY | Remaining balance and receipt history |
| POST/GET /api/beneficiary/complaints | BENEFICIARY | Submit (auto-classified) and track |
| GET/PATCH /api/officer/complaints | ADMIN, OFFICER | Triage queue; override category/priority/department, set status |
| GET /api/officer/dashboard/summary, stock-by-grain, trend, shops, districts, top-shops | ADMIN, OFFICER | Data for the GrainGuard dashboard cards, donut, line chart, district and shop tables |

Distribution rules: card eligible and assigned to the shop, quantity within this month's remaining entitlement,
shop stock sufficient. The shop row is locked during issue so concurrent requests cannot overdraw stock.
Complaint classification is rule-based now; it sits behind `ComplaintClassifier` so Sprint 4 can swap in an ML model.

Upgrading from Sprint 2: the distributions table changed (receipt number is no longer unique). Reset the dev database:
`docker compose down -v && docker compose up --build`.

## Sprint 4: AI service
| Method + path | Roles | Purpose |
|---|---|---|
| POST /api/admin/ai/demo-history?fpsId=1 | SUPER_ADMIN | Generates ~5 months of synthetic history + injected anomalies (disable with AI_DEMO_ENDPOINT=false) |
| POST /api/officer/ai/run | ADMIN, OFFICER | Run all AI checks now (also runs nightly at 02:00 IST) |
| GET /api/officer/ai/alerts?status=OPEN | ADMIN, OFFICER | AI alerts (auditors: /api/audit/ai/alerts) |
| PATCH /api/officer/ai/alerts/{id} | ADMIN, OFFICER | Review: `{"status":"RESOLVED","note":"..."}` |
| GET /api/officer/ai/stock-forecast?type=FPS or WAREHOUSE | ADMIN, OFFICER | Days until each grain runs out |
| GET /api/officer/ai/demand | ADMIN, OFFICER | Next-month demand per shop and grain, with shortfall vs stock |
| POST /api/officer/ai/classify | ADMIN, OFFICER | Try the complaint classifier: `{"text":"..."}` |
| GET /api/officer/ai/model-info | ADMIN, OFFICER | Classifier algorithm, training size, cross-validation accuracy |

Models
- Fraud / anomaly: custom Isolation Forest (Liu et al. 2008) on 4 features per issue (quantity/entitlement, issues per card per month, hour of day,
  shop issues per hour) plus hard rules (off-hours, >= 25 issues per shop-hour).
- Demand: least-squares trend blended with recent mean, optional seasonal index, capped by entitlement requirement.
- Stock exhaustion: balance / average daily outflow over 30 days. CRITICAL <= 3 days, WARNING <= 7, WATCH <= 14.
- Complaints: TF-IDF + Multinomial Naive Bayes (Weka), trained at startup from src/main/resources/ai/complaints_training.csv,
  with rule-based fallback. Add more rows to the CSV to improve accuracy.

Demo flow: login as admin -> POST /api/admin/ai/demo-history -> login as officer -> POST /api/officer/ai/run -> GET /api/officer/ai/alerts.
Reset the dev database first (`docker compose down -v`) because Sprint 4 adds the ai_alerts table and receipts changed in Sprint 3.

## Sprint 5 backend additions (for the frontend)
- GET /api/warehouse/{id}/shops - shops served by the warehouse (transfer form)
- GET /api/officer/fps - shop picker for the ration card form




## How to Run the Application Locally

### Prerequisites

Ensure the following software is installed:

* Java JDK 21
* Apache Maven 3.10.0
* Node.js and npm
* MySQL 8.0

### 1. Start the MySQL Database

Ensure the MySQL service is running and the database credentials are configured correctly.

On Windows, open PowerShell as Administrator if necessary and run:

```powershell
Start-Service MySQL80
```

If MySQL is already running, continue to the next step.

### 2. Start the Backend (Spring Boot)

Open PowerShell and navigate to the backend directory:

```powershell
cd "C:\Users\Admin\Desktop\PGDS\pgds-backend-sprint5\pgds-backend"
```

Configure Java and Maven:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:MAVEN_HOME = "C:\Tools\apache-maven-3.10.0"
$env:Path = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:Path"
```

Configure the database connection:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/pgds?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_USER = "root"
$securePassword = Read-Host "Enter MySQL root password" -AsSecureString
$env:DB_PASS = [System.Net.NetworkCredential]::new("", $securePassword).Password
$env:JWT_SECRET = "pgds-local-development-secret-key-2026"
```

Start the backend:

```powershell
mvn spring-boot:run
```

Wait until Spring Boot reports that the application has started successfully. Keep this terminal open.

Backend URL: `http://localhost:8080`

### 3. Start the Frontend (React + Vite)

Open a second PowerShell window and navigate to the frontend directory:

```powershell
cd "C:\Users\Admin\Desktop\PGDS\pgds-frontend-sprint5\pgds-frontend"
```

Install dependencies if this is the first run:

```powershell
npm install
```

Start the frontend:

```powershell
npm run dev
```

Frontend URL: `http://localhost:3000`

### 4. Log In to the Application

Open `http://localhost:3000` in your browser and sign in using the demo credentials configured for the project.

### Troubleshooting

* **`mvn` is not recognized:** Verify that Java and Maven are installed and that their `bin` directories are included in the PATH.
* **Network Error:** Confirm that the backend is running on port 8080 and that the frontend API URL and CORS configuration are correct.
* **Database connection error:** Verify that MySQL is running and that the database username and password are correct.

**Note:** Update the local directory paths to match your computer. Never commit database passwords, production secrets, or private credentials to the repository.
