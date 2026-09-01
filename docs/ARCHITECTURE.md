# StockPulse — Architecture

## Repository Structure (Actual)

```text
StockPulse/
├── docs/
│   ├── problem-statement.html      # STRICT source of truth
│   ├── PROJECT_CONTEXT.md, IMPLEMENTATION_STATUS.md, ARCHITECTURE.md, DECISIONS.md, CHANGELOG.md
├── backend/                        # Spring Boot 3.2.4 (Maven)
│   ├── pom.xml
│   ├── src/main/java/com/stockpulse/
│   │   ├── StockpulseBackendApplication.java
│   │   ├── ai/                     # LLMGateway, AiCommerceAdvisor, AiPricingStrategy, AiReorderStrategy
│   │   ├── bootstrap/              # DataSeeder (Addendum A 8 products)
│   │   ├── config/                 # WebConfig (CORS), AsyncConfig (@EnableAsync)
│   │   ├── domain/                 # Product, PricingSuggestion, ReorderSuggestion, InventorySnapshot, Enums
│   │   ├── dto/                    # Requests, Responses, Decision DTOs
│   │   ├── engine/                 # StrategyType, PricingStrategy, ReorderStrategy, RuleBased strategies, Manager
│   │   ├── event/                  # ProductInventoryChangedEvent
│   │   ├── exception/              # ResourceNotFoundException, BadRequestException, GlobalExceptionHandler
│   │   ├── loop/                   # AgenticRecommendationListener (@Async event listener + idempotency)
│   │   ├── repository/             # ProductRepository, PricingSuggestionRepository, ReorderSuggestionRepository, InventorySnapshotRepository
│   │   ├── service/                # ProductService, SuggestionService
│   │   └── web/                    # ProductController, PricingSuggestionController, ReorderSuggestionController, CommerceController
│   └── src/test/java/com/stockpulse/ # 27 Unit, Integration, and E2E Tests
├── frontend/stockpulse/            # React 19 + Vite
│   ├── src/
│   │   ├── services/api.js         # API integration client
│   │   ├── App.jsx                 # Real-time Merchandising Console & Simulation Workbench
│   │   ├── App.css, index.css
│   │   └── main.jsx
├── ADR.md                          # Architecture Decision Record (6 Decisions)
├── README.md                       # Quickstart (<5 min), Demo Walkthrough, Architecture Spec
├── StockPulse_EXECUTION_PLAN_FINAL.md
└── StockPulse_QUICKSTART_FINAL.md
```

## Backend Architecture Components

1. **Domain Layer (`com.stockpulse.domain`)**:
   - Entities: `Product`, `PricingSuggestion`, `ReorderSuggestion`, `InventorySnapshot`.
   - Enums: `Category`, `ProductStatus`, `PriceDirection`, `SuggestionStatus`, `TriggerReason`.
   - Invariants: Product lifecycle transitions (`ACTIVE -> PRICE_REVIEW_PENDING -> ACTIVE`, `OUT_OF_STOCK` on zero inventory).
   - Sprint 2 Seams: `costPrice` on `Product`, `suggestedLeadTimeDays` on `ReorderSuggestion`.

2. **Commerce Strategy Engine (`com.stockpulse.engine`)**:
   - Strategy interfaces: `PricingStrategy` and `ReorderStrategy`.
   - Implementations: `RuleBasedPricingStrategy` (+10% on low stock, +5% on velocity spike, HOLD otherwise), `RuleBasedReorderStrategy` (`threshold * 3 - stock`), `AiPricingStrategy`, `AiReorderStrategy`.
   - `CommerceStrategyManager`: Central coordinator managing active strategy type (`RULE_BASED` vs `AI`) with zero-downtime runtime switching via `POST /commerce/strategy`.

3. **AI Commerce Advisor (`com.stockpulse.ai`)**:
   - `LLMGateway`: Multi-provider gateway for Gemini 1.5 Flash, Groq, Ollama, OpenAI-compatible APIs, and local fallback.
   - `AiCommerceAdvisor`: Constructs prompt templates differentiating `INVENTORY_LOW` (price protection vs clearance) from `DEMAND_SPIKE` (surge pricing without gouging), validates output bounds (`0.2x <= price <= 3.0x`, `qty >= 1`), and falls back to deterministic rule strategies on failure.

4. **Agentic Recommendation Loop (`com.stockpulse.loop`)**:
   - `AgenticRecommendationListener`: Asynchronous event listener triggered on `ProductInventoryChangedEvent`.
   - Evaluates `INVENTORY_LOW` and `DEMAND_SPIKE` triggers.
   - Enforces idempotency to avoid duplicate `PENDING` suggestions.

5. **REST API Controllers (`com.stockpulse.web`)**:
   - `ProductController`: `/products` CRUD, `/products/{id}/stock`, `/products/{id}/orders`, `/products/{id}/suggest-pricing`, `/products/{id}/suggest-pricing/stream` (SSE Bonus), `/products/{id}/suggest-reorder`.
   - `PricingSuggestionController`: `/pricing-suggestions` list, get, and `PATCH` accept/reject.
   - `ReorderSuggestionController`: `/reorder-suggestions` list, get, and `PATCH` accept/reject.
   - `CommerceController`: `/commerce/strategy` get and switch active strategy.

## Frontend Architecture Components

- `src/services/api.js`: Reusable HTTP client communicating with backend endpoints.
- `src/App.jsx`: Complete Merchandising Console containing:
  - Global Header & Active Strategy Switcher (`Rule-Based` vs `AI Advisor`).
  - 3-second live polling indicator and manual refresh.
  - Interactive Agentic Loop Step visualizer.
  - Pending Pricing & Reorder Recommendation cards with AI rationale, confidence score meters, and Accept/Reject buttons.
  - Product Catalog Table with stock health gauges, demand velocity surge badges, instant sale simulation buttons (`⚡ Order 1`, `⚡⚡ Order 5`), and on-demand recommendation triggers.
