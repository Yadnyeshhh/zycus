# StockPulse — Execution Plan

## 0. Source of Truth

`docs/problem-statement.html` is the **only authoritative source** for StockPulse requirements.

`EXECUTION_PLAN.md` and `QUICKSTART.md` are implementation guidance. If anything conflicts with the HTML, the HTML wins.

Do not add features, APIs, entities, infrastructure, or technologies that are not required by the HTML.

### Hackathon priority

Build mandatory functionality first. Bonus functionality such as SSE should be attempted only after all mandatory requirements, tests, UI, ADR, and walkthrough/demo requirements are complete.

---

# 1. Project Goal

StockPulse automates the merchandising loop:

**Detect → Recommend → Approve/Reject → Act**

The system should detect low inventory or demand spikes, generate pricing/reorder suggestions using the configured commerce strategy and AI advisor, show pending suggestions to merchandising, and apply changes only after human approval.

---

# 2. Technology Direction

Use the technology choices explicitly permitted by the HTML:

- Backend: Java + Spring Boot
- Persistence: H2 for the hackathon
- Frontend: React 18 + Vite
- AI: pluggable advisor supporting the configured provider
- Commerce strategy: pluggable rule-based and AI-assisted strategies

Do not introduce Kafka, Redis, Kubernetes, microservices, authentication, payments, supplier integrations, competitor scraping, or other unrelated infrastructure.

---

# 3. Domain Model

Implement the entities required by the HTML.

## Product

Required business information includes:

- product identity
- name
- category
- current price
- current stock
- reorder threshold
- demand velocity
- category average velocity

Keep the actual fields aligned with the HTML specification.

## InventorySnapshot

Represents inventory/demand state at a point in time.

Use it to support the inventory/demand information required by the system and the detection/recommendation flow.

At minimum, model the information required by the HTML:

- product reference
- stock level
- demand velocity
- timestamp

Do not add unrelated fields.

## PricingSuggestion

Represents an AI/rule-generated price recommendation.

Required concepts include:

- product
- trigger reason
- current price
- recommended price
- price direction
- confidence
- reasoning
- status
- timestamps

Required statuses:

- PENDING
- ACCEPTED
- REJECTED

## ReorderSuggestion

Represents an AI/rule-generated reorder recommendation.

Required concepts include:

- product
- trigger reason
- current stock
- recommended reorder quantity
- confidence
- reasoning
- status
- timestamps

Required statuses:

- PENDING
- ACCEPTED
- REJECTED

---

# 4. Trigger Reasons

Use the trigger reasons defined by the HTML:

- `INVENTORY_LOW`
- `DEMAND_SPIKE`

The system must distinguish these reasons in suggestions and AI prompting.

---

# 5. Commerce Strategy

Create a pluggable strategy abstraction so the recommendation logic is not hard-coded into controllers.

Support:

- rule-based strategy
- AI-assisted strategy

The active strategy must be switchable at runtime without application restart, as required by the HTML.

Do not implement runtime switching merely as a startup-only environment/property value.

---

# 6. Rule-Based Strategy

Implement the exact rules from the HTML.

### Pricing

If:

`stock < reorderThreshold`

increase price by **10%**.

If:

`demandVelocity > 2 × categoryAverageVelocity`

increase price by **5%**.

Otherwise:

`HOLD`

Do not invent additional pricing rules.

### Reorder

Use:

`reorderQuantity = (reorderThreshold × 3) - currentStock`

with a minimum of `1`.

Keep the implementation faithful to the HTML.

---

# 7. AI Advisor

Create a clean advisor abstraction.

The AI advisor must receive the product/relevant inventory context and trigger context required by the HTML.

It must produce structured output containing the required recommendation information, including:

- recommended price
- direction
- confidence
- reasoning
- reorder quantity where applicable

The two trigger reasons must result in genuinely different prompt/context instructions:

- `INVENTORY_LOW`
- `DEMAND_SPIKE`

Do not make them cosmetically different only.

---

# 8. AI Failure Handling

Handle the failure modes explicitly required by the HTML:

- timeout
- quota/API failure
- malformed JSON

When the AI advisor fails, use the rule-based strategy as fallback.

The application should remain usable and should not create an invalid suggestion because an LLM response was malformed.

