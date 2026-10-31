# PackPilot

A small microservice application for the assignment packing-checklist generator
for trips, with a saved-trip library to check items off and tweak as you go.

## Components

| Component                   | Type                                           | Role                                                                                                                                                                         |
|-----------------------------|------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `packing-generator-service` | Spring Boot REST API, stateless                | Generates a categorized packing checklist from climate, trip length, traveler counts, and activities. No database access.                                                    |
| `packing-trip-service`      | Spring Boot REST API, stateless                | Serves the web UI, calls `packing-generator-service` as a REST client, and persists/retrieves saved trips in MongoDB. The only component reachable from outside the cluster. |
| MongoDB                     | Database (StatefulSet + PersistentVolumeClaim) | Stores saved trips and their checklists as plain JSON documents. Single replica; not required to scale.                                                                      |

See `docs/` for software description, architecture
design, and the benefits/challenges/security log. Each service also has
its own README for its API contract, configuration, and how to run and
test it on its own: [`packing-generator-service/README.md`](packing-generator-service/README.md)
and [`packing-trip-service/README.md`](packing-trip-service/README.md).

## Tools used

- **Kubernetes**, the container-orchestration platform this application is
  built to run on. A Kubernetes cluster is one or more machines (nodes)
  managed by a control plane (an API server plus a scheduler and a few
  controllers) that keeps a declarative set of YAML manifests actually
  running: creating containers, restarting ones that crash, routing
  traffic between them, and so on, rather than any of that being done by
  hand.
- **Podman** for building and running the two application images. No
  images are pushed to Docker Hub by anything here: that step is  (`podman tag` + `podman push`).
- **minikube**, which has a built-in Podman driver, for a local, real
  Kubernetes cluster to deploy into. This is the same local-cluster
  approach taught in previous sessions (there run with the default driver on
  Windows/Mac Docker Desktop, or with minikube directly on Linux); here it
  is pointed at Podman instead.
- **kubectl**, the command-line client used to talk to that cluster's
  control plane, specifically its API server. Every `kubectl apply -f
  ...` below sends a manifest to it as a request for that desired state
  to exist; the control plane's own controllers then do the actual work
  of making it so.

Install `minikube` and `kubectl` once:

```bash
brew install minikube kubectl
```

## 1. Create a local Kubernetes cluster with minikube

```bash
minikube start -p packpilot --driver=docker --container-runtime=containerd --cni=bridge
```

Docker Desktop hosts the cluster's single node here; `--container-runtime=containerd`
keeps the runtime that actually runs the application's Pods off Docker. 

The driver itself did start out as Podman, but its
networking on this machine turned out not to route Pod traffic to Service
IPs at all (confirmed with a direct `nc` test against a Service's
ClusterIP from inside a throwaway Pod: it timed out even for `kube-dns`
itself, regardless of container runtime or CNI plugin), which is why every
in-cluster hostname, including MongoDB's, was unreachable no matter what
changed in the app or the manifests. Switching only the driver to Docker
fixed that same test immediately, which is why it's used here instead.

minikube bundles a default `StorageClass` backed by a hostpath
provisioner, so the MongoDB `PersistentVolumeClaim` in `k8s/03-mongo.yaml`
binds automatically with no extra setup.

> The NetworkPolicy manifests (`k8s/06-network-policies.yaml`) describe the
> intended least-privilege traffic rules, but minikube's default CNI does
> not enforce NetworkPolicy. A CNI (Container Network Interface) plugin is
> what actually implements pod-to-pod networking inside a cluster, and
> minikube's default one only routes traffic without checking any
> NetworkPolicy against it. Calico is a different CNI plugin that does
> enforce NetworkPolicy. If you want to actually demonstrate the policies
> taking effect, add `--cni=calico` to the `minikube start` command above
> to use it instead.

## 2. Build the two images, then load them into the cluster

