# StockPulse — AI Inventory & Dynamic Pricing Engine

StockPulse is an autonomous, event-driven commerce advisor that closes the loop between real-time inventory signals, AI-driven price/replenishment recommendations, and human merchandising approval.

```
+-----------------------------------------------------------------------------------+
|                            THE AGENTIC COMMERCE LOOP                              |
|                                                                                   |
|  [ 1. Inventory Signal ]  --->  [ 2. Auto-Detection ]  ---> [ 3. AI / Rule Engine ] |
|  - Sale / Simulated Order       - Low Stock Threshold        - Dynamic Price Rec  |
|  - Stock Level Change           - Demand Velocity Spike      - Reorder Batch Rec  |
|                                                                     |             |
|                                                                     v             |
|  [ 5. Live State Action ]  <---  [ 4. Human Approval ] <--- [ PENDING Suggestion] |
|  - Price updated on Accept       - Merchandising Console     - Confidence Score   |
|  - Inbound stock increment       - Approve or Reject         - Reasoning Text     |
+-----------------------------------------------------------------------------------+
```

---

## 1. Overview
ShopStream operates a multi-category online store with SKUs across Electronics, Apparel, and Home goods. StockPulse monitors real-time inventory levels and order velocity, automatically proposes price changes and replenishment orders when inventory or demand thresholds are crossed, and presents proposals in a real-time console for merchandiser approval.

---

## 2. Problem Being Solved
Prices in traditional retail are reviewed manually on weekly schedules via spreadsheets and emails. When demand surges or stock runs critically low:
- Fast-selling items run out of stock while prices remain flat, leaving gross margin on the table.
- Stock depletions fail silently when no operator is actively monitoring the catalog.
- Merchandisers debate price adjustments over email instead of acting on immediate inventory signals.

StockPulse automates this reactive loop with sub-second event detection, structured AI reasoning, and a human-in-the-loop approval checkpoint.

---

## 3. The Detect → Recommend → Approve → Act Flow
1. **Detect**: An order or stock adjustment changes inventory. The system evaluates whether `stock < reorderThreshold` (`INVENTORY_LOW`) or `demandVelocity > 2x category baseline` (`DEMAND_SPIKE`).
2. **Recommend**: The active commerce strategy (Rule-Based or AI Advisor) generates a price adjustment and/or reorder batch proposal with confidence metrics and plain-English rationale.
3. **Approve**: Suggestions are stored with status `PENDING` and rendered in the Merchandising Console with distinct trigger badges. Merchandisers can **Approve** or **Reject**.
4. **Act**:
   - Accepting a pricing suggestion atomically updates `Product.currentPrice`.
   - Accepting a reorder suggestion atomically increments `Product.stockLevel` (simulated inbound shipment).
   - Rejecting a suggestion updates its status to `REJECTED` and leaves live product state unchanged.

---

## 4. Features Actually Implemented
- **Domain Entity Model & State Machines**: `Product`, `PricingSuggestion`, `ReorderSuggestion`, `InventorySnapshot` with explicit lifecycle transitions (`ACTIVE -> PRICE_REVIEW_PENDING -> ACTIVE`, `OUT_OF_STOCK`).
- **Pluggable Commerce Strategy Engine**:
  - `RuleBasedPricingStrategy`: +10% price increase when stock < threshold; +5% price increase when velocity > 2x category average; HOLD otherwise.
  - `RuleBasedReorderStrategy`: `(reorderThreshold * 3) - currentStock` (minimum 1).
  - `CommerceStrategyManager`: Instant zero-downtime runtime switching between `RULE_BASED` and `AI` via API or console UI.
- **AI Commerce Advisor**:
  - `LLMGateway` supporting Google Gemini (1.5 Flash), Groq (Llama 3.1), Ollama, OpenAI-compatible endpoints, and local fallback.
  - Distinct trigger-specific prompts for `INVENTORY_LOW` (tradeoff between price protection and clearance markdown) and `DEMAND_SPIKE` (surge pricing without gouging).
  - Output bounds validation (`0.2x <= price <= 3.0x`, `qty >= 1`) with graceful fallback to deterministic rule strategies on timeouts, quota errors, or malformed JSON.
- **Asynchronous Agentic Loop**:
  - Event-driven decoupling using Spring `ApplicationEventPublisher` and `@Async @EventListener`.
  - Non-blocking order and stock updates (<10ms response times).
  - Repository-level idempotency preventing duplicate `PENDING` suggestions for the same product, trigger, and type.
