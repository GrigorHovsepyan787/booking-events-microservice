# Booking Events Microservice

A distributed microservice architecture for event booking and management system. The project implements a complete booking platform with user authentication, event management, booking processing, and notification services using Spring Boot, Spring Cloud Gateway, and modern data stores.

---

# Architecture

The system follows a microservice architecture pattern with an API Gateway as the single entry point. Each microservice is responsible for a specific business domain and communicates asynchronously via Apache Kafka.

Architectural patterns

• Database per Service
• Event-Driven Architecture
• API Gateway Pattern
• Shared Nothing Services
• Stateless Authentication (JWT)

#

This project was created to practice building a production-style
microservice architecture using Spring Boot.

Main goals:

- Microservice communication
- JWT authentication
- API Gateway
- Event-driven architecture
- Docker deployment
- Database-per-service pattern

#

# Profiles

Profiles

application.yml

application-docker.yml

application-prod.yml

Docker Compose injects environment variables from .env.

#

## Microservices

### API Gateway
- **Purpose**: Single entry point for all client requests
- **Responsibilities**: Request routing, JWT authentication, rate limiting, CORS handling
- **Database**: Redis (for rate limiting)
- **Interaction**: Routes requests to all downstream services based on path predicates

### User Service
- **Purpose**: User management and authentication
- **Responsibilities**: User registration, login, logout, JWT token generation and validation, refresh token management
- **Database**: PostgreSQL (users_db)
- **Interaction**: Publishes user events to Kafka; stores refresh tokens in Redis

### Event Service
- **Purpose**: Event management and catalog
- **Responsibilities**: Event CRUD operations, event search and pagination, event caching
- **Database**: PostgreSQL (events_db)
- **Interaction**: Publishes event-related events to Kafka; uses Redis for caching

### Booking Service
- **Purpose**: Event booking management
- **Responsibilities**: Booking creation, retrieval, and cancellation; booking history
- **Database**: PostgreSQL (bookings_db)
- **Interaction**: Publishes booking events to Kafka; consumes events from other services

### Notification Service
- **Purpose**: Notification processing and delivery
- **Responsibilities**: Notification storage, retrieval, and deletion; processes Kafka events to generate notifications
- **Database**: MongoDB (notifications_db)
- **Interaction**: Consumes events from Kafka topics; provides notification history via REST API

## Infrastructure Components

### Apache Kafka
Message broker for asynchronous inter-service communication. Services publish domain events (user registration, event creation, booking confirmation) to Kafka topics, enabling loose coupling and event-driven architecture.

### Redis
Used for two purposes:
- Refresh token storage in User Service
- Rate limiting in API Gateway

### PostgreSQL
Three separate databases for data isolation:
- users_db: User accounts and credentials
- events_db: Event catalog and metadata
- bookings_db: Booking transactions

### MongoDB
Stores notification documents with flexible schema for notification history.

---

# Tech Stack

Backend

- Java 21
- Spring Boot
- Spring Security

Persistence

- PostgreSQL
- MongoDB
- Redis

Messaging

- Kafka

Infrastructure

- Docker
- Docker Compose

Build

- Maven

Documentation

- SpringDoc OpenAPI

---

# Project Structure

```
booking-events-microservice/
├── api-gateway/
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── user-service/
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── event-service/
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── booking-service/
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── notification-service/
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── security-common/
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── common/
│   ├── src/
│   └── pom.xml
├── docker-compose.yml
├── .env
└── pom.xml
```

---

# Features

* JWT-based Authentication (Access and Refresh tokens)
* Role-based Authorization
* API Gateway Routing with path-based predicates
* Redis-based Rate Limiting
* Event Booking with pagination support
* Event Management with CRUD operations
* Notification Processing via Kafka
* Kafka Event Messaging for async communication
* PostgreSQL database per service
* MongoDB for notification storage
* Liquibase database migrations
* OpenAPI documentation (SpringDoc)
* Actuator endpoints

---

# Getting Started

## Requirements

* Java 21
* Maven 3.8+
* Docker Desktop
* Docker Compose

## Clone

```bash
git clone https://github.com/GrigorHovsepyan787/booking-events-microservice.git
cd booking-events-microservice
```

## Build

```bash
mvn clean install
```

## Start

```bash
docker compose up --build
```

The API Gateway will be available at `http://localhost:8080`.

---

# Environment Variables

| Variable | Description |
| ----------------- | ------------------- |
| POSTGRES_USER | PostgreSQL username (default: root) |
| POSTGRES_PASSWORD | PostgreSQL password (default: root) |
| MONGO_USER | MongoDB root username (default: root) |
| MONGO_PASSWORD | MongoDB root password (default: root) |
| JWT_SECRET | JWT signing key (min 256-bit recommended) |
| JWT_EXPIRATION | Access token expiration in milliseconds (default: 3600000 = 1 hour) |
| JWT_REFRESH_EXPIRATION | Refresh token expiration (default: 7d) |
| KAFKA_BOOTSTRAP_SERVERS | Kafka broker address (default: kafka:29092) |
| USER_DB_URL | JDBC URL for user database |
| EVENT_DB_URL | JDBC URL for event database |
| BOOKING_DB_URL | JDBC URL for booking database |
| MONGO_URI | MongoDB connection string |
| REDIS_HOST | Redis hostname (default: redis) |
| REDIS_PORT | Redis port (default: 6379) |

