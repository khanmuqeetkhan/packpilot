# packing-trip-service

Spring Boot REST API that serves the web UI, asks
`packing-generator-service` for a packing-list draft, and persists saved
trips, together with their checklists, in MongoDB as plain JSON documents.
It is the only PackPilot component reachable from outside the cluster.

## Responsibility

Everything stateful: saving a generated draft under a trip name, listing
and searching saved trips, renaming or deleting a trip, and editing a
saved trip's checklist item by item (toggling checked, changing quantity
or note, adding or removing an item). List generation itself is delegated
to `packing-generator-service` over REST, through `GeneratorClient`; this
service implements no packing rules of its own.

## API

All endpoints are under `/packing-lists`.

| Method | Path | Purpose |
|---|---|---|
| POST | `/generate` | Ask the generator service for a draft checklist. Nothing is persisted yet. |
| POST | `` | Save a (possibly edited) draft under a trip name. |
| GET | `` | List saved trips. Optional `?climate=` or `?search=` (trip-name substring) filters. |
| GET | `/{id}` | Fetch one saved trip. |
| PUT | `/{id}` | Rename a saved trip. |
| DELETE | `/{id}` | Delete a saved trip. |
| POST | `/{id}/items` | Add a custom item to a saved trip. |
| PUT | `/{id}/items/{itemId}` | Update an item's checked state, quantity, or note. |
| DELETE | `/{id}/items/{itemId}` | Remove an item. |

A missing trip or item id returns `404 Not Found`. The static files under
`src/main/resources/static` (`index.html`, `app.js`, `style.css`) are the
browser UI that calls these endpoints; Spring Boot serves them directly,
so no separate frontend build step is needed.

## Trying the API

`requests.http` has ready-to-run sample requests for every endpoint above
(IntelliJ's built-in HTTP client, opened directly from this folder),
including the save-a-trip request chaining into the id-based requests
that follow it.

## Configuration

Read from environment variables, all with local-dev defaults (see
`application.yml`):

| Variable | Default | Purpose |
|---|---|---|
| `MONGO_HOST` | `localhost` | MongoDB host |
| `MONGO_PORT` | `27017` | MongoDB port |
| `MONGO_DATABASE` | `packpilot` | Database name |
| `MONGO_USERNAME` / `MONGO_PASSWORD` | empty | Set only when MongoDB requires auth; `MongoConfig` connects without credentials when these are blank |
| `MONGO_AUTH_DATABASE` | `admin` | Authentication database, used only when credentials are set |
| `GENERATOR_SERVICE_URL` | `http://packing-generator-service:8080` | Base URL for `packing-generator-service` |

In the Kubernetes deployment these come from `k8s/02-app-config.yaml`
(ConfigMap) and `k8s/01-mongo-secret.yaml` (Secret), injected through
`envFrom`.

## Running it standalone

Needs a reachable MongoDB and a reachable `packing-generator-service`:

```bash
# a throwaway MongoDB, e.g.
podman run --rm -p 27017:27017 mongo:7

# in another terminal, with packing-generator-service already running on 8080
MONGO_HOST=localhost GENERATOR_SERVICE_URL=http://localhost:8080 \
  mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Both services default to port 8080, so run this one on a different port
when the two are not in separate containers and would otherwise collide on
the same host.

## Tests

```bash
mvn test
```

Unit tests mock `PackedTripRepository` and `GeneratorClient` directly, so
they exercise the controller's logic (save, list/search, rename, delete,
item CRUD) without a live MongoDB or a live generator service.

## Building the image and deploying with the rest of the stack

Building the container image, loading it into a local `minikube` cluster
running on Podman, and deploying the full stack are documented once in the
main [README](../README.md) rather than repeated here.