- **Real-Time Merchandising Console (React + Vite)**:
  - 3-second live polling with manual refresh option.
  - Strategy toggle switcher (`⚡ Rule-Based` vs `✨ AI Advisor`).
  - Pending recommendation cards with trigger badges, rationale, confidence meters, and Accept/Reject buttons.
  - Full product catalog with stock health gauges, demand velocity surge badges, instant sale simulation buttons (`⚡ Order 1`, `⚡⚡ Order 5`), and manual advice triggers.
- **Bonus SSE Token Streaming**:
  - `POST /products/{id}/suggest-pricing/stream` streams AI reasoning tokens progressively via Server-Sent Events (`text/event-stream`).

---

## 5. Technology Stack Actually Used
- **Backend**: Java 17+, Spring Boot 3.2.4 (Spring Web, Spring Data JPA, Spring Validation, Spring Actuator, Lombok)
- **Build Tool**: Apache Maven
- **Database**: H2 in-memory (`jdbc:h2:mem:stockpulse`) with H2 web console
- **Frontend**: React 19, Vite 8, Vanilla CSS3 (responsive grid/flexbox)
- **Testing**: JUnit 5, Mockito, AssertJ, Spring Boot Test, Spring MVC Test (MockMvc)

---

## 6. Project Structure
```text
StockPulse/
├── docs/                           # Persistent project documentation
│   ├── problem-statement.html      # STRICT source of truth
│   ├── PROJECT_CONTEXT.md
│   ├── IMPLEMENTATION_STATUS.md
│   ├── ARCHITECTURE.md
│   ├── DECISIONS.md
│   └── CHANGELOG.md
├── backend/                        # Spring Boot backend application
│   ├── pom.xml
│   ├── .env.example
│   └── src/
│       ├── main/java/com/stockpulse/
│       │   ├── StockpulseBackendApplication.java
│       │   ├── ai/                 # LLMGateway, AiCommerceAdvisor, AI strategies
│       │   ├── bootstrap/          # DataSeeder (Addendum A catalog)
│       │   ├── config/             # WebConfig (CORS), AsyncConfig (@EnableAsync)
│       │   ├── domain/             # Entities (Product, Suggestions, Snapshots) & Enums
│       │   ├── dto/                # Request & Response DTOs
│       │   ├── engine/             # Strategy abstractions & Rule-Based strategies
│       │   ├── event/              # ProductInventoryChangedEvent
│       │   ├── exception/          # GlobalExceptionHandler & custom exceptions
│       │   ├── loop/               # AgenticRecommendationListener (Async loop)
│       │   ├── repository/         # Spring Data JPA repositories
│       │   ├── service/            # ProductService, SuggestionService
│       │   └── web/                # REST Controllers
│       ├── main/resources/
│       │   └── application.properties
│       └── test/java/com/stockpulse/ # 27 Unit, Integration & E2E Tests
├── frontend/stockpulse/            # React + Vite frontend application
│   ├── package.json
│   ├── vite.config.js
│   ├── index.html
│   ├── .env.example
│   └── src/
│       ├── main.jsx
│       ├── App.jsx                 # Merchandising Console & Simulation Workbench
│       ├── App.css, index.css
│       └── services/api.js         # API integration client
├── ADR.md                          # Architecture Decision Record
└── README.md                       # Root submission documentation
```

---

## 7. Prerequisites
- **JDK 17 or JDK 21** installed and configured on `PATH`
- **Apache Maven 3.8+** installed and configured on `PATH`
- **Node.js 18+** & **npm 9+** installed and configured on `PATH`
- Modern web browser (Chrome, Firefox, Edge, Safari)

---

## 8. Environment Variables & Setup Instructions

### Summary Table

| Variable Name | Component | Required? | Default / Demo Value | Description |
|---|---|---|---|---|
| `LLM_PROVIDER` | Backend | Optional | `gemini` | AI provider (`gemini`, `groq`, `ollama`, `openai`, `mock`) |
| `LLM_API_KEY` | Backend | Optional | *(empty)* | API key for Gemini or Groq. If omitted, uses built-in generator & rule fallbacks |
| `LLM_MODEL` | Backend | Optional | `gemini-1.5-flash` | Model identifier |
| `LLM_BASE_URL` | Backend | Optional | `https://generativelanguage.googleapis.com` | Base API URL |
| `PORT` / `server.port` | Backend | Optional | `8080` | Backend HTTP server port |
| `VITE_API_BASE_URL` | Frontend | Optional | `http://localhost:8080` | Backend API URL for frontend |