---

# API

All endpoints are accessed through the API Gateway at `http://localhost:8080`.

## Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/auth/login | User login, returns JWT tokens |
| POST | /api/auth/register | User registration |
| POST | /api/auth/logout | User logout (invalidates refresh token) |
| POST | /api/auth/refresh | Refresh access token using refresh token |

## Events

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/events | Get all events (paginated) |
| GET | /api/events/my | Get current user's events |
| GET | /api/events/{id} | Get event by ID |
| POST | /api/events | Create new event |
| PUT | /api/events/{id} | Update event |
| DELETE | /api/events/{id} | Delete event |

## Bookings

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/bookings | Get current user's bookings (paginated) |
| GET | /api/bookings/{id} | Get booking by ID |
| POST | /api/bookings | Create new booking |
| DELETE | /api/bookings/{id} | Cancel booking |

## Notifications

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/notifications | Get current user's notifications (paginated) |
| DELETE | /api/notifications/{id} | Delete notification |

---

# Security

## JWT Authentication

The system uses JWT (JSON Web Token) for authentication with two token types:

- **Access Token**: Short-lived token (default 1 hour) used for API authentication
- **Refresh Token**: Long-lived token (default 7 days) used to obtain new access tokens

## User Roles

The system implements role-based authorization. User roles are stored in the JWT token claims and extracted during authentication.

## Authorization Flow

1. User registers or logs in via `/api/auth/login` or `/api/auth/register`
2. Server validates credentials and returns access and refresh tokens
3. Client includes access token in `Authorization: Bearer <token>` header
4. API Gateway and services validate JWT on each request
5. Refresh tokens are stored in Redis for revocation support
6. Expired access tokens can be refreshed via `/api/auth/refresh`

## Token Storage

- Refresh tokens are stored in Redis with expiration matching JWT_REFRESH_EXPIRATION
- Access tokens are stateless JWT tokens
- Logout invalidates the refresh token in Redis

---

# Docker

## Containers

The system runs the following containers:

| Service | Container Name | Port | Image |
|---------|---------------|------|-------|
| API Gateway | api-gateway | 8080 | Built from ./api-gateway/Dockerfile |
| User Service | user-service | 8081 | Built from ./user-service/Dockerfile |
| Event Service | event-service | 8083 | Built from ./event-service/Dockerfile |
| Booking Service | booking-service | 8084 | Built from ./booking-service/Dockerfile |
| Notification Service | notification-service | 8082 | Built from ./notification-service/Dockerfile |
| PostgreSQL (Users) | postgres-user | 5433 | postgres:17 |
| PostgreSQL (Events) | postgres-events | 5435 | postgres:17 |
| PostgreSQL (Bookings) | postgres-bookings | 5436 | postgres:17 |
| MongoDB | mongo-notification | 27017 | mongo:8 |
| Redis | redis | 6379 | redis:8-alpine |
| Kafka | kafka-kraft | 9092 | apache/kafka:3.8.0 |

## Volumes

- `postgres_user_data`: PostgreSQL users database persistence
- `mongo_notification_data`: MongoDB notification data persistence
- `kafka_data`: Kafka broker data persistence
- `redis_data`: Redis data persistence

## Networks

All services communicate via Docker Compose default network. Services reference each other by container name (e.g., `postgres-user`, `kafka`, `redis`).

## Ports

| Port | Service |
|------|---------|
| 8080 | API Gateway |
| 8081 | User Service |
| 8082 | Notification Service |
| 8083 | Event Service |
| 8084 | Booking Service |
| 5433 | PostgreSQL (users_db) |
| 5435 | PostgreSQL (events_db) |
| 5436 | PostgreSQL (bookings_db) |
| 27017 | MongoDB (notifications_db) |
| 6379 | Redis |
| 9092 | Kafka (external) |
| 29092 | Kafka (internal) |

## Health Checks

All services implement health checks:
- PostgreSQL: `pg_isready` command
- MongoDB: `mongosh` ping command
- Redis: `redis-cli ping` command
- Application services: Actuator `/actuator/health` endpoint

Services use `depends_on` with `condition: service_healthy` to ensure proper startup order.

---

# .env file variables

POSTGRES_USER=root
POSTGRES_PASSWORD=root

MONGO_USER=root
MONGO_PASSWORD=root

JWT_SECRET=(You should add it here)

JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=7d

KAFKA_BOOTSTRAP_SERVERS=kafka:29092

USER_DB_URL=jdbc:postgresql://postgres-user:5432/users_db
EVENT_DB_URL=jdbc:postgresql://postgres-events:5432/events_db
BOOKING_DB_URL=jdbc:postgresql://postgres-bookings:5432/bookings_db

MONGO_URI=mongodb://root:root@mongo-notification:27017/notifications_db?authSource=admin

REDIS_HOST=redis
REDIS_PORT=6379

# Tests

JUnit 5

Mockito

Spring Boot Test

@WebMvcTest

@DataJpaTest

# Future Improvements

Kubernetes

CI/CD

Monitoring

Distributed Tracing

Resilience4j

Testcontainers

---
