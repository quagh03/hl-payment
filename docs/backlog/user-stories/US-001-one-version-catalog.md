# US-001: One version catalog

| | |
|---|---|
| Feature | [FT-001](../features/FT-001-version-catalog-and-settings.md) |
| Status | Done |
| Phase | 0 |
| Estimate | S |
| Last updated | 2026-11-08 |

## Story

As a **developer**, I want **one version catalog** so that **Boot, Kafka, Flyway, and Testcontainers versions are declared once**.

## Acceptance criteria

```gherkin
Scenario: Boot, Kafka, Flyway, and Testcontainers are declared once
  Given the catalog file gradle/libs.versions.toml
  When the build-logic catalog test loads it
  Then spring-boot has one required version
  And the spring-boot plugin alias and spring-boot-gradle-plugin use that version
  And spring-kafka, flyway-core, flyway-database-postgresql, spring-boot-starter-actuator, spring-boot-starter-test, and junit-jupiter carry no version of their own
  And testcontainers-junit-jupiter carries a version only when the testcontainers version key exists
  And jspecify has a required version

Scenario: build-logic reads that same catalog
  Given build-logic/settings.gradle.kts loads ../gradle/libs.versions.toml
  When :build-logic:test resolves the libs catalog
  Then its spring-boot version is the version in gradle/libs.versions.toml

Scenario: Error Prone entries stay together
  Given the catalog comment records whether Error Prone supports the Java 25 toolchain
  When the catalog test inspects the errorprone version, the errorprone-plugin version, the errorprone library, and the errorprone plugin alias
  Then those four entries are all present or all absent

Scenario: the Gradle version lives in the wrapper
  Given gradle/wrapper/gradle-wrapper.properties
  When the version-literal test reads the checkout
  Then that file records a Gradle release that runs on Java 25 and a distributionSha256Sum
  And the catalog has no Gradle version key
  And a *.gradle.kts file contains a version literal only on the Foojay plugin line in the root settings

Scenario: a second libs catalog fails configuration
  Given Gradle has already created the libs catalog from gradle/libs.versions.toml
  When settings declare another catalog named libs
  Then configuration fails
```

## Notes

- Catalog shape, aliases, and the patch-version selection procedure are in [FT-001](../features/FT-001-version-catalog-and-settings.md#version-catalog).
- This story adds `gradle/libs.versions.toml`, the wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/`), `build-logic/settings.gradle.kts` (the shared catalog plus the repository lock the tests need in order to resolve), and `build-logic/build.gradle.kts` with `kotlin-dsl`, the Boot BOM platform, `junit-jupiter`, `junit-platform-launcher`, and `useJUnitPlatform()`.
- Root `settings.gradle.kts` only includes `build-logic` and names the root project, so `./gradlew :build-logic:test` has a build to run. `.gitignore` ignores `.gradle/` and `build/`. [US-002](US-002-one-include-in-settings.md) adds `gradle.properties`, the root repository lock, the include path, and the Foojay plugin line. That line is the one version literal the catalog test allows in `*.gradle.kts`.
- FT-002 adds the Boot 4 OpenTelemetry alias and the convention plugins.

## Definition of done

- [x] Acceptance scenarios automated in `build-logic`
- [x] `./gradlew :build-logic:test` passes on Java 25
- [x] The catalog comment records the Spring Boot patch, the Testcontainers BOM check, and the Error Prone decision
- [x] [FT-001](../features/FT-001-version-catalog-and-settings.md) matches the catalog that was checked in