> [!NOTE]
> **Zero Configuration Required**: The system runs completely out-of-the-box with **zero required environment variables**. When no API key is provided, the AI advisor produces structured demo reasoning and seamlessly falls back to deterministic rule strategies.

### Optional: Configuring a Real AI Provider

#### Option A: Google Gemini
```powershell
$env:LLM_PROVIDER="gemini"
$env:LLM_API_KEY="your-gemini-api-key"
$env:LLM_MODEL="gemini-1.5-flash"
$env:LLM_BASE_URL="https://generativelanguage.googleapis.com"
```

#### Option B: Groq + Llama 3.1
```powershell
$env:LLM_PROVIDER="groq"
$env:LLM_API_KEY="gsk_your_groq_key"
$env:LLM_MODEL="llama-3.1-8b-instant"
$env:LLM_BASE_URL="https://api.groq.com"
```

#### Option C: Local Ollama
```powershell
$env:LLM_PROVIDER="ollama"
$env:LLM_MODEL="llama3.1"
$env:LLM_BASE_URL="http://localhost:11434"
```

---

## 9. Backend Setup Commands

Open a Windows PowerShell terminal:
```powershell
cd C:\StockPulse\backend
mvn clean compile
```

---

## 10. Frontend Setup Commands

Open a second Windows PowerShell terminal:
```powershell
cd C:\StockPulse\frontend\stockpulse
npm install
```

---

## 11. Commands to Run Tests

### Run Full Backend Test Suite (27 Tests)
```powershell
cd C:\StockPulse\backend
mvn test
```

---

## 12. Commands to Build

### Backend Build
```powershell
cd C:\StockPulse\backend
mvn clean package -DskipTests
```

### Frontend Build
```powershell
cd C:\StockPulse\frontend\stockpulse
npm run build
```

---

## 13. How to Run the Complete Application

### Step 1: Start Backend (Terminal 1)
```powershell
cd C:\StockPulse\backend
mvn spring-boot:run
```

### Step 2: Start Frontend (Terminal 2)
```powershell
cd C:\StockPulse\frontend\stockpulse
npm run dev
```

---

## 14. URLs
- **Backend API**: `http://localhost:8080`
- **H2 Database Console**: `http://localhost:8080/h2-console`
- **Frontend Merchandising Console**: `http://localhost:5173`

---

## 15. Database Information
- **Engine**: In-Memory H2 Database
- **JDBC URL**: `jdbc:h2:mem:stockpulse`
- **Username**: `sa`
- **Password**: `password`
- **Pre-Seeded Data**: 8 Addendum A products automatically seeded via `DataSeeder.java` on application boot:
  - `PRD-001` (`SKU-ELEC-001`): Wireless Earbuds Pro (Stock: 45, Threshold: 20)
  - `PRD-002` (`SKU-ELEC-002`): USB-C Hub 7-Port (Stock: 120, Threshold: 30)
  - `PRD-003` (`SKU-APP-001`): Organic Cotton T-Shirt (Stock: 8, Threshold: 15 — **Low Stock Demo Item**)
  - `PRD-004` (`SKU-APP-002`): Running Shorts — Navy (Stock: 55, Threshold: 20)
  - `PRD-005` (`SKU-HOME-001`): Ceramic Pour-Over Set (Stock: 22, Threshold: 10)
  - `PRD-006` (`SKU-HOME-002`): LED Desk Lamp — Dimmable (Stock: 0, Threshold: 15, Status: OUT_OF_STOCK)
  - `PRD-007` (`SKU-ELEC-003`): Portable Charger 20K (Stock: 18, Threshold: 25)
  - `PRD-008` (`SKU-APP-003`): Hoodie — Heather Grey (Stock: 11, Threshold: 12, Velocity: 15 — **Demand Spike Demo Item**)

---

## 16. Implemented API Endpoints

