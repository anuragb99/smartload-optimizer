# SmartLoad Optimization API

REST API that selects the optimal combination of freight orders for a truck, maximising revenue while respecting weight, volume, hazmat, and route constraints.

**Stack:** Spring Boot 3.2 · Java 17 · In-memory only · Docker

---

## How to run

```bash
git clone https://github.com/anuragb99/smartload-optimizer.git
cd smartload-optimizer
docker compose up --build
# Service available at http://localhost:8080
```

---

## Health check

```bash
curl http://localhost:8080/actuator/health
```

---

## Example request

```bash
curl -X POST http://localhost:8080/api/v1/load-optimizer/optimize \
  -H "Content-Type: application/json" \
  -d @sample-request.json
```

**Expected response:**

```json
{
  "truck_id": "truck-123",
  "selected_order_ids": ["ord-001", "ord-002"],
  "total_payout_cents": 430000,
  "total_weight_lbs": 30000,
  "total_volume_cuft": 2100,
  "utilization_weight_percent": 68.18,
  "utilization_volume_percent": 70.0
}
```

---

## API

### `POST /api/v1/load-optimizer/optimize`

| Status | Meaning |
|--------|---------|
| 200    | Optimal combination found (may be empty if no feasible set exists) |
| 400    | Invalid input — see `details[]` in the response body |
| 413    | Payload exceeds 50 KB limit |
| 500    | Unexpected server error |

---

## Algorithm

Bitmask Dynamic Programming — O(n · 2ⁿ), safe for n ≤ 22 (≈ 88 M operations).

Each integer bitmask represents a subset of orders. The DP builds up cumulative weight, volume, and payout incrementally — adding one order at a time — and discards infeasible subsets early. The constraints checked per subset are:

- **Weight:** `totalWeight ≤ truck.max_weight_lbs`
- **Volume:** `totalVolume ≤ truck.max_volume_cuft`
- **Route:** all selected orders share the same origin **and** destination
- **Hazmat isolation:** hazmat orders cannot be combined with non-hazmat orders

Money is stored entirely in integer cents (`long`) — no floating-point arithmetic.

---

## Project layout

```
src/main/java/com/smartload/optimizer/
├── controller/         LoadOptimizerController   — REST layer
├── service/            LoadOptimizerService      — orchestration
├── algorithm/          LoadOptimizer (interface)
│                       BitmaskDPOptimizer        — core DP
├── model/              OptimizationResult        — internal
│   └── dto/            OptimizeRequest/Response, TruckDto, OrderDto, ErrorResponse
├── validator/          RequestValidator           — domain rules
└── exception/          InvalidRequestException, GlobalExceptionHandler
```

---

## Running tests locally

```bash
./mvnw test
```
