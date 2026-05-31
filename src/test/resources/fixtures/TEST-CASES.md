# SmartLoad Optimizer — Test Case Catalog

Fixtures live in this folder. Run against a running service:

```bash
curl -s -X POST http://localhost:8080/api/v1/load-optimizer/optimize \
  -H "Content-Type: application/json" \
  -d @src/test/resources/fixtures/n22-weight-bound-knapsack.json
```

## Assumptions (align with grader / your implementation)

| Rule | Definition used in expected answers below |
|------|-------------------------------------------|
| Route | Same `origin` **and** `destination` (case-insensitive, trimmed) |
| Hazmat | Never mix hazmat + non-hazmat on one truck |
| Time (pairwise) | **If implemented:** intervals `[pickup, delivery]` conflict when they overlap (inclusive dates). **Current code:** only per-order `pickup ≤ delivery` |
| Money | Integer `payout_cents`; utilization rounded to 2 decimals |
| n > 22 | **400** (bean `@Size(max=22)` on your project) |

---

## n = 22 scenarios

### TC-N22-01 — Weight-bound knapsack (`n22-weight-bound-knapsack.json`)

**Purpose:** Correctness at max n; weight is binding; greedy-by-payout alone would be wrong if items had different densities.

| Field | Value |
|-------|-------|
| Orders | 22 × (`weight=4000`, `volume=136`, same lane, non-hazmat) |
| Payout | `ord-XX` → `(index+1) × 10_000` cents (ord-00 = 10k … ord-21 = 220k) |
| Capacity | 44_000 lbs, 3_000 cuft |

**Expected optimal:** Pick **11** heaviest-payout orders `ord-11` … `ord-21` (only 11 fit by weight).

```json
{
  "truck_id": "truck-n22",
  "selected_order_ids": ["ord-11","ord-12","ord-13","ord-14","ord-15","ord-16","ord-17","ord-18","ord-19","ord-20","ord-21"],
  "total_payout_cents": 1870000,
  "total_weight_lbs": 44000,
  "total_volume_cuft": 1496,
  "utilization_weight_percent": 100.0,
  "utilization_volume_percent": 49.87
}
```

**Performance:** Response &lt; 800 ms on judge hardware; &lt; 2 s is assignment minimum.

---

### TC-N22-02 — All 22 fit (generate locally)

Same truck; each order: `weight_lbs=1000`, `volume_cuft=50`, payout `(i+1)*10000`, same lane.

**Expected:** All 22 selected; `total_payout_cents` = 2_530_000; `total_weight_lbs` = 22_000; `total_volume_cuft` = 1_100.

---

### TC-N22-03 — Hazmat partition at scale

11 non-hazmat (payout 50k each, 3k lbs) + 11 hazmat (payout 80k each, 3k lbs), same lane, truck 44k lbs.

- Best non-hazmat subset: 14 orders max by weight → 14×50k = 700k (but only 14×3k=42k… 14 orders = 42000 lbs, 14*50k=700k)
- Actually max 14 non-hazmat at 3k = 42k lbs → 14 orders = 700_000 cents
- Max hazmat: 14 at 80k = 1_120_000 cents → **expect all 11 hazmat** if 11×3k=33k: 11×80k = 880_000. Need tune so hazmat lane wins.

Use: 11 hazmat @ 25k lbs each won't fit. Better: 11 hazmat weight 3500 payout 200k → 11*3500=38500, total 2.2M vs 11 non-hazmat weight 3500 payout 100k → 1.1M → pick hazmat only.

---

## Edge cases (API + algorithm)

| ID | Fixture / input | Expected HTTP | Expected behavior |
|----|-----------------|---------------|-------------------|
| EC-01 | `sample-request.json` | 200 | `ord-001`, `ord-002`; payout 430_000; weight 30_000; volume 2_100 |
| EC-02 | `edge-greedy-trap.json` | 200 | **`ord-small-a` + `ord-small-b`** (4_000 cents), not `ord-big` (3_000) |
| EC-03 | `edge-multi-lane.json` | 200 | **`chi-1` only** (250k, fits); Dallas pair 190k together but 40k lbs; Chicago pair 490k but 50k lbs over |
| EC-04 | `edge-hazmat-vs-nonhazmat.json` | 200 | Same as sample: norm-1 + norm-2 (430k), exclude haz-1 |
| EC-05 | `edge-volume-bound.json` | 200 | **`vol-light-a` + `vol-light-b`** (8_000 cents, vol 100); not `vol-heavy` alone |
| EC-06 | `edge-time-overlap.json` | 200* | *If pairwise time enforced:* at most one of tw-a/tw-b/tw-c OR non-overlapping subset e.g. tw-a + tw-c; *current code:* all three (550k) |
| EC-07 | `orders: []` | **400** | Your `@NotEmpty` rejects; assignment text sometimes allows empty → clarify |
| EC-08 | Single order over capacity | 200 | `selected_order_ids: []`, zeros |
| EC-09 | Duplicate `id` in orders | 400 | "Duplicate order IDs" |
| EC-10 | `pickup_date` > `delivery_date` | 400 | Per-order date validation |
| EC-11 | 23 orders | 400 | `orders list exceeds maximum of 22` |
| EC-12 | Missing `truck` / null `payout_cents` | 400 | Bean validation |
| EC-13 | Malformed JSON | 400 | Malformed JSON |
| EC-14 | Origin case: `"los angeles, ca"` vs `"Los Angeles, CA"` | 200 | Should match same lane |
| EC-15 | All orders different lanes, each alone fits | 200 | Best single order by payout |
| EC-16 | All hazmat, same lane, all fit | 200 | Select all |
| EC-17 | Tie payout: two subsets same cents | 200 | Any valid max (document tie-break if required) |
| EC-18 | `payout_cents` near `Long.MAX_VALUE` | 200 | No overflow in sum for feasible n≤22 |

---

## Suggested JUnit additions

Mirror `BitmaskDPOptimizerTest` with:

1. Load `n22-weight-bound-knapsack.json` via `ObjectMapper` → assert exact ids and cents.
2. `edge-greedy-trap.json` → assert 4_000 payout.
3. `@Timeout(2)` on n=22 test for performance regression.

---

## Time-window conflict rule (for hidden tests)

Treat each order as inclusive interval `[pickup_date, delivery_date]`.

**Conflict:** orders A and B cannot both be selected if  
`A.pickup <= B.delivery AND B.pickup <= A.delivery`.

**EC-06 detail:**

- tw-a: [05, 09], tw-b: [07, 11] → **overlap** → incompatible pair
- tw-a + tw-c: [05,09] and [10,14] → **no overlap** → compatible (550k if no other rule)
- tw-b + tw-c: [07,11] and [10,14] → overlap on 10–11 → incompatible

Implement this before relying on EC-06 expected output.
