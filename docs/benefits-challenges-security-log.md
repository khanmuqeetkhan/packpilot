---
title: "PackPilot: Benefits, Challenges, and Security Log"
course: Applied Cloud Computing and Big Data
module: Module 5
---

# PackPilot: Benefits, Challenges, and Security Log

## Benefits of this architecture

### Independent scaling matches independent cost profiles

`packing-generator-service` is pure in-memory rule evaluation with no I/O; `packing-trip-service` is mostly I/O-bound, talking to Mongo and to the generator. Splitting them lets each be given the replica count and resource shape that actually fits its workload, instead of scaling both together as one undifferentiated unit, which is what would happen if the checklist logic lived inside the trip service itself.

### A small blast radius for the one component with external dependencies

Only `packing-trip-service` has outbound dependencies (Mongo, the generator), and only it is reachable from outside the cluster. A restart or redeploy of either service leaves the other entirely unaffected: a bug in the generator's rule logic cannot take down the ability to browse, check off, or edit already-saved trips, since that path never calls the generator at all.

### Schema-free storage fits a genuinely variable checklist

Storing a trip as a MongoDB document with an embedded, variable-length list of items avoids forcing every trip into a fixed relational shape. A cold-weather hiking trip with a toddler and a three-day beach trip for one adult produce checklists of very different length and content, and nothing about the storage model has to change to accommodate either one, nor would a future optional field (a packed-weight estimate, say) require a migration.

## Challenges, limitations, and mitigations

Each item below states the issue first, then what this design does, or could do, about it. Where a mitigation is not implemented, that is because it falls outside this assignment's single-user scope, not because it was overlooked.

### This system is too small to need the scaling it demonstrates

**Issue:** one family's trip planning, even done several times a year, will not generate meaningful concurrent traffic. The horizontal scaling shown in the README and the recorded walkthrough is a deliberate exercise, not a response to real load.

**Context that would make it matter:** a small travel agency or school trip coordinator generating checklists for many families at once during a short enrollment window, each parent filling in trip details and clicking "Generate" independently. `packing-generator-service` would need several replicas to keep response latency low under that burst, while `packing-trip-service` and MongoDB would see steadier, much lower traffic from the smaller number who actually save a trip.

### No authentication or authorization anywhere in the system

**Issue:** both REST APIs are open. Any client that can reach `packing-trip-service` can read, modify, or delete any saved trip, and, if NetworkPolicy were not enforced, any client that could reach `packing-generator-service` directly could call it too. This is acceptable for a single-household tool on a local cluster, but would not be once more than one family's trips lived in the same deployment.

**Mitigation (not implemented, out of scope):** a lightweight API key or JWT check in front of `packing-trip-service`, rejecting unauthenticated requests and scoping saved trips to whichever identity created them.

### Mongo credentials are Secret-sourced but not encrypted at rest

**Issue:** credentials reach the Pods as plain environment variables, originating from a Kubernetes Secret rather than being hardcoded into the image or a ConfigMap. That part is implemented. What is not addressed is that a Secret's values stay readable by anything with `exec` access into the Pod, or with permission to read Secrets in the namespace, since Kubernetes Secrets are base64-encoded, not encrypted, by default.

**Mitigation (not implemented, out of scope):** encryption at rest for Secrets at the cluster level, or moving credential storage to a dedicated secrets manager with Kubernetes holding only a short-lived reference.

### No TLS anywhere inside the cluster or on the external NodePort

**Issue:** all three internal connections (browser to the trip service, trip service to the generator, trip service to Mongo) are plaintext. Reasonable on a local minikube cluster; not acceptable on a cluster shared with other workloads or reachable over an untrusted network.

**Mitigation (not implemented, out of scope):** TLS termination at an Ingress controller for the external-facing path; for the internal paths, either a service mesh with automatic mutual TLS or TLS configured directly on MongoDB's listener, trading operational complexity for protection against an attacker already inside the cluster network.

### Network segmentation is enforced only if the cluster's CNI supports it

**Issue:** the included NetworkPolicy manifests express the intended least-privilege rule (Mongo and the generator each accept traffic only from the trip service, not from each other or from anywhere else), but minikube's default networking plugin does not actually enforce NetworkPolicy.

**Mitigation (documented, verify rather than trust):** the README notes the alternative, starting the cluster with `--cni=calico`, which does enforce it. Based on professional experience with Kubernetes deployments in a telecom environment, this is an easy gap to miss: a NetworkPolicy manifest that looks correct in review can still be doing nothing if the cluster's networking plugin silently ignores it. The right check is attempting a denied connection and confirming it actually fails, not trusting the manifest's presence alone.

### Generated checklists rely on simplifying assumptions

**Issue:** the generator assumes, for example, that laundry becomes available after about a week (clothing quantities are capped at seven days' worth even on a longer trip), and that a traveler's clothing needs scale linearly with trip length within that cap. Reasonable defaults, but occasionally wrong for an unusual trip, a three-week trip with no laundry access, for instance.

**Mitigation (implemented):** every generated item remains editable after being saved. Quantities can be adjusted, items removed, and custom items added, so the generator only has to get a reasonable starting point, not a perfect one, and the user is never locked into an assumption they disagree with.
