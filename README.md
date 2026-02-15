# Product Information Aggregator

A backend service that aggregates product information from multiple upstream services into a single, market-aware response for a B2B e-commerce platform.

## How to Run

### Prerequisites
- Java 25+
- Maven 3.9+ (or use the included Maven wrapper)

### Start the application

```bash
./mvnw spring-boot:run
```

The service starts on `http://localhost:8080`.

### Run tests

```bash
./mvnw test
```

### Example requests

```bash
# Full request with customer context
curl "http://localhost:8080/api/v1/products/PART-001?market=nl-NL&customerId=DEALER-001"

# Without customer (non-personalized)
curl "http://localhost:8080/api/v1/products/PART-001?market=de-DE"

# Polish market
curl "http://localhost:8080/api/v1/products/PART-002?market=pl-PL&customerId=FLEET-001"
```

### Available mock data

| Product ID | Description            |
|-----------|------------------------|
| PART-001  | Oil Filter (John Deere 6030) |
| PART-002  | Hydraulic Filter       |
| PART-003  | V-Belt                 |

| Customer ID  | Segment  | Discount |
|-------------|----------|----------|
| DEALER-001  | DEALER   | 10%      |
| DEALER-002  | DEALER   | 10%      |
| WORKSHOP-001| WORKSHOP | 5%       |
| FLEET-001   | FLEET    | 15%      |

Supported markets: `nl-NL`, `de-DE`, `pl-PL`, `en-GB`, `fr-FR`

### Health check

```bash
curl http://localhost:8080/actuator/health
```

Returns circuit breaker status for all upstream services.

## Key Design Decisions

### 1. Two-phase aggregation: critical vs optional

The core insight is that not all upstream services are equal. The Catalog Service is **critical** — without product name and description, there's nothing to show. The other three (Pricing, Availability, Customer) are **optional** — the response degrades gracefully without them.

`ProductFacade` reflects this directly:
- **Phase 1**: Synchronous catalog fetch. Failure throws `CatalogUnavailableException` (503).
- **Phase 2**: Parallel fan-out of optional services via `AggregatorService`. Failures are absorbed and reflected as status enums (`UNAVAILABLE`, `UNKNOWN_STOCK`, `NON_PERSONALIZED`).

### 2. FetchService plugin pattern

All optional services implement `FetchService<T extends FetchResult>`. `AggregatorService` receives `List<FetchService<?>>` via Spring constructor injection and fans them out in parallel.

### 3. CompletableFuture

I chose `CompletableFuture.supplyAsync()` with `.orTimeout()`
The timeout is configurable via `application.yaml` (`app.timeout.service-timeout-millis: 150`)

### 4. Resilience strategy

**All services** (including Catalog) have a Resilience4j **Circuit Breaker** that opens after 50% failure rate over 10 calls, waits 30s before half-open.

Each optional service additionally has:
- **Timeout**: configurable via `CompletableFuture.orTimeout()` (`app.timeout.service-timeout-millis`)
- **Fallback**: Returns null/empty DTO so the response can still be built
- **Isolation**: `AggregatorService.callSafely()` catches exceptions per service

The Catalog Service circuit breaker (`catalogCB`) uses slow-call detection — if calls consistently exceed the threshold, the circuit opens. Its fallback re-throws as `CatalogUnavailableException` (503) since catalog is critical.

### 5. Mock service realism

Each mock client simulates:
- **Latency**: Base delay + random jitter (Catalog 50ms, Pricing 80ms, Availability 100ms, Customer 60ms)
- **Failures**: Random failures matching specified reliability (98%–99.9%)
- **Data-driven**: Products, pricing, and customers loaded from JSON files at startup

### 6. Market localization

`catalog.json` stores product names and descriptions per locale. `MockCatalogClient` uses a private `CatalogTemplate` record to deserialize the locale maps and resolves the correct translation at fetch time, with `en-GB` fallback. Pricing applies market-specific exchange rates. Availability maps markets to regional warehouses.

## Trade-offs

| Decision | Benefit                                                         | Cost |
|----------|-----------------------------------------------------------------|------|
| `CompletableFuture` over reactive (WebFlux) | Simpler work model, easier debugging                            | Thread-per-request model, less efficient under extreme load |
| Mock clients as `@Component` beans | Easy to swap for real HTTP clients (Feign/RestClient)           | Mocks live in production source tree |
| No retry pattern | Simpler flow, circuit breaker already handles repeated failures | Transient single-call failures aren't retried |
| Status enums in `ProductResponse` | Explicit service health per field, clear API contract           | Slightly more verbose response structure |
| Catalog as synchronous call with circuit breaker | Clear failure semantics, simpler error propagation              | Adds latency before parallel fan-out starts |

## What I Would Do Differently With More Time

- **Add Timeout for Catalog service** (could be implemented with Feign client)
- **Integration tests** to test the full request flow including validation and error handling
- **Add WireMock** for test calls similar to production (Feign/RestClient)
- **Add observability** with correlation IDs for tracing requests across service calls
- **Rate limiting** on the aggregator endpoint to protect upstream services
- **API documentation** with SpringDoc/OpenAPI


## Design Question: Adding a "Related Products" service (200ms latency, 90% reliability)

The Related Products service should be **optional**, not required.

### Why optional?

- **90% reliability** means 1 in 10 calls will fail — far too unreliable for a required dependency. Making it required would degrade the aggregator's own availability to ~90%, which is unacceptable for a B2B platform.
- **200ms latency** is the highest among all services, but since optional services run in parallel, it only adds ~200ms to the total response time (not stacked on top of other services).
- **Related products are supplementary** — a dealer can still view pricing, availability, and place an order without seeing recommendations.

### How the current design accommodates it

Thanks to the `FetchService` plugin pattern, adding a new optional service requires just 3 steps:

1. Create a `RelatedProductsDto` implementing `FetchResult`
2. Create a `RelatedProductsService` implementing `FetchService` with `@CircuitBreaker` and timeout
3. Add a mapping method in `ProductResponseMapper`

Spring auto-discovers the new `@Service` and injects it into `AggregatorService`'s `List<FetchService<?>>`. No changes needed to `AggregatorService`, `ProductFacade`, or any existing service.

### Resilience considerations

Given the 90% reliability, the circuit breaker should be tuned more aggressively. The fallback returns null, so the response simply omits the related products section when the service is unavailable.
