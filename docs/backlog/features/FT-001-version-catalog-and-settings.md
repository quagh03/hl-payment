# FT-001: Version catalog and settings

| | |
|---|---|
| Epic | [EP-001](../epics/EP-001-monorepo-and-build-conventions.md) |
| Status | Backlog |
| Phase | 0 |
| Owner | huylq |
| Last updated | 2026-11-08 |
| Services | `build-logic` |

## Summary

FT-001 creates the Gradle skeleton the rest of EP-001 builds on: one version catalog, a wrapper that runs on Java 25, and settings that include `build-logic` and resolve libraries from Maven Central. Group `com.hl.payment` and version `0.0.1-SNAPSHOT` are set once and inherited by every project in the root build. A module added later is one `include` line and one build file. FT-002 adds the convention plugins. FT-003 wires `./gradlew check`.

## User stories

Story files stay in the [delivery plan](../delivery-plan.md) until they are split.

| ID | Story | Status |
|---|---|---|
| US-001 | One version catalog: Boot, Kafka, Flyway, and Testcontainers versions are declared once | Backlog |
| US-002 | One include in settings registers a module | Backlog |

## Functional behavior

### Preconditions

- The checkout contains the files in [Files](#files) and no `platform/` or `services/` project yet.
- The developer invokes `./gradlew` with Java 25. The wrapper release is one whose release notes list Java 25 as a supported runtime.
- Network access reaches Maven Central, the Gradle Plugin Portal, and `services.gradle.org`.

### Main flow

1. `./gradlew projects` configures the root build `hl-payment` and the included build `build-logic`.
2. Gradle loads `gradle/libs.versions.toml` as the `libs` catalog. `build-logic/settings.gradle.kts` loads that same file, so plugin builds and service builds read one list of versions.
3. Library resolution uses Maven Central. A project build script that adds a repository fails configuration.
4. A later epic adds a module by creating its directory and appending one `include` line. The new project inherits group and version. Its build file names a convention plugin; FT-002 provides those plugins.

### Failure flows

- A `repositories` block in any project build script fails configuration (`RepositoriesMode.FAIL_ON_PROJECT_REPOS`).
- Declaring a catalog named `libs` in the root settings fails configuration. Gradle already created `libs` from `gradle/libs.versions.toml`.
- A version literal in a `*.gradle.kts` file other than the Foojay plugin line fails the catalog test.
- When the current Error Prone release does not support the Java 25 toolchain, the catalog has no `errorprone` key. The wrapper, the Boot version, and settings still configure.
- `build-logic` is a separate build. It reads the catalog only through `from(files("../gradle/libs.versions.toml"))`. A wrong path fails configuration of the included build.

## Technical design

### Files

```
hl-payment/
├── .gitignore
├── gradle.properties
├── settings.gradle.kts
├── gradle/libs.versions.toml
├── gradle/wrapper/gradle-wrapper.properties
├── gradle/wrapper/gradle-wrapper.jar
├── gradlew
├── gradlew.bat
└── build-logic/
    ├── settings.gradle.kts
    ├── build.gradle.kts
    └── src/test/kotlin/
```

FT-003 adds the root `build.gradle.kts` for the `check` task. FT-002 adds `build-logic/src/main/kotlin/` and the three convention plugins. Generate the wrapper with the Wrapper task for the chosen Gradle version so the jar, properties, and scripts match, and commit the checksum in `distributionSha256Sum`.

`.gitignore` ignores `.gradle/` and `build/`.

### Project identity

`gradle.properties` at the root holds the only project coordinates:

```properties
group=com.hl.payment
version=0.0.1-SNAPSHOT
```

Gradle applies these to every project in the root build. The group matches the HLD package root `com.hl.payment`. Subprojects do not get their own `gradle.properties`. `build-logic` is an included build, so it does not inherit this file; its outputs are plugin ids.

### Version catalog

`gradle/libs.versions.toml` is the single catalog. A third-party library version is a key in this file, or it is supplied by the Spring Boot BOM. Plugin and library aliases that the BOM manages carry a module and no version.

Header comment, filled in when the versions are chosen:

```toml
# Single catalog for hl-payment.
# Checked against spring-boot <version> on <date>:
# - org.testcontainers managed by spring-boot-dependencies: yes|no
# - Error Prone supports the Java 25 toolchain: yes|no
```

| Key | Where the version lives |
|---|---|
| `spring-boot` | This catalog. The only Spring Boot version. |
| `jspecify` | This catalog. |
| `testcontainers` | This catalog only when the pinned `spring-boot-dependencies` BOM does not manage `org.testcontainers`. |
| `errorprone` | This catalog only when that Error Prone release supports the Java 25 toolchain. |
| `errorprone-plugin` | This catalog under the same condition. Version of `net.ltgt.errorprone`. |

| Alias | Module | Version |
|---|---|---|
| `spring-boot-dependencies` | `org.springframework.boot:spring-boot-dependencies` | `version.ref = "spring-boot"` |
| `spring-boot-gradle-plugin` | `org.springframework.boot:spring-boot-gradle-plugin` | `version.ref = "spring-boot"` |
| `spring-boot-starter-actuator` | `org.springframework.boot:spring-boot-starter-actuator` | Boot BOM |
| `spring-boot-starter-test` | `org.springframework.boot:spring-boot-starter-test` | Boot BOM |
| `junit-jupiter` | `org.junit.jupiter:junit-jupiter` | Boot BOM |
| `spring-kafka` | `org.springframework.kafka:spring-kafka` | Boot BOM |
| `flyway-core` | `org.flywaydb:flyway-core` | Boot BOM |
| `flyway-database-postgresql` | `org.flywaydb:flyway-database-postgresql` | Boot BOM. Confirm the artifact id against the pinned BOM; keep the alias version-less. |
| `jspecify` | `org.jspecify:jspecify` | `version.ref = "jspecify"` |
| `testcontainers-junit-jupiter` | `org.testcontainers:junit-jupiter` | Boot BOM, or `version.ref = "testcontainers"` when the BOM does not manage it |
| `errorprone` | `com.google.errorprone:error_prone_core` | `version.ref = "errorprone"` when the key exists |

Plugin alias `spring-boot` is `org.springframework.boot` with `version.ref = "spring-boot"`. When `errorprone` exists, plugin alias `errorprone` is `net.ltgt.errorprone` with `version.ref = "errorprone-plugin"`. FT-002 adds the Boot 4 OpenTelemetry starter alias after checking the Boot 4 reference.

`junit-jupiter` is a version-less alias. `build-logic` tests depend on it through the Boot BOM. It has no `[versions]` key.

Selection procedure, recorded in the catalog comment and the wrapper properties:

1. Choose the current stable Gradle release that runs on Java 25. Record it only in `gradle/wrapper/gradle-wrapper.properties`, with `distributionSha256Sum` for the `-bin` distribution.
2. Choose the current Spring Boot 4 patch. Record it as `spring-boot`.
3. Read that release's `spring-boot-dependencies` BOM. If `org.testcontainers` is managed, add the Testcontainers alias with no version. If it is not, add `[versions].testcontainers` and point the alias at it.
4. Choose the current JSpecify release and record it as `jspecify`.
5. If the current Error Prone release supports the Java 25 toolchain, record `errorprone` and `errorprone-plugin`. Otherwise omit both keys, the library alias, and the plugin alias.
6. Choose the current `org.gradle.toolchains.foojay-resolver-convention` release that can provision a Java 25 JDK. Record that version only in the root `settings.gradle.kts`.

### Settings

Root `settings.gradle.kts`:

```kotlin
pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "<recorded at implementation>"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "hl-payment"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
```

Settings plugins resolve before the catalog exists, so the Foojay version is the one version literal allowed in `*.gradle.kts`. The Plugin Portal is the repository for that settings plugin. Project dependencies resolve from Maven Central only.

`TYPESAFE_PROJECT_ACCESSORS` stays enabled on wrapper releases where it is still a feature preview. On a release that has graduated the preview and rejects the flag, delete the line; accessors remain available.

`build-logic/settings.gradle.kts` loads the shared catalog and repeats the repository lock:

```kotlin
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
```

The included build's name is `build-logic`, so its tasks are `:build-logic:test` and, once FT-002 adds the plugin classes, `:build-logic:check`.

`build-logic` applies `kotlin-dsl` so the included build configures before any convention plugin exists. It cannot apply `hl.java-conventions` to itself. Its test classpath imports the Boot BOM from the catalog and asks for JUnit Jupiter without a version:

```kotlin
plugins {
    `kotlin-dsl`
}

dependencies {
    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation(libs.junit.jupiter)
    testImplementation(gradleTestKit())
}

tasks.test {
    useJUnitPlatform()
}
```

FT-002 applies `hl.java-conventions` on service and platform modules, which is what turns on `useJUnitPlatform()` there. `build-logic` keeps the test setup above.

### Module registration

An include path maps to the directory with the same segments. No `projectDir` override.

```kotlin
include("platform:platform-bom")
```

That line expects `platform/platform-bom/build.gradle.kts`. The type-safe accessor is `projects.platform.platformBom`. The build file that FT-002 makes sufficient is:

```kotlin
plugins {
    id("hl.library")
}
```

A service uses `id("hl.spring-service")` the same way. The build file names the plugin. Group, version, repositories, and the toolchain stay on the root build and the convention plugins. FT-001 proves the directory mapping with TestKit. The root settings include `build-logic` only.

```mermaid
flowchart LR
  props["gradle.properties group and version"]
  include["include platform:platform-bom"]
  dir["platform/platform-bom/build.gradle.kts"]
  plugin["plugins block names one convention plugin"]
  props --> include
  include --> dir --> plugin
```

## Non-functional requirements

- One catalog file. `build-logic` and the root build both read `gradle/libs.versions.toml`.
- The wrapper distribution is verified with `distributionSha256Sum`. The wrapper jar and scripts are committed.
- Project dependency resolution is Maven Central, locked with `FAIL_ON_PROJECT_REPOS` in the root build and in `build-logic`.
- The local gate for this feature is `./gradlew :build-logic:test` on Java 25. FT-003 makes the root `check` task run it.
- Adding a module touches `settings.gradle.kts` with one `include` line and adds one build file. Catalog, repositories, group, and version stay as this feature left them.

## Test plan

Tests live in `build-logic/src/test/kotlin` and run with `./gradlew :build-logic:test`. Pass the real catalog path into the test JVM with a system property set from `rootDir.parentFile.resolve("gradle/libs.versions.toml")`.

- **Catalog keys.** A TestKit build loads the real `libs.versions.toml`. Assert `spring-boot` has a required version; `spring-boot-gradle-plugin` and the `spring-boot` plugin alias use that same version; `spring-kafka`, `flyway-core`, `flyway-database-postgresql`, `spring-boot-starter-actuator`, `spring-boot-starter-test`, and `junit-jupiter` have no version of their own; `jspecify` has a required version. Assert `testcontainers-junit-jupiter` has a version only when `[versions].testcontainers` exists, and the `errorprone` library exists only when `[versions].errorprone` exists.
- **Version literals.** Read every `*.gradle.kts` in the checkout. The only version literal is the Foojay plugin version in the root settings. `gradle.properties` holds `0.0.1-SNAPSHOT`. `gradle-wrapper.properties` holds the wrapper version and checksum.
- **Repository lock.** A TestKit settings file sets `FAIL_ON_PROJECT_REPOS` and `mavenCentral()`, and a subproject adds `repositories { mavenCentral() }`. The build fails.
- **Include path.** A TestKit build whose settings file is `include("platform:platform-bom")` plus a `platform/platform-bom` directory reports that project directory. Assert the production root settings contain `includeBuild("build-logic")`, `rootProject.name = "hl-payment"`, and no `projectDir` assignment.
- **Clean checkout.** `./gradlew projects` on Java 25 lists the root project `hl-payment` and the included build `build-logic`. The repository-level `check` gate is FT-003.

## Open questions

None. The selection procedure above records the Gradle, Foojay, Spring Boot, JSpecify, Testcontainers, and Error Prone versions when this feature is implemented.
