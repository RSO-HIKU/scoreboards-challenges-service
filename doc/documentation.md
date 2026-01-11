# Scoreboards & Challenges Service - Technical Documentation

## Overview

The **Scoreboards & Challenges Service** is a microservice responsible for managing hiking challenges and tracking user challange completion within the HIKU hiking application. It provides endpoints for viewing challenges, completing them, and displaying leaderboards that rank users based on their completed challenges.

## Table of Contents

1. [Architecture](#architecture)
2. [Technology Stack](#technology-stack)
3. [Database Schema](#database-schema)
4. [API Endpoints](#api-endpoints)
5. [Authentication & Authorization](#authentication--authorization)
6. [Business Logic](#business-logic)
7. [Health Checks](#health-checks)
8. [Configuration](#configuration)
9. [Deployment](#deployment)
10. [Local Development](#local-development)


---

## Architecture

### Key Components

- **Controllers**: Handle HTTP requests and responses
- **Service Layer**: Business logic for challenges and scoreboards
- **DAOs**: Data Access Objects for database operations
- **Entities**: JPA-mapped domain models
- **Health**: Kubernetes-ready health check endpoints


---

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Runtime** | Java (Eclipse Temurin) | 17+ |
| **Build Tool** | Maven | 3.9 |
| **Framework** | KumuluzEE | 4.1.0 |
| **JPA Provider** | Hibernate | 5.6.15.Final |
| **Database** | PostgreSQL | 14+ |
| **Migration** | Flyway | 9.16.1 |
| **Authentication** | MicroProfile JWT | 2.1 |
| **Dependency Injection** | CDI (Weld) | via KumuluzEE |
| **Containerization** | Docker | - |
| **Orchestration** | Kubernetes (via Helm) | - |



## Database Schema

### Schema: `scoreboards_challenges_service`

#### Table: `challenges`

Stores monthly hiking challenges.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | SERIAL | PRIMARY KEY | Auto-increment challenge ID |
| `title` | VARCHAR(255) | NOT NULL | Challenge title |
| `description` | TEXT | - | Challenge description |
| `month` | INTEGER | NOT NULL | Month (1-12) |
| `year` | INTEGER | NOT NULL | Year |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Challenge creation timestamp |

**Unique Constraint**: `(title, month, year)` - prevents duplicate challenges in same month

**Indexes**:
- Primary key on `id`
- Index on `(month, year)` for efficient monthly queries

#### Table: `user_challenge_completions`

Tracks which users completed which challenges.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | SERIAL | PRIMARY KEY | Auto-increment ID |
| `user_id` | VARCHAR(255) | NOT NULL | User ID (UUID) |
| `username` | VARCHAR(255) | - | Username for display |
| `challenge_id` | INTEGER | NOT NULL, FK → challenges(id) | Reference to challenge |
| `completed_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Completion timestamp |

**Unique Constraint**: `(user_id, challenge_id)` - prevents duplicate completions

**Foreign Keys**:
- `challenge_id` references `challenges(id)` ON DELETE CASCADE

**Indexes**:
- Primary key on `id`
- Index on `user_id` for user queries
- Index on `challenge_id` for challenge statistics

### Database Migration

Database schema is managed using **Flyway** migrations located in `src/main/resources/db/migration/`.

Rules when working with migrations:
- Each migration has to follow the naming convention: VX__\<short name\>, where X is the next number that hasn't been used yet.
- Database migrations must be idempotent. You must not delete already existing and applied migrations.

Migrations run automatically via Kubernetes Job (see `helm/templates/migrate-job.yaml`).

### Default Challenges

The service comes pre-populated with 5 challenges.


## API Endpoints

Api endpoints are described in doc/Scoreboards and Challanges Service.postman_collection.json file.


## Authentication & Authorization

### JWT-Based Authentication

The Scoreboards & Challenges Service uses **MicroProfile JWT** with Keycloak as the identity provider.


## Business Logic

### Challenge Management

The service manages monthly hiking challenges with the following capabilities:

- **Challenge Retrieval**: Users can view challenges for the current month 
- **Challenge Persistence**: Challenges are stored with unique titles per month/year to prevent duplicates
- **Historical Access**: Acess to challenges from past months

### Challenge Completion Tracking

The service tracks user progress through the challenge system:

- **Completion Recording**: Users can mark challenges as completed, creating a timestamped completion record
- **Duplicate Prevention**: The system prevents users from completing the same challenge multiple times
- **User Attribution**: Each completion is associated with both user ID and username for display purposes
- **Validation**: The system verifies challenge existence before allowing completion

### Scoreboard Generation

The service provides dynamic leaderboards that rank users based on their challenge completion:

- **Monthly Rankings**: Scoreboards are calculated per month/year, showing the top performers for that period
- **Aggregation**: Completion counts are aggregated by user to determine rankings
- **Configurable Display**: Leaderboards support configurable limits (default top 10 users)
- **User Context**: Each scoreboard entry includes user ID, username, and total completed challenges

## Health Checks

The service implements two health check endpoints for Kubernetes orchestration:



**Endpoint**: `GET /api/scoreboards-challenges/health`

Performs multiple health checks:
1. **Database connectivity** - Executes test query
2. **Memory usage** - Monitors JVM heap usage
3. **Uptime tracking** - Reports service uptime

### Liveness Probe

**Endpoint**: `GET /api/scoreboards-challenges/health/live`

Indicates if the application is running. Kubernetes restarts the pod if this fails.


---


## Configuration

### Application Configuration

Configuration file: `src/main/resources/config.yaml`

### JPA Configuration

Configuration file: `src/main/resources/META-INF/persistence.xml`





### Environment Variables

These values can be set in the `\helm\template\values-dev.yaml` file.

| Variable | Description | Default |
|----------|-------------|---------|
| `KUMULUZEE_ENV_NAME` | Environment name | `dev` |
| `KUMULUZEE_SERVER_HTTP_PORT` | HTTP server port | `8091` |
| `KUMULUZEE_SERVER_HTTP_ADDRESS` | Bind address | `0.0.0.0` |
| `KUMULUZEE_DATASOURCES_DEFAULT_CONNECTIONURL` | JDBC connection URL | `jdbc:postgresql://localhost:5432/hikudb` |
| `KUMULUZEE_DATASOURCES_DEFAULT_POOL_MAX_SIZE` | Connection pool size | `3` |
| `KUMULUZEE_JWT_AUTH_ISSUER` | JWT issuer URL | (required) |
| `KUMULUZEE_JWT_AUTH_JWKS_URI` | JWKS endpoint for JWT verification | (required) |
| `RABBITMQ_HOST` | RabbitMQ hostname | `rabbitmq.platform.svc.cluster.local` |
| `RABBITMQ_PORT` | RabbitMQ port | `5672` |

For local dev these values can be set in the `\helm\template\secret.yaml` file.
For the test and prod environments they are obtained from our Azure Key Vault.

| Secret | Description | Default |
|--------|-------------|---------|
| `KUMULUZEE_DATASOURCES_DEFAULT_USERNAME` | Database username | `hikuuser` |
| `KUMULUZEE_DATASOURCES_DEFAULT_PASSWORD` | Database password | `hikupassword` |
| `FLYWAY_USER` | Flyway Database role username | `hikuuser` |
| `FLYWAY_PASSWORD` | Flyway Database role password | `hikuadmin` | 
| `PG_HOST` | Database hostname | `localhost` |
| `RABBITMQ_USER` | RabbitMQ username | `hikuuser` |
| `RABBITMQ_PASSWORD` | RabbitMQ password | `hikupassword` |

---

## Deployment


#### Database Migrator Image

**Dockerfile**: `Dockerfile.migrator`

Runs Flyway migrations as a Kubernetes Job.

---

### Kubernetes (Helm)

#### Chart Structure

```
helm/
├── Chart.yaml              # Chart metadata
├── values-dev.yaml         # Development values
└── templates/
    ├── _helpers.tpl        # Template helpers
    ├── deployment.yaml     # Main application deployment
    ├── service-clusterip.yaml  # Internal service
    ├── service-nodeport.yaml   # External service (dev)
    ├── migrate-job.yaml    # Database migration job
    ├── secret.yaml         # Database credentials
    └── secretsproviderclass.yaml  # Azure Key Vault integration
```

---

### CI/CD Pipelines

#### Test Environment Pipeline

**File**: `.github/workflows/test-build-deploy.yaml`

**Triggers**:
- Push to `test` branch

**Steps**:
1. Checkout code
2. Build Docker images (app + migrator)
3. Push to Azure Container Registry (ACR)
4. Deploy to AKS test environment using ArgoCD

---

#### Production Promotion Pipeline

**File**: `.github/workflows/prod-promote.yaml`

**Triggers**:
- Manual workflow dispatch with image tag selection

**Steps**:
1. Pull images from test ACR
2. Retag images for production
3. Push to production ACR
4. Deploy to AKS production environment

Secrets used in the GitHub Actions workflows are saved as secrets in our GitHub Organization. Secrets used for deployment on the Azure cluster are provided by our Azure Key Vault.

---

## Local Development

Building images and deployment for local development is handled by Skaffold. By running the command **skaffold dev** in the root folder of the repository in a terminal window will make Skaffold automatically build and deploy the service to your local Minikube cluster. Skaffold watches your local files and when you save a change, Skaffold automatically applies it.  

### Prerequisites

#### Required Tools & Services
- **Java 17+**
- **Maven 3.9+**
- **PostgreSQL 14+**
- **Docker Desktop**
- **Keycloak**
- **RabbitMQ**
- **Minikube**
- **Skaffold**

#### Other requirements
- Docker Desktop is running,
- Minikube cluster is running on Docker Desktop,
- The database is deployed on your local cluster,
- The Traefik ingress controller is deployed on your local cluster,
- Keycloak is deployed on your local cluster.

### Steps performed by Skaffold
- Builds docker image for microservice,
- Builds docker image for database migrations,
- Deploys both images,
- Portforwards NodePort to the default port setting.

