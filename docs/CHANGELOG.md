# StockPulse — Changelog

Chronological record of meaningful project implementation changes.

## Initial Setup (Phase 0)
- Verified repository foundation: Spring Boot 3.2.4 (Maven), H2 in-memory configuration, Vite + React scaffold in `frontend/stockpulse/`.

## Phase 1 — Domain Model & Persistence
- Created JPA domain entities: `Product`, `PricingSuggestion`, `ReorderSuggestion`, `InventorySnapshot`.
- Created enums: `Category`, `ProductStatus`, `PriceDirection`, `SuggestionStatus`, `TriggerReason`.
- Added Sprint 2 extension seams: `costPrice` on `Product`, `suggestedLeadTimeDays` on `ReorderSuggestion`.
- Implemented Spring Data JPA repositories: `ProductRepository`, `PricingSuggestionRepository`, `ReorderSuggestionRepository`, `InventorySnapshotRepository`.
- Implemented `DataSeeder` pre-loading the 8 Addendum A catalog products and initial snapshots.

## Phase 2 — Product & Order APIs
- Implemented `ProductService` managing product CRUD, stock updates, and simulated sales orders.
- Implemented `ProductController` with `POST /products`, `GET /products`, `GET /products/{id}`, `PATCH /products/{id}/stock`, `POST /products/{id}/orders`.
- Added request validation (`jakarta.validation`), DTOs, and `GlobalExceptionHandler`.
- Configured CORS for `localhost:5173`, `localhost:4200`, and `localhost:3000`.

## Phase 3 — Pluggable Commerce Engine
- Implemented `PricingStrategy` and `ReorderStrategy` interfaces.
- Implemented `RuleBasedPricingStrategy` (+10% on low stock, +5% on velocity spike, HOLD otherwise).
- Implemented `RuleBasedReorderStrategy` (`(threshold * 3) - stock`, min 1).
- Implemented `CommerceStrategyManager` and `CommerceController` for zero-downtime runtime strategy switching (`GET/POST /commerce/strategy`).

## Phase 4 — AI Commerce Advisor
- Implemented `LLMGateway` supporting Gemini, Groq, Ollama, OpenAI-compatible APIs, and local fallback.
- Implemented `AiCommerceAdvisor` with distinct prompt templates for `INVENTORY_LOW` (tradeoff analysis) and `DEMAND_SPIKE` (surge pricing).
- Added output bounds validation and automatic fallback to rule strategies.
- Implemented `AiPricingStrategy` and `AiReorderStrategy` registered with `StrategyType.AI`.

## Phase 5 — Suggestion Management APIs
- Implemented on-demand endpoints: `POST /products/{id}/suggest-pricing` and `POST /products/{id}/suggest-reorder`.
- Implemented `PricingSuggestionController` and `ReorderSuggestionController` with list, get, and `PATCH` decision endpoints.
- Wired atomic side-effects: Accepting pricing updates product live price; accepting reorder increments stock level.

## Phase 6 — Asynchronous Agentic Loop
- Implemented `AgenticRecommendationListener` listening to `ProductInventoryChangedEvent` asynchronously via `@Async @EventListener`.
- Implemented automated triggers for `INVENTORY_LOW` and `DEMAND_SPIKE`.
- Implemented repository-level idempotency preventing duplicate `PENDING` suggestions.

## Phase 7 — React Merchandising Console
- Implemented full-featured React console in `frontend/stockpulse/src/App.jsx`.
- Implemented API client in `frontend/stockpulse/src/services/api.js`.
- Added 3-second live polling loop, active strategy toggle, pending suggestion cards with AI reasoning and Accept/Reject buttons, product catalog table with stock health meters, and instant sale simulation controls.
- Created custom typography and color theme in `App.css`.

## Phase 8 & 9 — Hardening, Tests & Documentation
- Implemented 27 comprehensive tests across `DataJpaAndSeederTest`, `ProductApiTest`, `CommerceStrategyTest`, `AiCommerceAdvisorTest`, `SuggestionApiTest`, `AgenticLoopTest`, and `EndToEndFlowTest`.
- Implemented Bonus SSE token streaming endpoint on `POST /products/{id}/suggest-pricing/stream`.
- Created submission-ready `README.md` and `ADR.md`.

## Submission Readiness Audit
- Completed comprehensive audit of all configuration values, environment variables, endpoints, and secrets.
- Created `backend/.env.example` and `frontend/stockpulse/.env.example`.
- Verified that zero API keys are required for evaluation/demo (built-in generation and deterministic fallback).
- Re-verified full test suite: 27/27 tests passing.
- Re-verified frontend production build: 154ms build time.
- Synchronized all 5 persistent memory docs in `docs/`.
