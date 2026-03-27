# Gestion d'Abonnements - Application Full-Stack

![Version](https://img.shields.io/badge/version-2.0-blue.svg)
![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Build](https://img.shields.io/badge/build-passing-green.svg)
![Docker](https://img.shields.io/badge/Docker-ready-blue.svg)

Application de gestion d'abonnements avec interface web moderne, backend Java robuste, analytics avancé et déploiement dockerisé.

## 🚀 Démarrage Rapide

### Option 1 : Exécution Directe (JAR)

**Prérequis**: Java 21+

```bash
# Build
mvn clean package -Pprod -q

# Exécuter l'API
    java -jar target/gestion-abonnements-1.0-SNAPSHOT.jar api

# Ou utiliser le CLI
java -jar target/gestion-abonnements-1.0-SNAPSHOT.jar dashboard file=data/abonnements.csv

# Aide
java -jar target/gestion-abonnements-1.0-SNAPSHOT.jar help
```

L'API démarre sur **`http://localhost:4567`**

### Option 2 : Docker (Recommandé)

**Prérequis**: Docker + Docker Compose

```bash
# Build et lancer
docker-compose up --build

# Accéder à l'API
curl http://localhost:4567/health

# Arrêter
docker-compose down
```

### Option 3 : Build avec Profils Maven

```bash
# DEV (avec tests)
mvn clean verify -Pdev

# PROD (skip tests, optimisé)
mvn clean package -Pprod -DskipTests
```

## 🐳 Déploiement Docker

### Architecture Docker

```yaml
Backend Service (Java API + CLI)
├── Port: 4567 (API)
├── Volumes: ./data (persistence)
├── Network: app-network
└── Health Check: Dashboard CLI test
```

### Commandes Utiles

```bash
# Build l'image
docker build -t gestion-abonnements:latest .

# Lancer le conteneur
docker run -p 4567:4567 -v ./data:/app/data gestion-abonnements:latest api

# Lancer avec docker-compose
docker-compose up -d

# Consulter les logs
docker-compose logs -f backend

# Accéder au CLI dans le conteneur
docker-compose exec backend java -jar app.jar dashboard file=/app/data/abonnements.csv
```

### Configuration Environment

```env
# Copy .env.example -> .env then fill your keys

# Mailgun (emails real-time)
MAILGUN_API_KEY=key_xxxxxxxxxxxxxxxxxxxxxxxx
MAILGUN_DOMAIN=mg.your-domain.com

# Okta (sub-account identity management)
OKTA_ORG_URL=https://dev-xxxxxxxx.okta.com
OKTA_API_TOKEN=xxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Stripe (real payment processing)
STRIPE_SECRET_KEY=sk_test_xxxxxxxxxxxxxxxxxxxx

# Optional app settings
JAVA_OPTS=-Xmx512m -Xms128m
LOG_LEVEL=INFO
```

If these keys are missing or invalid, the application automatically falls back to local mode for Mailgun/Okta/Stripe.

### OpenAI - Feature Recommendation (`student40006741`)

La feature de recommandation d'abonnements utilise l'API OpenAI côté backend uniquement.

Variables attendues :

```env
OPENAI_API_KEY=sk-your-openai-api-key
OPENAI_MODEL=gpt-5.1
OPENAI_BASE_URL=https://api.openai.com/v1/chat/completions
```

Un exemple prêt à copier existe dans [backend/.env.example](./backend/.env.example).

Important :
- ne jamais exposer `OPENAI_API_KEY` dans le frontend
- la clé doit rester côté serveur
- la route backend utilisée est `POST /api/recommendations`

## 🔄 Pipeline CI/CD

### GitHub Actions

Le projet inclut un pipeline CI automatisé (`.github/workflows/ci.yml`):

```
✅ Maven Build (DEV + PROD)
✅ Unit Tests (avec couverture)
✅ JAR Validation
✅ Docker Build
✅ Container Test
```

**Déclenche automatiquement sur:**
- Push vers `main` ou `develop`
- Pull requests

### Exécuter Localement

```bash
# Vérifier compilation
mvn clean compile

# Exécuter les tests
mvn clean test

# Build JAR
mvn clean package -Pprod

# Vérifier JAR exécutable
java -jar target/gestion-abonnements-1.0-SNAPSHOT.jar help
```

## ✨ Fonctionnalités Principales

### PARTIE 3 : Feature AZIZ - Open Banking
- ✅ Détection automatique d'abonnements (CSV parsing → NLP recognition)
- ✅ Enrichissement API (ExchangeRate-API, DummyJSON, Mailgun)
- ✅ Scoring 6-critères sophistiqué (montant, fréquence, confiance, catégorie, déviation, devise)
- ✅ Détection anomalies (5 détecteurs : doublons, sous-utilisation, mutation prix/fréquence, budget exceeded)
- ✅ Recommandations automatiques (KEEP / REVIEW / CANCEL)
- ✅ Pipeline 7-phases <2 secondes end-to-end

### PARTIE 4 : Feature MAISSARA - Gestion Multi-Comptes
- ✅ Architecture hiérarchique MainAccount → SubAccounts
- ✅ Permissions granulaires (canRead, canWrite, canDelete, canExport, canViewAnalytics, canManagePermissions, canUploadCSV)
- ✅ Isolation des données (zéro fuite cross-account)
- ✅ Audit trail complet (IMPORT, UPDATE, DELETE, EXPORT)
- ✅ Quotas configurables (imports/mois, storage, max users)
- ✅ Intégration Open Banking (PARTIE 3 appliquée par sous-compte)

### PARTIE 5 : Feature `student40006741` - Recommandation IA OpenAI
- ✅ Route backend `POST /api/recommendations`
- ✅ Analyse d'un texte libre utilisateur via OpenAI
- ✅ Réponse structurée JSON parsée côté backend
- ✅ Mapping local de domaines vers suggestions d'abonnements
- ✅ Implémentation stateless sans base de données ni persistance
- ✅ Page de démo HTML dédiée dans `student40006741/frontend`

### Infrastructure & Qualité
- ✅ 28 tests integration + unit (100% pass rate)
- ✅ APIs distantes intégrées : ExchangeRate-API, DummyJSON, Mailgun
- ✅ Caching intelligent (24h) + fallback mechanisms
- ✅ Zero bank data exposure (traitement local uniquement)
- ✅ DevOps complet : Docker, Docker Compose, GitHub Actions CI/CD

## 🏗️ Architecture Technique

| Couche | Technologies |
|--------|------------|
| **Frontend** | HTML5, CSS3, JavaScript vanilla, Chart.js |
| **API** | Spark Framework 2.9.4 (Java 21) |
| **Backend Services** | Jackson JSON, SLF4J logging |
| **Persistance** | Fichiers CSV, Format JSON |
| **Déploiement** | Docker (Alpine Linux, JAR 9.6MB) |
| **CI/CD** | GitHub Actions |

## 📊 Dashboard CLI

Commande principale pour visualiser le portefeuille :

```bash
java -jar app.jar dashboard file=data/abonnements.csv
```

Affiche:
- Résumé financier (dépenses, projection annuelle)
- Score de santé du portefeuille
- Composition par catégorie
- Abonnements à haut risque churn
- Top 5 priorités à conserver
- Opportunités d'économies
- Expirations proches

## 👥 Auteurs

- **Aziz TLILI** (41006201)
- **Maissara FERKOUS** (42006149)
- **Thi Mai Chi DOAN** (40016084)

## 📋 Projet

- **Contexte** : Projet DevOps & Architecture Backend (MIAGE M1)
- **Dates** : Octobre 2025 - Février 2026
- **Objectif** : Application robuste avec architecture clean et déploiement automatisé

---

📅 Dernière mise à jour : 10 février 2026

**Status**: ✅ Backend Core Complete | 🐳 Docker Ready | 🔄 CI/CD Active
