import org.gradle.api.artifacts.VersionCatalogsExtension

val catalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
val report = layout.projectDirectory.file("catalog-report.properties")

tasks.register("reportCatalog") {
    val output = report
    doLast {
        val lines = mutableListOf<String>()
        fun emit(key: String, value: String) {
            lines += "$key=$value"
        }

        listOf(
            "spring-boot",
            "jspecify",
            "testcontainers",
            "errorprone",
            "errorprone-plugin",
            "gradle",
        ).forEach { alias ->
            val found = catalog.findVersion(alias)
            if (found.isPresent) {
                val constraint = found.get()
                emit("version.$alias", constraint.requiredVersion)
                emit("version.$alias.strict", constraint.strictVersion)
                emit("version.$alias.preferred", constraint.preferredVersion)
            } else {
                emit("version.$alias", "ABSENT")
            }
        }

        listOf(
            "spring-boot-dependencies",
            "spring-boot-gradle-plugin",
            "spring-boot-starter-actuator",
            "spring-boot-starter-test",
            "junit-jupiter",
            "junit-platform-launcher",
            "spring-kafka",
            "flyway-core",
            "flyway-database-postgresql",
            "jspecify",
            "testcontainers-junit-jupiter",
            "errorprone",
        ).forEach { alias ->
            val found = catalog.findLibrary(alias)
            if (found.isPresent) {
                val dependency = found.get().get()
                emit("library.$alias.version", dependency.versionConstraint.requiredVersion)
                emit("library.$alias.module", "${dependency.module.group}:${dependency.module.name}")
            } else {
                emit("library.$alias.version", "ABSENT")
                emit("library.$alias.module", "ABSENT")
            }
        }

        listOf("spring-boot", "errorprone").forEach { alias ->
            val found = catalog.findPlugin(alias)
            if (found.isPresent) {
                val plugin = found.get().get()
                emit("plugin.$alias.id", plugin.pluginId)
                emit("plugin.$alias.version", plugin.version.requiredVersion)
            } else {
                emit("plugin.$alias.id", "ABSENT")
                emit("plugin.$alias.version", "ABSENT")
            }
        }

        output.asFile.writeText(lines.joinToString("\n") + "\n")
    }
}
