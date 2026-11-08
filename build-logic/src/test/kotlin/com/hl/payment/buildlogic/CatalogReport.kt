package com.hl.payment.buildlogic

import java.io.File
import org.gradle.testkit.runner.GradleRunner

internal class CatalogReport private constructor(
    private val values: Map<String, String>,
) {
    fun requiredVersion(alias: String): String = present("version.$alias")

    fun hasVersion(alias: String): Boolean = values["version.$alias"] != "ABSENT"

    fun strictVersion(alias: String): String = values.getValue("version.$alias.strict")

    fun preferredVersion(alias: String): String = values.getValue("version.$alias.preferred")

    fun libraryVersion(alias: String): String = values.getValue("library.$alias.version")

    fun libraryModule(alias: String): String = values.getValue("library.$alias.module")

    fun pluginId(alias: String): String = values.getValue("plugin.$alias.id")

    fun pluginVersion(alias: String): String = values.getValue("plugin.$alias.version")

    private fun present(key: String): String {
        val value = values.getValue(key)
        check(value != "ABSENT") { "$key is absent from the catalog" }
        return value
    }

    companion object {
        fun load(catalogFile: File, projectDir: File): CatalogReport {
            cached?.let { return it }
            projectDir.resolve("build.gradle.kts").writeText(probeBuildScript())
            projectDir.resolve("settings.gradle.kts").writeText(
                """
                dependencyResolutionManagement {
                    versionCatalogs {
                        create("libs") {
                            from(files("${catalogFile.absolutePath}"))
                        }
                    }
                }

                rootProject.name = "catalog-probe"
                """.trimIndent() + "\n",
            )

            GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("reportCatalog")
                .forwardOutput()
                .build()

            val values = projectDir.resolve("catalog-report.properties")
                .readLines()
                .filter { it.isNotBlank() }
                .associate { line ->
                    val separator = line.indexOf('=')
                    check(separator > 0) { "Malformed catalog report line: $line" }
                    line.substring(0, separator) to line.substring(separator + 1)
                }
            return CatalogReport(values).also { cached = it }
        }

        @Volatile
        private var cached: CatalogReport? = null

        private fun probeBuildScript(): String {
            val resource = CatalogReport::class.java.getResource("/catalog-probe/build.gradle.kts")
                ?: error("catalog probe build script is missing from the test resources")
            return resource.readText()
        }
    }
}
