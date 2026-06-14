# SmartSales — Frontend Angular

Interface de gestion des commerciaux et du planning de travail (Angular 21, standalone).

## Lancement rapide

Démarrer les trois services dans des terminaux séparés :

```bash
# 1. Backend Spring Boot (port 8080)
cd backend
mvn spring-boot:run

# 2. Service IA Python (port 8000)
cd ai-service
pip install -r requirements.txt
uvicorn main:app --reload --port 8000

# 3. Frontend Angular (port 4200)
cd frontend
npm install
ng serve
```

Ouvrir **http://localhost:4200** — login : `admin` / `admin`

## Comptes de test

| Rôle       | Accès                                              |
|------------|----------------------------------------------------|
| ADMIN      | Tout                                               |
| MANAGER    | Dashboard, Commerciaux, Clients, Planning, Perfs   |
| COMMERCIAL | Dashboard, Planning, Performances                  |

Créer des comptes MANAGER/COMMERCIAL via `POST /api/auth/register` (token ADMIN requis).

## Structure

```
src/app/
├── core/
│   ├── guards/          authGuard · roleGuard
│   ├── interceptors/    authInterceptor (Bearer + logout sur 401)
│   ├── models/          Interfaces TS miroir des DTOs Spring Boot
│   └── services/        AuthService (signals + sessionStorage)
├── features/
│   ├── auth/login/      Formulaire réactif + validation
│   ├── dashboard/       Placeholder — vue d'ensemble à venir
│   ├── commerciaux/     Placeholder — liste/CRUD à venir
│   ├── clients/         Placeholder — liste/CRUD à venir
│   ├── planning/        Placeholder — calendrier + IA à venir
│   └── performances/    Placeholder — KPIs à venir
└── layout/shell/        Sidebar + topbar adaptatifs au rôle
```

## Proxy

`proxy.conf.json` redirige `/api/*` → `http://localhost:8080` (actif avec `ng serve`).
