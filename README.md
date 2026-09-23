# collector-packetevents

VaniaMetrics collector for packetevents. One module = one jar, loaded by the platform if — and only if — the core is present.

## Build

```sh
./gradlew build                                # build/libs/vania-metrics-collector-packetevents-<v>.jar
./gradlew build -PvaniaCore.ref=main           # API from another core ref
./gradlew build -PvaniaCore.dir=../core        # API from a local core (API development)
./gradlew compileJava                          # compile only
```

The API comes from the git repo [Vania-Metrics/core](https://github.com/Vania-Metrics/core), at the ref from `gradle.properties` (`vaniaCore.ref`). Gradle clones it into `.gradle/vania-core` and includes it as a composite build: `fr.samflix:vania-metrics-api` is compiled from its sources at that ref. The produced jar's version is that of its `Version.java`.

Bumping the version: change `vaniaCore.ref` (a `vX.Y.Z` tag).

## Dependencies

- `gradle.properties` — repo and ref of the core, source of the API.
- `gradle/libs.versions.toml` — paper-api, velocity-api, and the targeted plugins (Modrinth Maven repo), compile-only.
- `gradle/verification-metadata.xml` — SHA-256 checksums for everything resolved. After a version bump: `./gradlew --write-verification-metadata sha256 build`.
