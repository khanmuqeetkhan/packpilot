# packing-generator-service

Stateless REST API that turns a climate, trip length, traveler counts, and
a list of activities into a categorized packing checklist. It holds no
state of its own and does not talk to any database: every call is a pure
function of its input.

## Responsibility

Rule-based list generation only. Saving a generated list, checking items
off, or editing them afterwards is handled by `packing-trip-service`,
which calls this service as a REST client. See the main
[README](../README.md) for how the two services and MongoDB fit together.

## API

### `POST /packing-lists/generate`

Request body:

```json
{
  "climate": "tropical",
  "tripLengthDays": 7,
  "adults": 2,
  "children": 1,
  "toddlers": 0,
  "activities": ["beach", "hiking"]
}
```

- `climate` (required): one of `cold`, `mild`, `warm`, `tropical`,
  `variable`. Case-insensitive.
- `tripLengthDays` (required): between 1 and 60.
- `adults`, `children`, `toddlers` (optional, default to 0 when omitted).
- `activities` (optional): any combination of `beach`, `hiking`,
  `business`, `winter_sports`, `formal_event`. Bound to the API-layer enum
  `ApiActivities`, kept separate from the internal `Activity` enum in
  `model` so the domain model is never exposed on the request type
  directly; `PackingGenerationService` maps one to the other.

An unknown climate or activity value returns `400 Bad Request` with a
message naming the allowed values (`GlobalExceptionHandler`).

Response body: a list of packing items grouped by category (clothing,
toiletries, documents, electronics, misc), each with a name, a quantity,
and an optional note.

### `GET /actuator/health`

Liveness and readiness probe used by Kubernetes (`k8s/04-packing-generator.yaml`).

## Generation rules

Implemented in `PackingGenerationService`: base essentials for every trip,
climate-driven clothing (thermal layers for `cold`, swimwear for
`tropical`/`warm`, and so on), activity-driven additions (hiking boots,
business attire, winter sports gear, formal wear), and scaling by trip
length and traveler count, including diapers for toddlers.
`PackingListAccumulator` merges items that land in the same category under
the same name instead of listing duplicates. There is no randomness and no
I/O: the same input always produces the same output, which is what lets
the unit tests below be exhaustive without a running server.

## Trying the API

`requests.http` has ready-to-run sample requests for the endpoint above
(IntelliJ's built-in HTTP client, opened directly from this folder).

## Running it standalone

```bash
mvn spring-boot:run
```

Starts on port 8080. No environment variables are required since this
service has no external dependencies.

## Tests

```bash
mvn test
```

Unit tests cover the default case, toddler-specific scaling, climate and
activity combinations that should merge rather than duplicate items, and
rejection of unknown climate or activity values.

## Building the image and deploying with the rest of the stack

Building the container image, loading it into a local `minikube` cluster
running on Podman, and deploying it alongside `packing-trip-service` and
MongoDB involve more than this one service, so those steps are documented
once in the main [README](../README.md) rather than repeated here.
