# Ladder Sense — dev notes

Fabric mod for Minecraft 26.1.x, Java 25, Loom. User-facing docs live in README.md; this file is
build/test conventions only.

## Versioning

- The build pins `minecraft_version` to the **26.1 floor** in gradle.properties so the compiler
  can't admit symbols that only exist in later patches; the released jar declares `~26.1` in
  fabric.mod.json and runs on all of 26.1.x.
- `fabric_api_version` must stay on the `+26.1` branch (tops out at 0.145.1). Don't add a
  fabric-api floor in fabric.mod.json above that, or 26.1 users get locked out.

## Game tests

```
./gradlew runGameTest      # just the tests
./gradlew build            # also runs them — CI and releases are test-gated
```

Tests boot a real headless server with mixins applied. Success line: `All N required tests passed`.

- Test sources live in `src/gametest/` — **all lowercase**. `src/gameTest/` works on a
  case-insensitive Mac and then fails on Linux CI with "Failed to query the value of property
  'modId'".
- The test source set is its own mod (`src/gametest/resources/fabric.mod.json`, id
  `laddersense_gametest`) so test classes and the `fabric-gametest` entrypoint never reach the
  release jar. Don't set `modId` in the `fabricApi { configureTests }` block — it collides with
  the `loom { mods }` block in build.gradle.
- The `fabricApi { configureTests }` block must stay **after** the `loom {}` block: it finalizes
  loom properties that `splitEnvironmentSourceSets()` still needs to change.
- Default test structure is an empty 8x8 box, default `maxTicks` 20 — raise per-test for anything
  that waits. The test server ticks flat-out, so long waits finish in milliseconds.

## Dev server

`./gradlew runServer` boots a headless dev server (eula already accepted in run/). Kill the server
**fully** before deleting run/world — a dying server rewrites a corrupt partial world ("Overworld
settings missing" on next boot) and holds port 25565 plus run/world/session.lock.
