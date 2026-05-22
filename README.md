# RevenueSync

RevenueSync is a full-stack payment attribution and merchant operations platform built with Java + Spring Boot and Angular.

The platform enables merchants to create public checkout flows powered by Solana Pay while synchronizing payment activity, buyer history, and conversion analytics through responsive admin and merchant dashboards.

---

# Overview

RevenueSync evolved from a Stripe attribution prototype into a crypto-native marketplace and merchant infrastructure platform focused on:

- Solana Pay checkout flows
- Merchant onboarding
- Public merchant discovery
- Buyer payment history
- Admin operations dashboard
- Conversion tracking
- Payment synchronization
- Responsive SaaS-style frontend

The project simulates a real-world production architecture with separated backend/frontend applications, persistent audit flows, and multi-role access control.

---

# Core Features

## Authentication & Roles

- JWT authentication
- User registration/login
- Role-based access:
  - USER
  - MERCHANT
  - ADMIN

---

## Merchant Operations

Merchants can:

- Create merchant profiles
- Configure public storefronts
- Generate Solana Pay checkout flows
- Track payments
- Access buyer history
- Monitor conversions

---

## Solana Pay Integration

RevenueSync integrates with Solana Pay to generate QR-code based payment flows.

Features:

- Dynamic Solana checkout
- QR code generation
- Payment verification polling
- Transaction synchronization
- Merchant wallet support
- Mainnet-ready architecture

---

## Marketplace / Discover

Public merchant discovery interface:

- Public storefront listing
- Merchant cards
- Responsive marketplace UI
- Payment flow integration

---

## Admin Dashboard

Administrative dashboard with:

- Payment monitoring
- Merchant management
- Conversion tracking
- Lead monitoring
- Activity feeds
- Operational analytics

---

## Buyer History

Authenticated users can:

- View purchase history
- Filter payments
- Track statuses
- Access completed transactions

---

# Architecture

## Backend

- Java 21
- Spring Boot
- Spring Security
- JWT Authentication
- Spring Data JPA
- Flyway
- PostgreSQL
- WebClient

---

## Frontend

- Angular
- TypeScript
- SCSS
- Responsive mobile-first dashboards
- Lazy-loaded modules

---

# Project Structure

```text
src/
 ├── controller/
 ├── service/
 ├── repository/
 ├── domain/
 ├── dto/
 ├── infra/
 └── config/

web/
 ├── src/app/pages/
 ├── core/
 ├── shared/
 └── styles/
```

---

# Database

Main entities:

* users
* merchants
* payments
* conversions
* leads
* solana_payments

Flyway migrations are included for full schema versioning.

---

# Running Locally

## Backend

Create your environment file:

```bash
cp .env.example .env
```

Run PostgreSQL and dependencies:

```bash
docker compose up -d
```

Run backend:

```bash
./mvnw spring-boot:run
```

Backend:

```text
http://localhost:8080
```

API documentation can be enabled locally during development.

---

## Frontend

```bash
cd web

npm install

npm start
```

Frontend:

```text
http://localhost:4200
```

---

# Environment Variables

Example variables are available in:

```text
.env.example
```

Main variables:

```env
DB_HOST=
DB_PORT=
DB_NAME=
DB_USER=
DB_PASSWORD=

JWT_SECRET=

ADMIN_EMAIL=
ADMIN_PASSWORD=

GITHUB_CLIENT_ID=
GITHUB_CLIENT_SECRET=

HELIUS_RPC_URL=
```

---

# Current Product Status

Implemented:

* Responsive dashboards
* Merchant onboarding
* Solana Pay QR checkout
* Buyer history
* Discover marketplace
* Admin operations panel
* Mobile usability improvements
* Production-ready environment configuration

In progress:

* Production deployment
* Live RPC optimizations
* Multi-token support
* Real-time pricing feeds
* Enhanced analytics

---

# Screens Included

* Landing page
* Discover marketplace
* Merchant dashboard
* Admin dashboard
* Buyer history
* Solana checkout flow

---

# Design Goals

RevenueSync was designed to simulate a real SaaS/payment infrastructure product with:

* layered architecture
* auditability
* modular frontend
* scalable backend structure
* responsive operations dashboards
* production-oriented workflows

---

# Tech Stack

## Backend

* Java 21
* Spring Boot
* Spring Security
* PostgreSQL
* Flyway

## Frontend

* Angular
* TypeScript
* SCSS

## Infrastructure

* Docker
* Solana Pay
* Helius RPC

---

# License

MIT


