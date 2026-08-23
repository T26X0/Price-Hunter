# Price Hunter backend

Minimal Java 21 / Spring Boot backend for a price-comparison service.

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
