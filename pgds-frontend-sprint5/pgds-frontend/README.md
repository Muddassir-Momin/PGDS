# PGDS Frontend - Sprint 5

React 18 + Vite + Bootstrap 5 + Chart.js + Axios + React Router. Matches the GrainGuard AI dashboard layout.

## Run
    npm install
    npm run dev          # http://localhost:3000
    npm run build        # production bundle in dist/

The backend must be running on http://localhost:8080 (change with VITE_API_URL, see .env.example).
The backend allows CORS from http://localhost:3000, so keep that port or set CORS_ORIGIN on the backend.

## Screens by role
| Role | Screens |
|---|---|
| SUPER_ADMIN | Dashboard, Ration Cards, Complaints, AI Alerts, AI Forecasts, Users & Tools (create staff, demo data) |
| GOVT_OFFICER | Dashboard, Ration Cards, Complaints, AI Alerts, AI Forecasts |
| WAREHOUSE_MANAGER | Warehouse Stock (receive, transfer to shop, damage, ledger) |
| FPS_DEALER | Distribute Grain (check entitlement, issue, printable receipt), Shop Stock |
| BENEFICIARY | My Ration (card, remaining entitlement, history, shops), Complaints |
| AUDITOR | AI Alerts (read-only) |

Demo login buttons appear on the sign-in page in dev mode only (password Pgds@12345).

## Docker (optional)
    docker build -t pgds-frontend .
    docker run -p 3000:80 pgds-frontend
