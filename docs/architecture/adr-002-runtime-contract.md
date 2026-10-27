# ADR-002 - Runtime contract

Generated deterministically at project bootstrap. This file is the single source of truth
for which services this product runs against. `docker-compose.yml`, the build manifest and
the application configuration are CONSEQUENCES of what is declared here - never independent
decisions taken in one artifact.

## Declared services

```yaml
datastore: UNDECLARED
```

`UNDECLARED` is not a placeholder to be ignored. It is the open question, written down so
that it is an actual object: readable, refutable, and owned. Its owner is ARCHITECTURE
(BARCAN-TAG-01, stage 20), which decides the datastore from the client's brief and replaces
this line with the engine and version - for example `postgresql:15`, `mysql:8`, or `none`
for a product that genuinely stores nothing.

The bootstrap does not choose. A datastore is a contingent fact about ONE brief; the
scaffold may only contain what is true of every product this factory could build.

## Consequences of the declaration

Once the line above names an engine, all four of these must follow from it, and a check in
the factory reports it when they do not:

1. `docker-compose.yml` provides that engine.
2. The build manifest declares that engine's driver.
3. The application configuration points at that engine.
4. **The test suite runs against that engine.** This one is not optional and is the reason
   the other three were not enough: a migration written against one engine and verified
   against it will pass every gate the factory has and still be meaningless in delivery.
