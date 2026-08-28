# Price Hunter

Price-comparison service with a Java 21 / Spring Boot backend and a separate React / TypeScript frontend.

## Run locally

1. Start PostgreSQL: `docker compose up -d postgres`
2. Run the app: `mvn spring-boot:run`
3. The API is available at `http://localhost:8080/api/products`.

The checked-in database values are development-only defaults. Override them in any shared or deployed environment:

```bash
export DB_URL='jdbc:postgresql://localhost:5432/price_hunter'
export DB_USERNAME='price_hunter'
export DB_PASSWORD='your-local-password'
```

## API

Create a product:

```bash
curl -X POST http://localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Wireless headphones","sku":"HEADPHONES-001","description":"Over-ear Bluetooth headphones"}'
```

List products: `curl http://localhost:8080/api/products`

Run checks: `mvn test`

## Database model

Flyway migrations `V5` and `V6` separate the reusable catalog from city- and branch-specific retail data:

- `retail_chains` — a chain such as 941;
- `chain_city_markets` — the chain's commercial market in one city and sales channel;
- `store_locations` — physical branches belonging to that market;
- `product_models` — one shared model such as iPhone 16 Pro;
- `product_variants` and `product_variant_attributes` — memory, color and other filterable characteristics;
- `offers` — the current price, condition, availability and product URL for one city market or one branch and variant;
- `offer_terms` — gifts, discounts, instalments, credit and other conditions;
- `shipping_quotes` — delivery price from an offer to a destination city;
- `offer_state_history` — change-only price and availability intervals used by charts;
- `offer_monthly_stats` — monthly aggregates retained after detailed history expires.

The current state is kept separately from history so product cards do not scan the time series. Daily history is retained for six months. Older closed intervals can be aggregated and pruned with `HistoryRetentionService`; intervals crossing the retention boundary are split first, so charts retain an exact starting state.

Repositories expose narrow projections and `Slice`-based reads for parser queues, product cards, charts, inventory and delivered-price comparison. The ingestion services lock the current row while updating it and append a history interval only when its state hash changes.

## Parser pipeline

The first working adapter reads the public iPhone 16 Pro catalog from 941. It uses stable Schema.org fields for the product URL, name, price, currency, availability and condition; memory, SIM type, colour, old price and promotion labels are normalized separately. A response with no complete product cards fails the job instead of silently treating every product as unavailable.

The ingestion pipeline keeps one product model, creates separate filterable variants, and stores city-wide or branch-specific offers. Stable source links and database unique indexes prevent repeated scans from duplicating models, variants, offers or price history. A history row is added only when the price, availability or commercial terms change.

Ambiguous product matches, unidentified branches, accessories and non-technical positions are saved in `review_candidates` for manual review. Confident matches are imported automatically.

Connectors are selected by source type, connector mode and key. `HTML`, `BROWSER` and `API` are separate modes behind the same catalog/store-discovery contracts, so a source can later move to an official API without changing ingestion or deduplication.

The schedule policy is:

- prices every day;
- product discovery every seven days;
- store discovery every fourteen days.

Scheduling is disabled by default until parser sources and the required city market exist. Enable it with `PARSER_SCHEDULING_ENABLED=true`; `PARSER_POLL_CRON` controls how often due work is checked. The HTTP client accepts only public HTTPS sources, follows `robots.txt`, observes crawl delay, limits response size and does not follow redirects.

Run the deterministic parser tests with `mvn test`. The PostgreSQL end-to-end scenario is enabled with `RUN_DB_INTEGRATION_TESTS=true`. A live smoke test against 941 is deliberately opt-in:

```bash
RUN_PARSER_LIVE_TESTS=true \
mvn -Dtest=NineFortyOneCatalogConnectorLiveTest test
```

Yandex, Google, Yandex Maps and 2GIS currently have connector contracts and source types, but their browser implementations are intentionally separate future adapters. No CAPTCHA bypass is part of the pipeline.

To run the PostgreSQL integration scenario against a disposable database:

```bash
RUN_DB_INTEGRATION_TESTS=true \
DB_URL='jdbc:postgresql://localhost:5432/price_hunter_integration' \
DB_USERNAME='price_hunter' \
DB_PASSWORD='your-local-password' \
mvn -Dtest=DatabaseArchitectureIntegrationTest test
```

## Frontend

The React client lives in [`frontend`](frontend). Start the backend first, then follow [`frontend/README.md`](frontend/README.md). During local development Vite proxies `/api` requests to `http://localhost:8080`.
