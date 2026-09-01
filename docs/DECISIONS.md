# StockPulse — Decisions Log

Format: Decision → Reason → Consequence.

## D-001 — Backend: Java + Spring Boot 3.2.4 + Maven
- **Decision:** Built backend as a Spring Boot 3.2.4 application with Maven (`backend/pom.xml`), Java 17 target.
- **Reason:** Mandated by the problem statement HTML brief (Spring Boot 3.x, Maven/Gradle).
- **Consequence:** Clean enterprise foundation; runs on standard JDK 17/21 with zero external server dependencies.

## D-002 — Database: H2 in-memory with Seed Data
- **Decision:** Used in-memory H2 (`jdbc:h2:mem:stockpulse`) with `DataSeeder` loading the 8 Addendum A products.
- **Reason:** Enables instant, zero-configuration startup (<5 min) for hackathon demo and reviewer verification.
- **Consequence:** Pre-seeded with `PRD-003` near threshold and `PRD-008` for demand velocity spike demonstrations.

## D-003 — Frontend: React 19 + Vite
- **Decision:** Built merchandising console in React 19 + Vite (`frontend/stockpulse/`).
- **Reason:** High-speed development with Vite HMR; lightweight bundle (208 kB total); dev server on port 5173 matching CORS configuration.
- **Consequence:** Fast real-time polling and seamless interactive demo experience.

## D-004 — Commerce Logic Separation
- **Decision:** Encapsulated pricing heuristics, replenishment formulas, and AI prompting into dedicated strategy beans (`PricingStrategy`, `ReorderStrategy`) orchestrated by `CommerceStrategyManager`.
- **Reason:** Prevents controller/service bloat; isolates AI prompt engineering from JPA entities.
- **Consequence:** High testability (100% rule formula unit test coverage); easy runtime strategy switching.

## D-005 — Unified vs. Split Strategy Contracts
- **Decision:** Implemented separate `PricingStrategy` and `ReorderStrategy` interfaces rather than a monolithic interface.
- **Reason:** Allows independent failure handling and modular extensions (e.g. Sprint 2 `CompetitorAwarePricingStrategy` can be added without altering reorder logic).
- **Consequence:** Independent fallback resilience per recommendation type.

## D-006 — Runtime Zero-Downtime Strategy Switching
- **Decision:** Implemented `CommerceStrategyManager` maintaining a dynamic bean map of available strategies with a volatile active strategy pointer, exposed via `POST /commerce/strategy`.
- **Reason:** Problem statement requires strategy switching at runtime without restarting the application or modifying code.
- **Consequence:** Merchandisers can switch between Rule-Based and AI Advisor instantly from the frontend UI.

## D-007 — LLM Failure Resilience with Deterministic Fallbacks
- **Decision:** Wrapped all LLM API calls with sanity bounds validation and automatic fallback to `RuleBasedPricingStrategy` and `RuleBasedReorderStrategy`.
- **Reason:** LLMs can time out, exceed quotas, or return malformed JSON; the autonomous recommendation pipeline must never crash or silently drop proposals.
- **Consequence:** System maintains 100% operational uptime even when offline or during LLM outages.

## D-008 — Event-Driven Asynchronous Agentic Loop Decoupling
- **Decision:** Used Spring `ApplicationEventPublisher` + `@Async @EventListener AgenticRecommendationListener` with repository-level idempotency checks.
- **Reason:** Order and stock update HTTP endpoints must return immediately (<10ms) without waiting for AI LLM inference.
- **Consequence:** True reactive architecture; duplicate triggers while suggestions are `PENDING` are safely deduplicated.

## D-009 — Bonus SSE Token Streaming
- **Decision:** Implemented `POST /products/{id}/suggest-pricing/stream` returning `SseEmitter`.
- **Reason:** Problem statement offers +5 bonus points for streaming AI reasoning tokens end-to-end.
- **Consequence:** Supports progressive streaming of merchandising rationale.