### Product Management
- `GET /products`: List products with optional filters (`?status=ACTIVE&category=ELECTRONICS`).
- `GET /products/{id}`: Get product details by ID.
- `POST /products`: Create a new product with validation.
- `PATCH /products/{id}/stock`: Update stock level; fires `INVENTORY_LOW` if below threshold.
- `POST /products/{id}/orders`: Simulate a sale; decrements stock, increments velocity, and fires triggers.
- `POST /products/{id}/suggest-pricing`: Generate on-demand pricing suggestion.
- `POST /products/{id}/suggest-pricing/stream`: **Bonus** SSE stream of AI reasoning tokens.
- `POST /products/{id}/suggest-reorder`: Generate on-demand reorder suggestion.

### Suggestion Decision Lifecycle
- `GET /pricing-suggestions`: List pricing suggestions (`?status=PENDING&productId=...`).
- `GET /pricing-suggestions/{id}`: Get pricing suggestion by ID.
- `PATCH /pricing-suggestions/{id}`: Approve (`status: "ACCEPTED"`) or Reject (`status: "REJECTED"`). Accepting updates live product price.
- `GET /reorder-suggestions`: List reorder suggestions (`?status=PENDING&productId=...`).
- `GET /reorder-suggestions/{id}`: Get reorder suggestion by ID.
- `PATCH /reorder-suggestions/{id}`: Approve (`status: "ACCEPTED"`) or Reject (`status: "REJECTED"`). Accepting increments stock.

### Commerce Strategy Management
- `GET /commerce/strategy`: Get currently active strategy (`RULE_BASED` or `AI`).
- `POST /commerce/strategy`: Switch strategy at runtime (`{ "strategy": "AI" }`).

---

## 17. Demo / Testing Walkthrough

### Scenario 1: Low Inventory Autonomous Trigger & Price Approval
1. Open `http://localhost:5173`.
2. Locate **Organic Cotton T-Shirt** (`SKU-APP-001`), seeded with stock `8` and threshold `15`.
3. Click **⚡ Order 1**.
4. Stock drops to `7`. The system asynchronously generates `INVENTORY_LOW` pricing and reorder suggestions.
5. Review the proposals in the top section showing rationale and confidence.
6. Click **✓ Approve Price Change**: The live price in the catalog updates immediately.
7. Click **✓ Approve Reorder**: Stock increments by the replenishment quantity.

### Scenario 2: Demand Velocity Surge (Viral Spike) & Rejection Safety
1. Locate **Hoodie — Heather Grey** (`SKU-APP-003`).
2. Click **⚡⚡ Order 5** multiple times to trigger a demand velocity surge.
3. The system generates a `DEMAND_SPIKE` recommendation card.
4. Click **✕ Reject**: The suggestion status becomes `REJECTED`, and the product's catalog price remains unchanged (demonstrating human-in-the-loop safety).

### Scenario 3: Zero-Downtime Strategy Switching
1. In the header bar, click **✨ AI Advisor** or **⚡ Rule-Based**.
2. The active strategy updates immediately across all subsequent recommendations without restarting the application.

---

## 18. AI / LLM Configuration & Fallback Behavior
- **Prompt Design**: Trigger context is explicitly passed to the LLM. Low stock prompts instruct the model to evaluate the trade-off between price protection vs clearance markdowns; demand spike prompts instruct the model to capitalize on willingness to pay without price gouging.
- **Bounds Checking**: Sane boundaries are enforced: `0.2x currentPrice <= recommendedPrice <= 3.0x currentPrice` and `recommendedQuantity >= 1`.
- **Automatic Fallback**: If the LLM call fails for any reason (timeout, quota, malformed JSON, offline), `AiCommerceAdvisor` automatically executes the deterministic rule strategy, logs the event, and prefixes the reasoning with `[AI Fallback to Rules]`. Suggestions are never dropped.

---

## 19. Architecture Decision Record (ADR)
Detailed architectural rationale, evaluated options, and trade-offs are documented in [`ADR.md`](./ADR.md).

---

## 20. Limitations & Sprint 2/3 Roadmap
- **Sprint 1 Focus**: Inventory-signal → recommendation → human approval loop.
- **Sprint 2 Extensions (Seams in place)**: `costPrice` on `Product` for margin floors, `suggestedLeadTimeDays` on `ReorderSuggestion` for supplier catalogs, and `StrategyType.COMPETITOR_AWARE` for competitor scraping.
- **Sprint 3 Extensions**: Automated purchase order generation and customer storefront pricing display.
