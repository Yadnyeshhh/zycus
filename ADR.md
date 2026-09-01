# StockPulse — Architecture Decision Record (ADR)

This document records the architectural decisions that shaped the StockPulse autonomous inventory and dynamic pricing engine.

Format: **Context → Options → Decision → Tradeoffs**

---

## 1. Where Does Commerce Logic Live?

### Context
In reactive commerce systems, business logic can easily become entangled in controllers or bloated God-services when pricing formulas, reorder calculations, event publishing, and persistence are intermixed.

### Options
1. **Fat Service Layer**: Place all pricing heuristics, reorder math, and LLM calls inside `ProductService`.
2. **Domain-Driven Rich Entities**: Place all dynamic calculation and LLM integration methods inside the `Product` entity.
3. **Dedicated Strategy & Advisor Components (Chosen)**: Separate concerns into a pluggable engine (`com.stockpulse.engine`), dedicated AI advisor (`com.stockpulse.ai`), and lifecycle coordinators (`SuggestionService`, `ProductService`).

### Decision
Option 3 was chosen. `Product` maintains core state and lifecycle invariants (`ACTIVE`, `PRICE_REVIEW_PENDING`, `OUT_OF_STOCK`). Heuristics and AI recommendations are encapsulated within `PricingStrategy` and `ReorderStrategy` implementations managed by `CommerceStrategyManager`.

### Tradeoffs
Introduces additional interfaces and coordination classes compared to putting everything into a single service, but completely prevents controller bloat, provides clean testability, and allows swapping strategies at runtime.

---

## 2. Unified vs. Split Strategy Contracts

### Context
The commerce engine produces both pricing adjustments and reorder replenishment recommendations when inventory signals fire.

### Options
1. **Single Monolithic Contract**: A single `CommerceAdvisor` interface with a single method returning both pricing and reorder suggestions simultaneously.
2. **Dedicated Split Contracts with Shared Strategy Manager (Chosen)**: Separate `PricingStrategy` and `ReorderStrategy` interfaces orchestrated by `CommerceStrategyManager`.

### Decision
Option 2 was chosen. While AI prompting benefits from evaluating pricing and replenishment in unified context, split strategy contracts allow independent fallbacks (e.g. pricing strategy can succeed with AI while reorder strategy falls back to rules, or vice versa), and allow different strategy implementations (e.g. Sprint 2's `CompetitorAwarePricingStrategy`) to be introduced without modifying reorder logic.

### Tradeoffs
Requires two strategy invocations in the coordinator, but ensures modularity and granular resilience.

---

## 3. Runtime Strategy Switching

### Context
The system must support switching between Rule-Based and AI-Assisted strategies at runtime without restarting the Spring Boot application or modifying code.

### Options
1. **Spring Profile / Property Reloading**: Rely on configuration properties and application context refreshes.
2. **Dynamic Strategy Registry / Bean Map (Chosen)**: `CommerceStrategyManager` dynamically injects all Spring beans implementing `PricingStrategy` and `ReorderStrategy`, indexing them by `StrategyType`, with a thread-safe volatile active pointer and a REST controller endpoint (`POST /commerce/strategy`).

### Decision
Option 2 was chosen. Strategy implementations register themselves via Spring dependency injection. `CommerceStrategyManager` allows switching between `RULE_BASED` and `AI` instantaneously during runtime. HTTP endpoints and async event listeners always resolve the active strategy through `CommerceStrategyManager`.

### Tradeoffs
Requires managing thread-safe strategy references in memory, but enables immediate zero-downtime strategy switching from the UI or API.

---

## 4. LLM Failure Handling & Resilience

### Context
External LLM APIs can experience rate-limiting, network timeouts, unparseable JSON, or absurd price suggestions (e.g. $0 or $1,000,000). The automated recommendation pipeline must never crash or silently drop suggestions.

### Options
1. **Retry with Exponential Backoff Only**: Retry until the LLM responds or fail with an HTTP error.
2. **Resilient Fallback to Deterministic Rules (Chosen)**: Wrap LLM execution in bounds validation and exception handling; if the LLM fails or generates out-of-bounds values, automatically fall back to deterministic `RuleBasedPricingStrategy` and `RuleBasedReorderStrategy`.

### Decision
Option 2 was chosen. `AiCommerceAdvisor` validates:
- Sane price bounds: `0.2x currentPrice <= recommendedPrice <= 3.0x currentPrice` and `recommendedPrice > 0`.
- Sane reorder quantity: `>= 1`.
- Clean JSON extraction.
If any error occurs, it transparently invokes the deterministic rule-based calculation, marks the strategy name as `AI_FALLBACK_RULE`, and prefixes the reasoning with `[AI Fallback to Rules]`.

### Tradeoffs
Fallback suggestions may lack deep natural language reasoning nuance, but ensure 100% system availability and non-blocking asynchronous operations.

---

## 5. Agentic Loop Trigger and Decoupling

### Context
Inventory changes (via simulated orders or stock level updates) must trigger recommendations automatically without blocking the HTTP request path or creating duplicate pending proposals.

### Options
1. **Scheduled Polling Cron**: Run a recurring timer every few minutes querying the database for low-stock items.
2. **Synchronous Call in Controller**: Run LLM and pricing algorithms directly inside `POST /products/{id}/orders`.
3. **Event-Driven Asynchronous Pipeline with Idempotency (Chosen)**: Use Spring `ApplicationEventPublisher` to publish `ProductInventoryChangedEvent`, handled asynchronously by `@Async @EventListener AgenticRecommendationListener`.

### Decision
Option 3 was chosen. The HTTP order/stock endpoints update state and return immediately in under 10ms. The async listener receives the event, checks idempotency via `PricingSuggestionRepository.existsByProductIdAndStatusAndTriggerReason(productId, PENDING, trigger)`, and generates suggestions only if one is not already pending.

### Tradeoffs
Eventual consistency: suggestions appear on the UI's next polling interval (~3 seconds) rather than in the order response body, which matches genuine real-world event architectures.

---

## 6. Extensibility and Deliberate Exclusions

### Context
Building a clean foundation for Sprint 2 (Competitor data, supplier catalogs, margin floors) while keeping current scope strictly within requirements.

### Decision
- **Sprint 2 Extension Seams in Code**:
  - `Product.costPrice`: Nullable database column ready for margin floor guardrails.
  - `ReorderSuggestion.suggestedLeadTimeDays`: Ready for supplier catalog integrations.
  - `StrategyType.COMPETITOR_AWARE`: Enum placeholder ready for competitor scraping strategies.
  - `ProductRepository.averageDemandVelocityExcluding`: Peer-aware baseline query for multi-product categories.
- **Deliberate Exclusions**:
  - Storefront/cart/payment checkout systems (out of scope per brief).
  - External heavy message brokers (Kafka/RabbitMQ) avoided in favor of Spring's in-process asynchronous event bus to guarantee under-5-minute zero-dependency startup.
