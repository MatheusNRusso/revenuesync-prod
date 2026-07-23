# RevenueSync

**A Web3 marketplace where service providers get paid instantly on Solana — with built-in negotiation, conversion tracking, and a full merchant/buyer dashboard experience.**

Think *Stripe + Linktree for crypto*: merchants publish a public profile, buyers discover them, negotiate in real time, and pay directly to the merchant's wallet — no intermediary, no custody, confirmed on-chain in seconds.

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen?logo=springboot)
![Angular](https://img.shields.io/badge/Angular-21-red?logo=angular)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Neon-blue?logo=postgresql)
![Solana](https://img.shields.io/badge/Solana-Pay-9945FF?logo=solana)
![Flyway](https://img.shields.io/badge/Flyway-migrations-CC0200)
![Deploy](https://img.shields.io/badge/deployed-Render%20%2B%20Vercel-black)

---

## Table of Contents

- [Why this project](#why-this-project)
- [Screenshots](#screenshots)
- [Core features](#core-features)
- [GitHub integration](#github-integration)
- [Architecture](#architecture)
- [Key engineering decisions](#key-engineering-decisions)
- [Tech stack](#tech-stack)
- [Project structure](#project-structure)
- [Running locally](#running-locally)
- [Roadmap](#roadmap)

---

## Why this project

Most crypto payment demos stop at "generate a QR code, get paid." RevenueSync goes further: it's a two-sided marketplace with the full lifecycle a real payment product needs — discovery, negotiation, checkout, confirmation, and post-sale conversation management. It was built to explore, hands-on, the parts of payment infrastructure that are usually hidden behind a processor's API: on-chain verification, event-driven side effects, multi-tenant authorization, and conversation state management at scale.

---

## Screenshots

> _Screenshots to be added._

| Discover marketplace | Merchant dashboard | Chat & payment request |
|---|---|---|
| `screenshot placeholder` | `screenshot placeholder` | `screenshot placeholder` |

| Solana Pay checkout | Mobile payment (Phantom) | Archived conversations |
|---|---|---|
| `screenshot placeholder` | `screenshot placeholder` | `screenshot placeholder` |

---

## Core features

### Marketplace & profiles
- Public merchant profiles with slug-based URLs (`/u/:slug`)
- Discover feed with category filters, excluding the logged-in user's own listings
- GitHub OAuth or email/password authentication
- Multi-merchant support per user (a single account can run several storefronts)

### Payments (Solana Pay)
- On-chain payment requests with unique reference per transaction
- Async verification job polling the Solana blockchain for confirmation
- PIX-inspired UX: 5-minute QR expiration, single-use requests, duplicate-payment guard
- Manual confirmation fallback for edge cases
- Full payment ledger (`payments`, `solana_payments`) with reconciliation support

### Chat & negotiation
- Real-time-feel chat (polling-based) between buyer and merchant
- In-chat payment requests — merchant generates a QR without leaving the conversation
- Payment confirmation posted automatically to the chat thread
- **Per-side conversation management** (WhatsApp-style):
  - Archive / delete independently for buyer and merchant
  - Deleting or archiving never affects the other side's view
  - A new message resurrects a conversation either side had archived/deleted
  - Dedicated "Archived" view with restore (unarchive)
- Self-chat prevention enforced at the database query level, not just the UI

### Conversion tracking
- Every confirmed payment dispatches events to **Meta CAPI**, **Google Ads**, and **Pipedrive**
- Side effects are isolated — a failure in one integration (e.g. Meta returning 401) never blocks payment confirmation or the other integrations
- Full request/response payload persistence for auditability

### Admin & merchant tooling
- Admin dashboard: users, merchants, payments, conversions, leads
- Merchant dashboard: revenue charts, payment history, wallet management, per-merchant filtering
- CSV export for payments

---

## GitHub integration

RevenueSync is built for developers, so GitHub is its **primary identity provider and trust anchor** — not a decorative "Sign in with GitHub" button. A user's identity, public profile, and login email are all resolved from the GitHub API. Builders authenticate with GitHub and get a profile derived from their real GitHub presence; the platform never asks them to re-type data GitHub already verifies.

### OAuth application & scopes

Authentication uses a **GitHub OAuth App** (Authorization Code flow) requesting the **minimum scope** required — principle of least privilege:

- `user:email` — read the user's email addresses with visibility and verification status via `GET /user/emails`.

No write scopes are requested. RevenueSync never creates repositories, opens issues, or modifies any resource on the user's behalf.

### GitHub API endpoints consumed

| Endpoint | Purpose |
|----------|---------|
| `GET /user` | Display name, avatar, public repository count, follower count, and the stable `login` used as the identity key. |
| `GET /user/emails` | Resolve a **verified** email (primary + verified, falling back to any verified address). |

### GitHub-first identity

- **Accounts are created exclusively through GitHub OAuth.** Direct email/password registration is disabled in production (`POST /auth/register` returns `410 Gone`), removing the attack surface of open registration and credential stuffing. It can be re-enabled per environment via `app.auth.allow-direct-registration` (used only in local development, where the OAuth flow cannot complete).
- **The login email is verified by GitHub, not self-declared.** On every login the backend reads `GET /user/emails` and selects a verified address through a bounded, fault-tolerant fallback chain — an upstream failure degrades gracefully instead of breaking login.
- **Legacy accounts are reconciled.** Accounts created before this model that still carry a synthetic placeholder email are updated in place to the real verified address on the next login, matched by the stable GitHub username so no duplicate account is ever created.
- **Password login is preserved** for existing accounts; no user is locked out.

### Secure token handoff

After a successful OAuth login the backend hands the session token to the frontend via a `302` redirect carrying the JWT in the **URL fragment** (`#token=...`). The fragment is never sent to the server, never written to access logs, and never leaked through the `Referer` header; the frontend reads it and strips it from the address bar immediately. Responses are protected by a strict **Content Security Policy** (`script-src 'self'`).

### Privacy

The verified email is used **only for account login** and is never exposed on the public profile, which shows only GitHub-derived data the user already makes public (avatar, repository count, followers).

> The full rationale lives in [`docs/adr/0002-github-first-identity.md`](docs/adr/0002-github-first-identity.md).

---

## Architecture

```
                        ┌──────────────────────┐
                        │    Angular 21 SPA    │
                        │  (standalone comps)  │
                        └──────────┬───────────┘
                                   │ REST (JWT)
                        ┌──────────▼───────────┐
                        │   Spring Boot API    │
                        │ Controller → Service │
                        │     → Repository     │
                        └───┬────────┬────────┬┘
                            │        │        │
              ┌─────────────▼──┐ ┌───▼────┐ ┌─▼──────────────┐
              │ PostgreSQL     │ │ Solana │ │ Meta/Google/   │
              │ (Neon, Flyway) │ │ RPC    │ │ Pipedrive APIs │
              └────────────────┘ └────────┘ └────────────────┘
                                     ▲
                                     │
                      ┌──────────────┴───────────────┐
                      │    SolanaVerificationJob     │
                      │     (scheduled polling,      │
                      │    on-chain confirmation)    │
                      └──────────────────────────────┘

```

**Payment confirmation pipeline:**

```
Solana tx confirmed
      │
      ▼
Payment upserted (idempotent)
      │
      ├──▶ Chat notification        (try/catch — isolated)
      ├──▶ Meta CAPI dispatch       (try/catch — isolated)
      ├──▶ Google Ads dispatch      (try/catch — isolated)
      └──▶ Pipedrive lead creation  (try/catch — isolated)
```

Each side effect is wrapped independently so that a third-party outage degrades gracefully instead of breaking the payment flow — a lesson learned the hard way after a Meta API 401 once silently killed the entire pipeline before this isolation was added.

---

## Key engineering decisions

**Polling over WebSocket for chat.** WebSocket infrastructure existed early in the project but was never actually wired to a consumer. Rather than keep unused complexity, it was removed in favor of a proven, Render-free-tier-friendly polling pattern (`interval(3000)`), which is simpler to reason about and sufficient for the product's real-time needs.

**Per-side conversation state instead of a shared status.** Early versions used a single conversation `status` (ACTIVE/CLOSED), which caused a real bug: closing a conversation from one side left it inaccessible for both. The fix models archive/delete as independent boolean flags per side (`archived_by_buyer`, `deleted_by_merchant`, etc.), matching how messaging apps like WhatsApp actually behave — and matching what a two-sided marketplace needs.

**Null-safe anonymous access on public endpoints.** A security review caught that adding a self-chat filter to the public Discover endpoint introduced a `NullPointerException` for logged-out visitors (`@AuthenticationPrincipal User user` with no fallback). Fixed by making the filter tolerate a null principal at both the controller and the JPQL query level — a reminder that authorization changes on `permitAll()` routes need explicit handling of the unauthenticated case.

**Idempotent payment upserts.** Payments are upserted by external ID, not blindly inserted, so retried webhooks or duplicate on-chain confirmations never create duplicate records.

**Dev/prod parity via full codebase mirroring.** The private development repo is kept as a structural mirror of the production repo rather than a divergent branch, avoiding the schema and dependency drift that caused migration conflicts earlier in the project.

---

## Tech stack

**Backend:** Java 21 · Spring Boot 3.5 · Spring Security 6 (JWT via `jjwt`, GitHub OAuth2) · Spring Data JPA · PostgreSQL (Neon) · Flyway

**Frontend:** Angular 21 (standalone components) · RxJS · TypeScript

**Payments:** Solana Pay · Solana Web3.js

**Integrations:** Meta Conversions API · Google Ads API · Pipedrive CRM

**Infra:** Docker (local Postgres) · Render (backend) · Vercel (frontend)

---

## Project structure

```
revenuesync/
├── src/main/java/com/mtnrs/revenuesync/
│   ├── controller/      # REST endpoints (public, auth, chat, admin, solana)
│   ├── service/         # Business logic, payment pipeline, chat rules
│   ├── domain/           # JPA entities (Merchant, Payment, Conversation, ...)
│   ├── repository/       # Spring Data repositories
│   ├── dto/              # Request/response DTOs
│   └── infra/             # Security config, JWT filter, exception handling
├── src/main/resources/db/migration/   # Flyway migrations (V1 → V25+)
└── web/
    └── src/app/
        ├── pages/         # Discover, merchant/buyer dashboards, checkout, admin
        ├── shared/         # Chat component and other shared UI
        └── core/           # Services, models, guards
```

---

## Running locally

**Prerequisites:** Java 21, Node 20+, Docker, a Postgres container or local instance.

```bash
# 1. Start Postgres
docker start revenue-postgres   # or docker run ... postgres:15

# 2. Backend — copy .env.example to .env and fill in the values
set -a && source .env && set +a
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run

# 3. Frontend
cd web
npm install
ng serve --proxy-config proxy.conf.json
```

Backend: `http://localhost:8080` · Frontend: `http://localhost:4200`

Flyway applies all migrations automatically on startup. An admin user is seeded on first boot (see `ADMIN_EMAIL` / `ADMIN_PASSWORD` in `.env`).

> GitHub OAuth is not configured for local development — use email/password login locally.

---

## Roadmap

- [ ] Multi-negotiation model per merchant (separate deal threads instead of one lifelong conversation)
- [ ] Deal funnel states (negotiating / completed — won / lost)
- [ ] Block user
- [ ] Automated test coverage (JwtService, ChatService)
- [ ] Multi-coin support
- [ ] Email verification

---

## License

This project is licensed under the [MIT License](LICENSE).