```bash
podman build -t packing-generator-service:local ./packing-generator-service
podman build -t packing-trip-service:local ./packing-trip-service
podman save -o /tmp/packing-generator-service.tar packing-generator-service:local
podman save -o /tmp/packing-trip-service.tar packing-trip-service:local
minikube image load /tmp/packing-generator-service.tar -p packpilot
minikube image load /tmp/packing-trip-service.tar -p packpilot
```

The images are built with your regular local Podman first, exactly as you
would for any other Podman image. `minikube image load` can take either an
image name or a tar archive; loading by name can fail to find a
Podman-built image on some setups ("was not found; unable to add it to
cache"), so this saves each image to an archive first and loads from that
instead, which works reliably. Re-run all four commands for a service
whenever you change its code and want the redeployed Pod to pick up the
new image.

(`minikube podman-env`, which points your shell's `podman` client straight
at the cluster node's daemon and skips the build-then-load step entirely,
only works when the cluster runs the `crio` container runtime. This
cluster uses `containerd` instead, specifically to avoid any
Docker-branded runtime, so that shortcut isn't available here.)

## 3. Deploy

```bash
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-mongo-secret.yaml
kubectl apply -f k8s/02-app-config.yaml
kubectl apply -f k8s/03-mongo.yaml
kubectl apply -f k8s/04-packing-generator.yaml
kubectl apply -f k8s/05-packing-trip.yaml
kubectl apply -f k8s/06-network-policies.yaml   # optional, see note above

kubectl -n packpilot get pods -w
```

Wait until all pods show `Running` and `1/1` ready (MongoDB usually takes
the longest to become ready on first start).

## 4. Access it from a browser

`packing-trip-service` is exposed as a `NodePort` on `30081`. A
port-forward works regardless of driver:

```bash
kubectl -n packpilot port-forward svc/packing-trip-service 8080:8080
```

minikube can also open it directly, resolving the node's own address for
you:

```bash
minikube service packing-trip-service -n packpilot -p packpilot --url
```

Then open the resulting address in a browser (`http://localhost:8080` if
you used the port-forward). Generate a checklist, save it under a trip
name, check items off, add a custom item, and delete a trip, all through
the UI, all going through the two REST APIs.

## 5. Demonstrate independent horizontal scaling

```bash
kubectl -n packpilot scale deployment packing-generator-service --replicas=4
kubectl -n packpilot scale deployment packing-trip-service --replicas=1
kubectl -n packpilot get pods -l app=packing-generator-service
kubectl -n packpilot get pods -l app=packing-trip-service
```

Each `Deployment` scales independently of the other and of MongoDB:
`packing-generator-service` does no I/O of its own, so it is the cheapest
of the three to scale up under load, while `packing-trip-service` can be
scaled separately to handle more concurrent browser traffic, and MongoDB
stays at one replica throughout.

## 6. Push the images (if you wish to)

When you want the images on Docker Hub:

```bash
podman tag packing-generator-service:local docker.io/<your-dockerhub-username>/packing-generator-service:1.0
podman tag packing-trip-service:local docker.io/<your-dockerhub-username>/packing-trip-service:1.0
podman login docker.io
podman push docker.io/<your-dockerhub-username>/packing-generator-service:1.0
podman push docker.io/<your-dockerhub-username>/packing-trip-service:1.0
```

Then update the `image:` fields in `k8s/04-packing-generator.yaml` and
`k8s/05-packing-trip.yaml` to the pushed tags before redeploying to a
cluster that cannot see images loaded into this one minikube cluster (e.g.
any cluster other than `packpilot`).

## Running a service locally without Kubernetes, or its tests

Each service can be run and tested standalone, outside any container.
That is documented in the service's own README, together with the
environment variables it needs:
[`packing-generator-service/README.md`](packing-generator-service/README.md#running-it-standalone)
and [`packing-trip-service/README.md`](packing-trip-service/README.md#running-it-standalone).
