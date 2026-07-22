# 2. GitHub-First Identity and Verified Email Resolution

## Status

Accepted — 2026-07-22

## Context

RevenueSync targets developers: builders publish a public profile derived from
their GitHub presence, and buyers hire and pay them. Two identity problems
surfaced early:

1. **No way to verify ownership of an email.** The platform has no outbound
   SMTP infrastructure. A direct email+password registration cannot confirm
   that the user actually owns the address they typed, so any throwaway or
   fake email works. This makes accounts non-auditable and opens the door to
   spam and abuse.
2. **Unreliable identity for OAuth users.** When a GitHub user keeps their
   email private, the standard OAuth payload exposes no email. The original
   implementation fell back to a synthetic `<login>@github.oauth` placeholder,
   which leaked into the user dashboard and made the "login email" meaningless.

Because the entire product revolves around developers and their GitHub
identity, GitHub is the natural — and, for the initial release, the only —
trust anchor for identity.

## Decision

Adopt a **GitHub-first identity model**:

1. **Accounts are created exclusively through GitHub OAuth.** Direct
   email+password registration is disabled in production. The endpoint
   `POST /auth/register` returns `410 Gone` when registration is closed.
   - Controlled by `app.auth.allow-direct-registration` (default `false` in
     production, `true` in the local dev profile so the flow remains testable
     where OAuth cannot complete).
   - The admin seed (`DataInitializer`) writes directly through the repository
     and is unaffected by the gate.
   - Password login (`POST /auth/login`) is preserved for accounts that
     already exist; no user is locked out.

2. **The verified email is resolved from the GitHub API**, not from the OAuth
   payload. On every OAuth login the backend calls `GET /user/emails` with the
   OAuth access token (`user:email` scope) and selects the email with a
   three-level fallback:
   1. primary + verified email;
   2. any verified email;
   3. the `email` attribute from the OAuth payload;
   4. synthetic `<login>@github.oauth` as a last resort (never blocks login).
   The call is bounded by timeouts and wrapped so an upstream failure degrades
   gracefully instead of breaking login.

3. **Legacy synthetic emails are reconciled.** If an existing account still
   carries a `@github.oauth` address and a real verified email is resolved, the
   account email is updated in place (guarded against collisions with emails
   already owned by another account). Identity is matched by the stable
   `githubUsername`, so no duplicate account is ever created.

4. **The OAuth token handoff is secure.** After a successful login the backend
   issues a `302` redirect carrying the JWT in the **URL fragment**
   (`/oauth2/callback#token=...`), which the frontend reads and then strips via
   `history.replaceState`. The fragment is never sent to the server, never
   written to access logs, and never leaked through the `Referer` header. This
   replaces an earlier inline-`<script>` handoff that both violated the Content
   Security Policy and exposed the token in the query string.

5. **A strict Content Security Policy is enforced.** Responses carry
   `script-src 'self'` (no `unsafe-inline`); `style-src` keeps `unsafe-inline`
   only because Angular component encapsulation requires it.

## Consequences

**Positive**
- Identity is auditable: GitHub is the attester, and every account maps to a
  real, verified GitHub identity.
- The fake-email gap is eliminated without building SMTP infrastructure.
- The attack surface shrinks: no open registration means no credential-stuffing
  target on `/auth/register` and no disposable accounts.
- Dashboards show a real email; the `@github.oauth` placeholder is reconciled
  away on the next login.
- The JWT no longer appears in query strings, server logs, or `Referer`
  headers, and the CSP is strict.

**Negative / trade-offs**
- **Reduced reach:** only people with a GitHub account can sign up. This is
  acceptable because the product is explicitly aimed at developers.
- **Single identity provider:** GitHub becomes a single point of failure for
  login. Mitigation path: add a second IdP (e.g. Google) later — without
  reopening direct registration.

**Neutral**
- Existing email+password accounts keep working; there is no forced migration.

## Considered Alternatives

1. **Direct registration with SMTP email verification** — rejected: requires
   email infrastructure (own SMTP or a third-party service) and added
   complexity, while the target audience already has GitHub.
2. **Direct registration without verification** (the original behavior) —
   rejected: fake emails, non-auditable accounts, larger abuse surface.
3. **Multiple IdPs from day one (GitHub + Google)** — deferred: GitHub-first is
   sufficient for the audience; a second IdP is a future, additive change.
4. **Token handoff via inline script + query parameter** (the original
   behavior) — rejected: violates the CSP and leaks the token into logs and
   `Referer` headers. Replaced by the `302` + fragment handoff.

## References

- GitHub OAuth — requesting the `user:email` scope
- GitHub REST API — `GET /user/emails`
- MDN Web Docs — Content Security Policy `script-src`
- OWASP — credential stuffing and registration abuse
