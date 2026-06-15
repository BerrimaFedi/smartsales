# SmartSales

> Application intelligente de gestion des commerciaux et du planning de travail, basée sur des agents IA.
SmartSales est une solution **web et mobile** qui permet de gérer les équipes commerciales, les visites clients, les plannings et les performances, avec trois agents d'intelligence artificielle pour optimiser les tournées, analyser les performances et assister les utilisateurs.

![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot&logoColor=white)
![Angular](https://img.shields.io/badge/Angular-21-DD0031?logo=angular&logoColor=white)
![Flutter](https://img.shields.io/badge/Flutter-3.41-02569B?logo=flutter&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-Python-009688?logo=fastapi&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)

---

## Sommaire

- [Aperçu](#aperçu)
- [Fonctionnalités](#fonctionnalités)
- [Les agents IA](#les-agents-ia)
- [Architecture](#architecture)
- [Stack technique](#stack-technique)
- [Démarrage rapide (Docker)](#démarrage-rapide-docker)
- [Démarrage en mode développement](#démarrage-en-mode-développement)
- [Application mobile](#application-mobile)
- [Comptes de démonstration](#comptes-de-démonstration)
- [Structure du projet](#structure-du-projet)
- [Auteur](#auteur)

---

## Aperçu

SmartSales s'adresse à trois types d'utilisateurs :

| Rôle | Accès |
|------|-------|
| **Administrateur** | Gère les utilisateurs, les rôles et la configuration globale |
| **Manager / Responsable commercial** | Planifie les missions, suit et analyse les performances de l'équipe |
| **Commercial** | Consulte son planning, gère ses visites, rédige ses comptes-rendus (web + mobile) |

L'application combine une interface web complète (pour l'administration et le pilotage) et une application mobile dédiée aux commerciaux en déplacement.

---

## Fonctionnalités

### Gestion intelligente du planning
- Création et gestion des plannings
- **Affectation automatique** des missions (par zone géographique, charge de travail et compétence)
- **Réaffectation dynamique** en cas d'imprévu
- Visualisation calendrier (jour / semaine / mois)
- **Optimisation IA** des tournées commerciales (carte interactive)

### Gestion des commerciaux et des clients
- Profils commerciaux avec compte de connexion automatique
- Affectation par zone géographique
- Gestion des compétences
- Clients géolocalisés (latitude / longitude)

### Missions et visites
- Planification des visites (prospection, relance, négociation)
- Suivi des statuts (planifiée, en cours, terminée, annulée)
- Comptes-rendus et montants
- Géolocalisation des missions

### Suivi des performances
- Indicateurs clés : chiffre d'affaires, nombre de visites, taux de conversion
- Performances **calculées automatiquement** à partir des visites
- **Tableau de bord BI** interactif (graphiques en barres, camemberts, courbes d'évolution)
- Export des rapports au format CSV

### Assistant et notifications
- **Assistant intelligent** (chatbot) répondant aux questions métier
- **Notifications intelligentes** : rappels de missions, alertes de retard, suggestions de relance

### Application mobile (commerciaux)
- Consultation du planning
- Check-in / check-out géolocalisé des visites
- Saisie des comptes-rendus
- Navigation vers les clients (Google Maps)

---

## Les agents IA

Le projet intègre **trois agents** d'aide à la décision :

| Agent | Rôle | Approche |
|-------|------|----------|
| **Optimisation des tournées** | Calcule l'ordre de visite optimal (distance + priorité) | Algorithme du voyageur de commerce : haversine + plus proche voisin + 2-opt |
| **Analyse des performances** | Détecte les tendances, anomalies et propose des recommandations | Analyse statistique (moyenne, écart-type) + règles métier |
| **Assistant (chatbot)** | Répond aux questions métier et suggère des actions | Détection d'intentions par règles + interrogation des données |

> Ces agents relèvent de l'IA décisionnelle (algorithmique, statistique et à base de règles), et non de modèles de machine learning.

---

## Architecture

```
                    ┌──────────────────┐
   Navigateur ────► │  Frontend (nginx)│
                    │     Angular      │
                    └────────┬─────────┘
                             │  /api  (reverse proxy)
                             ▼
   App mobile ───────► ┌──────────────────┐      ┌────────────────────┐
   (Flutter)           │  Backend         │ ───► │  Service IA        │
                       │  Spring Boot     │      │  FastAPI (Python)  │
                       │  REST + JWT      │      └────────────────────┘
                       └────────┬─────────┘
                                │  JDBC
                                ▼
                       ┌──────────────────┐
                       │   PostgreSQL     │
                       └──────────────────┘
```

Le **frontend web** et l'**application mobile** partagent le même backend et la même base de données : toute donnée saisie depuis l'un est immédiatement disponible sur l'autre.

---

## Stack technique

| Couche | Technologies |
|--------|--------------|
| **Frontend web** | Angular (standalone, signals), SCSS, Chart.js, Leaflet, FullCalendar |
| **Application mobile** | Flutter / Dart (provider, geolocator, url_launcher) |
| **Backend** | Java 17, Spring Boot 3.2.5, Spring Security (JWT), Spring Data JPA |
| **Service IA** | Python, FastAPI, Uvicorn |
| **Base de données** | PostgreSQL 16 |
| **Conteneurisation** | Docker, Docker Compose, nginx |

---

## Démarrage rapide (Docker)

La méthode recommandée : toute l'application démarre avec une seule commande.

### Prérequis
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installé et démarré

### Lancement

```bash
# Cloner le dépôt
git clone https://github.com/BerrimaFedi/smartsales.git
cd smartsales

# Construire et démarrer les 4 services
docker compose up --build
```

Au premier démarrage, la base est automatiquement initialisée et remplie avec un jeu de données de démonstration (zones, commerciaux, clients, visites).

Une fois les services démarrés, l'application est accessible sur :

| Service | URL |
|---------|-----|
| **Application web** | http://localhost:4200 |
| API backend | http://localhost:8080 |
| Service IA | http://localhost:8000 |

### Commandes utiles

```bash
docker compose up -d        # démarrer en arrière-plan
docker compose logs -f      # suivre les logs
docker compose down         # arrêter (les données sont conservées)
docker compose down -v      # arrêter et réinitialiser la base (données de démo rechargées)
```

---

## Démarrage en mode développement

Pour développer chaque service séparément (hors Docker).

### Prérequis
- Java 17, Maven
- Node.js 20 + Angular CLI
- Python 3.12
- PostgreSQL 16 (base `smartsales`, utilisateur `smartsales` / `smartsales`)

### Backend (Spring Boot)
```bash
cd backend
mvn spring-boot:run
```
Démarre sur http://localhost:8080

### Service IA (FastAPI)
```bash
cd ai-service
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

### Frontend (Angular)
```bash
cd frontend
npm install
ng serve
```
Démarre sur http://localhost:4200

---

## Application mobile

L'application mobile n'est pas conteneurisée (une application mobile s'installe sur un appareil, pas dans un conteneur). Elle communique avec le backend (dockerisé ou local) sur le port 8080.

```bash
cd mobile
flutter pub get
flutter run -d edge      # test dans le navigateur
# ou : flutter run        (émulateur / appareil Android)
```

> Pour un émulateur Android, l'URL de l'API doit être `http://10.0.2.2:8080` (configurable dans `mobile/lib/config/api_config.dart`).

---

## Comptes de démonstration

| Identifiant | Mot de passe | Rôle |
|-------------|--------------|------|
| `admin` | `admin` | Administrateur |
| `sami` | `admin` | Commercial |
| *(autres commerciaux)* | `admin` | Commercial |

> Les comptes commerciaux sont créés automatiquement par le jeu de données de démonstration.

---

## Structure du projet

```
smartsales/
├── backend/            # API REST Spring Boot (Java)
├── frontend/           # Application web Angular
├── mobile/             # Application mobile Flutter
├── ai-service/         # Service d'agents IA (Python / FastAPI)
├── docker/             # Scripts d'initialisation (seed)
├── docker-compose.yml  # Orchestration des 4 services
└── smartsales-seed.sql # Données de démonstration
```

---

---

<p align="center"><i>Application intelligente de gestion commerciale — web, mobile et agents IA.</i></p>