---

# 9. Required APIs

Implement the APIs explicitly required by the HTML.

## Product APIs

Use the exact endpoint paths and behavior specified in the HTML.

Required operations include:

- list products
- get product
- create product
- update product
- delete product

## Recommendation APIs

Implement:

- `POST /products/{id}/suggest-pricing`
- `POST /products/{id}/suggest-reorder`

These on-demand endpoints must work without requiring a dashboard button.

## Suggestion APIs

Implement the HTML-required suggestion retrieval and decision endpoints, including:

- pending suggestions
- suggestion details
- accept
- reject

Use the exact paths and HTTP methods from the HTML specification.

Do not invent additional public APIs unless strictly necessary to implement an explicitly required behavior.

---

# 10. Automatic Agentic Loop

This is the primary business flow.

## Flow

1. An order/inventory change occurs.
2. Inventory state is updated.
3. The system evaluates the required trigger conditions.
4. If a relevant trigger fires, a recommendation is generated asynchronously.
5. The recommendation is stored as `PENDING`.
6. The merchandising console can retrieve it.
7. The merchandiser accepts or rejects it.
8. Rejection changes only the suggestion status.
9. Acceptance applies the appropriate action.

### Acceptance behavior

For an accepted pricing suggestion:

- update the product's live price.

For an accepted reorder suggestion:

- perform the inventory action required by the HTML.

Do not apply either recommendation before acceptance.

---

# 11. Idempotency

Implement the idempotency rule required by the HTML.

For a product, do not create duplicate `PENDING` suggestions for the same relevant trigger/suggestion condition when one is already pending.

The implementation must prevent repeated detection events from filling the dashboard with duplicate pending suggestions.

Test this explicitly.

---

# 12. Asynchronous Recommendation Generation

The automatic recommendation flow must be asynchronous as required by the HTML.

The inventory/order operation should trigger recommendation generation without turning the entire flow into a blocking manual dashboard action.

Keep the implementation simple and reliable for a hackathon.

Do not introduce a message broker unless the HTML requires one.

---

# 13. Frontend — Merchandising Console

Build the required React merchandising console.

The UI must provide the functionality explicitly required by the HTML.

It should show:

- products
- current price
- stock
- relevant demand information
- pending suggestions
- trigger badges/reasons
- AI reasoning
- confidence
- recommendation details
- accept/reject controls

The UI should make the end-to-end StockPulse workflow easy to demonstrate.

Do not build unrelated ecommerce pages such as carts, checkout, customer accounts, payments, or product purchasing.

---

# 14. Implementation Phases

## Phase 0 — Repository and project setup

Create:

- backend Spring Boot project
- frontend React/Vite project
- H2 configuration
- basic project structure
- `.gitignore`
- README/documentation structure

Do not implement business logic yet.

### OpenCode task

Inspect the repository before making changes. Create only the project foundation required by the HTML and this plan. Do not add unrelated infrastructure. Compile/run the empty applications and report the result.

---

## Phase 1 — Domain model and persistence

Implement:

- Product
- InventorySnapshot
- PricingSuggestion
- ReorderSuggestion
- statuses/enums required by the HTML
- repositories
- required seed/demo data

Add persistence tests where useful.

### Done when

The backend starts, schema/data initializes correctly, and the required entities can be persisted and retrieved.

---

## Phase 2 — Product and inventory/order APIs

Implement the required product APIs and the order/inventory behavior needed by the HTML.

Ensure an order/inventory change updates the relevant stock/demand information.

Add tests for normal and trigger-producing changes.

### Done when

A complete request can change product inventory and the resulting state is persisted correctly.

---

## Phase 3 — Commerce strategies

Implement:

- strategy abstraction
- rule-based strategy
- AI-assisted strategy abstraction
- runtime strategy switching

Implement the exact rule formulas.

Write unit tests for every specified rule.

### Done when

The strategy can be selected at runtime and produces correct rule-based recommendations.

---

## Phase 4 — AI advisor

Implement:

- advisor interface
- configured provider integration
- structured response parsing
- trigger-specific prompts
- confidence/reasoning extraction
- timeout/quota/malformed-response handling
- rule-based fallback

Do not add multiple AI providers unless required or necessary.

