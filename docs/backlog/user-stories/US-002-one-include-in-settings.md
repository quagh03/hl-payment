# US-002: One include in settings

| | |
|---|---|
| Feature | [FT-001](../features/FT-001-version-catalog-and-settings.md) |
| Status | Done |
| Phase | 0 |
| Estimate | S |
| Last updated | 2026-11-09 |

## Story

As a **developer**, I want **adding a module to be one include in settings** so that **the module inherits group, version, and repositories**.

## Acceptance criteria

```gherkin
Scenario: one include registers a module
  Given settings contain include("platform:platform-bom")
  And the directory platform/platform-bom exists
  And gradle.properties sets group com.hl.payment and version 0.0.1-SNAPSHOT
  When the build configures that project
  Then its project directory is platform/platform-bom
  And its group is com.hl.payment
  And its version is 0.0.1-SNAPSHOT
  And settings assign no projectDir

Scenario: the checkout includes build-logic only
  Given a clean checkout
  When the developer runs ./gradlew projects on Java 25
  Then the root project is hl-payment
  And the included build is build-logic
  And the project list has no platform project and no services project

Scenario: a project repository fails configuration
  Given settings set RepositoriesMode.FAIL_ON_PROJECT_REPOS and mavenCentral()
  When a project adds a repositories block
  Then the build fails

Scenario: root settings register the included build and the toolchain resolver
  Given the root settings.gradle.kts
  When the settings test reads it
  Then it includes build-logic
  And it applies org.gradle.toolchains.foojay-resolver-convention
  And rootProject.name is hl-payment
  And type-safe project accessors are on
  And project dependencies resolve from mavenCentral()
```

## Notes

- Depends on [US-001](US-001-one-version-catalog.md) for the catalog, the wrapper, and the `build-logic` test source set.
- Settings layout and the include mapping are in [FT-001](../features/FT-001-version-catalog-and-settings.md#settings).
- US-001 already added root `settings.gradle.kts` with `includeBuild("build-logic")`, the Plugin Portal in `pluginManagement`, and `rootProject.name = "hl-payment"`. It also added `.gitignore` and the repository lock in `build-logic/settings.gradle.kts`. This story adds the Foojay plugin, root `gradle.properties`, `FAIL_ON_PROJECT_REPOS` and `mavenCentral()` in the root settings, type-safe project accessors, and the include-path tests.
- The root settings include `build-logic` only. A later epic appends the `include` line when it adds the first module. The build file that names `hl.library` or `hl.spring-service` arrives with FT-002.
- Implemented on 2026-11-09. The root settings apply `org.gradle.toolchains.foojay-resolver-convention` version 1.0.0. Gradle 9.9.0 still treats `TYPESAFE_PROJECT_ACCESSORS` as a preview, so settings enable that flag. `gradle.properties` sets `group=com.hl.payment` and `version=0.0.1-SNAPSHOT`.
- On a wrapper release that still treats `TYPESAFE_PROJECT_ACCESSORS` as a preview, settings enable that flag. On a release that has graduated the preview and rejects the flag, settings omit the line and accessors stay on.
- FT-003 adds the root `build.gradle.kts` and the `check` task.

## Definition of done

- [x] Acceptance scenarios automated in `build-logic`
- [x] `./gradlew :build-logic:test` passes on Java 25
- [x] `./gradlew projects` on a clean checkout lists root project `hl-payment` and included build `build-logic`
- [x] [FT-001](../features/FT-001-version-catalog-and-settings.md) matches the include contract that was checked in
