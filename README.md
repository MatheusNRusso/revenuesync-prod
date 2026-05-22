# RevenueSync

Stripe → Ads (Meta / Google) + CRM (Pipedrive)

RevenueSync is a Spring Boot API that processes Stripe webhook events and dispatches confirmed payments to advertising platforms (Meta CAPI, Google Ads) while creating leads in a CRM (Pipedrive).

All operations are persisted for auditability and traceability. The project includes mock endpoints for safe local development.

---

## Business Context

RevenueSync simulates a revenue attribution and marketing automation pipeline:

1. A payment occurs in Stripe
2. The backend validates and normalizes the event
3. Conversion events are sent to ad platforms
4. A lead is created in the CRM
5. All interactions are persisted for analytics and compliance

This pattern is common in SaaS, e-commerce, and performance marketing systems.

---

## Features

* Stripe webhook endpoint: `POST /webhooks/stripe`
* Signature verification (`Stripe-Signature`)
* Event parsing and normalization
* Idempotent payment upsert (`external_id`)
* Conversion dispatch:

  * Meta CAPI (mock supported)
  * Google Ads (mock supported)
* Lead creation (Pipedrive mock supported)
* Full audit persistence:

  * `payments`
  * `conversions`
  * `leads`
* Flyway migrations
* PostgreSQL support

---

## Supported Stripe Events

* `checkout.session.completed`

---

## Architecture (High Level)

Stripe
→ Webhook Controller
→ StripeWebhookService
→ PaymentService (idempotent upsert)
→ ConversionService (Meta + Google)
→ LeadService (Pipedrive)
→ PostgreSQL

---

## Processing Flow

1. Receive webhook
2. Verify signature
3. Parse event JSON
4. Upsert payment
5. If status = SUCCEEDED:

   * Send conversion to Meta
   * Send conversion to Google
   * Create lead (if email present)
6. Persist request and response payloads

---

## Endpoints

### Stripe Webhook

POST `/webhooks/stripe`

### Mock Providers (local)

POST `/mock/meta`
POST `/mock/google`
POST `/mock/pipedrive`

Mocks allow full end-to-end testing without external dependencies.

---

## Database

### payments

Normalized Stripe payment data.

### conversions

Stores each dispatch attempt:

* platform
* value
* request_payload
* response_payload
* created_at

### leads

Stores CRM lead payload.

---

## Running Locally

Start PostgreSQL:

```bash
docker compose up -d
```

Run the API:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

API:
[http://localhost:8080](http://localhost:8080)

---

## Example — checkout.session.completed

```bash
curl -X POST http://localhost:8080/webhooks/stripe \
  -H "Content-Type: application/json" \
  -H "Stripe-Signature: t=123,v1=fake" \
  -d '{
    "id": "evt_test_001",
    "type": "checkout.session.completed",
    "data": {
      "object": {
        "id": "cs_test_001",
        "amount_total": 1990,
        "currency": "brl",
        "payment_status": "paid",
        "customer_details": { "email": "test@example.com" }
      }
    }
  }'
```

Expected result:

* Payment stored
* 2 conversions created (META + GOOGLE)
* 1 lead created
* All payloads persisted

---

## Design Decisions

* Layered architecture (Controller → Service → Domain)
* WebClient for outbound HTTP
* Transactional webhook handling
* Idempotent payment processing
* Full audit persistence

---

## Tech Stack

* Java 21
* Spring Boot
* Spring Data JPA
* WebClient
* PostgreSQL
* Flyway
* Docker