### Done when

A real configured provider can produce a structured recommendation and failure falls back safely to rules.

---

## Phase 5 — Recommendation APIs

Implement the required on-demand pricing/reorder suggestion endpoints.

Implement pending suggestion retrieval/details and accept/reject operations.

Ensure accepted suggestions update live state and rejected suggestions do not.

### Done when

The complete manual recommendation → pending → accept/reject flow works through the API.

---

## Phase 6 — Automatic agentic loop

Connect inventory/order changes to automatic detection and asynchronous recommendation creation.

Implement:

- low-inventory detection
- demand-spike detection
- correct trigger reason
- asynchronous generation
- pending persistence
- idempotency

### Done when

The main business story works without manually clicking a "generate recommendation" button.

---

## Phase 7 — React merchandising console

Implement the dashboard required by the HTML.

Prioritize:

1. product/inventory visibility
2. pending suggestions
3. trigger badges
4. AI reasoning
5. confidence
6. accept/reject actions
7. live state refresh

Keep the UI simple and demo-friendly.

### Done when

A reviewer can understand and execute the StockPulse workflow from the dashboard.

---

## Phase 8 — Integration testing and demo hardening

Run:

- backend tests
- frontend build
- end-to-end flow
- low inventory scenario
- demand spike scenario
- accept scenario
- reject scenario
- duplicate/idempotency scenario
- AI failure/fallback scenario

Fix issues before adding bonuses.

---

## Phase 9 — Documentation and submission readiness

Complete:

- README
- ADR required by the HTML
- walkthrough/demo documentation required by the HTML
- setup instructions
- architecture explanation
- testing evidence

Verify all mandatory requirements before attempting bonus features.

---

# 15. Testing Matrix

| Scenario | Expected result |
|---|---|
| Normal inventory change | No incorrect trigger |
| Stock below threshold | `INVENTORY_LOW` detected |
| Demand > 2× category average | `DEMAND_SPIKE` detected |
| Pricing suggestion | Correct structured recommendation |
| Reorder suggestion | Correct quantity |
| AI valid response | AI suggestion stored as PENDING |
| AI timeout | Rule fallback |
| AI quota/API failure | Rule fallback |
| Malformed AI JSON | Rule fallback |
| Duplicate trigger while pending | No duplicate pending suggestion |
| Accept pricing | Product price changes |
| Accept reorder | Required inventory action occurs |
| Reject suggestion | Product live state unchanged |
| Full automatic flow | Trigger → recommend → pending → approve/reject → act |

---

# 16. Bonus Priority

Only after all mandatory requirements are complete:

### Bonus: SSE

If time remains, implement the HTML's SSE streaming enhancement.

Do not sacrifice mandatory functionality, tests, documentation, or the demo for SSE.

---

# 17. OpenCode Rules

Every implementation prompt should follow these rules:

1. Read the relevant existing code before changing it.
2. Treat `docs/problem-statement.html` as the source of truth.
3. Implement only the current phase.
4. Do not implement future phases early.
5. Do not invent features.
6. Do not introduce unnecessary infrastructure.
7. Preserve working functionality.
8. Add/update tests for changed behavior.
9. Run tests/build after implementation.
10. Report files changed and test results.
11. If a requirement is ambiguous, identify the ambiguity instead of inventing behavior.
12. If the plan conflicts with the HTML, follow the HTML.

---

# 18. Definition of Done

StockPulse is ready when:

- [ ] Required domain entities exist
- [ ] Product APIs work
- [ ] Inventory/order flow works
- [ ] Rule-based strategy works
- [ ] AI strategy works
- [ ] Runtime strategy switching works
- [ ] Trigger reasons are correct
- [ ] Automatic detection works
- [ ] Recommendations are generated asynchronously
- [ ] Suggestions are persisted as PENDING
- [ ] Idempotency works
- [ ] Accept works
- [ ] Reject works
- [ ] Accepted price changes apply
- [ ] Accepted reorder action applies
- [ ] AI failures fall back to rules
- [ ] React merchandising console works
- [ ] Required tests pass
- [ ] README is complete
- [ ] ADR is complete
- [ ] Walkthrough/demo is ready

Only after this checklist is substantially complete should bonus SSE work begin.
