# StockPulse — Project Context

## What StockPulse Is

StockPulse is an **AI Inventory & Dynamic Pricing Engine** for a multi-category ecommerce store ("ShopStream") with SKUs across Electronics, Apparel, and Home goods.

## The Problem Solved

Prices were previously set manually and reviewed on weekly cadence, leading to margin loss during stock depletions and missed revenue during viral demand spikes. StockPulse automates the reactive merchandising loop:

**Detect → Recommend → Approve/Reject → Act**

- **Detect:** Inventory drops below reorder threshold or demand velocity spikes above 2x category baseline.
- **Recommend:** Pluggable engine (Rule-Based or AI Advisor) generates price adjustment and reorder batch recommendations with confidence metrics and reasoning.
- **Approve/Reject:** Merchandisers review `PENDING` suggestions on a real-time console. Live state is never modified without explicit human approval.
- **Act:** Accepting a pricing suggestion updates the catalog live price; accepting a reorder suggestion applies simulated inbound replenishment.

## Source of Truth

`docs/problem-statement.html` is the **STRICT source of truth** for all requirements.

## Current Technology Stack (Fully Implemented & Verified)

- **Backend:** Java 17+, Spring Boot 3.2.4 (Web, Data JPA, Validation, Actuator, Lombok), Maven
- **Frontend:** React 19 + Vite (`frontend/stockpulse/`), Vanilla CSS
- **Database:** H2 in-memory (`jdbc:h2:mem:stockpulse`), console at `/h2-console`
- **AI Engine:** `LLMGateway` supporting Gemini 1.5 Flash, Groq, Ollama, OpenAI-compatible APIs, and local heuristic fallback
- **Agentic Decoupling:** Spring `ApplicationEventPublisher` + `@Async @EventListener` with repository-level idempotency
- **Bonus:** Server-Sent Events (SSE) token streaming on `POST /products/{id}/suggest-pricing/stream`

## Environment & Configuration (Verified from Source)

| Variable | Component | File | Required? | Default |
|---|---|---|---|---|
| `LLM_PROVIDER` | Backend | `LLMGateway.java` via `application.properties` | Optional | `gemini` |
| `LLM_API_KEY` | Backend | `LLMGateway.java` via `application.properties` | Optional | *(empty — uses fallback)* |
| `LLM_MODEL` | Backend | `LLMGateway.java` via `application.properties` | Optional | `gemini-1.5-flash` |
| `LLM_BASE_URL` | Backend | `LLMGateway.java` via `application.properties` | Optional | `https://generativelanguage.googleapis.com` |
| `VITE_API_BASE_URL` | Frontend | `src/services/api.js` | Optional | `http://localhost:8080` |

> All hardcoded values in `application.properties` are H2 in-memory database credentials (no external service). No secrets are committed. Zero required environment variables to run.

## CORS Configuration (Verified)
- Allowed origins: `http://localhost:5173`, `http://127.0.0.1:5173`, `http://localhost:4200`, `http://127.0.0.1:4200`, `http://localhost:3000`
- Configured in `WebConfig.java`

## Project Status

- All phases 0 through 9 are **COMPLETE** and verified.
- Submission-readiness audit completed.
- 27/27 tests pass (`mvn test` → BUILD SUCCESS in 17s).
- React console builds in 154ms (`npm run build` → dist generated).
- `README.md` and `ADR.md` present at root with full documentation.
- `.env.example` templates present in both `backend/` and `frontend/stockpulse/`.
