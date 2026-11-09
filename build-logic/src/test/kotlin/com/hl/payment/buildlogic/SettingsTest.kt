package com.hl.payment.buildlogic

import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SettingsTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `one include registers a module`() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            include("platform:platform-bom")
            """.trimIndent() + "\n",
        )
        projectDir.resolve("gradle.properties").writeText(projectCoordinates())
        val moduleDir = projectDir.resolve("platform/platform-bom")
        moduleDir.mkdirs()
        moduleDir.resolve("build.gradle.kts").writeText(
            """
            val reportFile = layout.projectDirectory.file("project-report.properties")
            val directory = projectDir.relativeTo(rootDir).invariantSeparatorsPath
            val projectGroup = group.toString()
            val projectVersion = version.toString()

            tasks.register("reportProject") {
                doLast {
                    reportFile.asFile.writeText(
                        "projectDir=" + directory + "\n" +
                            "group=" + projectGroup + "\n" +
                            "version=" + projectVersion + "\n",
                    )
                }
            }
            """.trimIndent() + "\n",
        )

        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("reportProject")
            .build()

        val report = moduleDir.resolve("project-report.properties")
            .readLines()
            .filter { it.isNotBlank() }
            .associate { line ->
                val separator = line.indexOf('=')
                check(separator > 0) { "Malformed project report line: $line" }
                line.substring(0, separator) to line.substring(separator + 1)
            }
        assertEquals("platform/platform-bom", report.getValue("projectDir"))
        assertEquals("com.hl.payment", report.getValue("group"))
        assertEquals("0.0.1-SNAPSHOT", report.getValue("version"))
        assertFalse(projectDir.resolve("settings.gradle.kts").readText().contains("projectDir"))
        assertEquals(projectCoordinates(), Checkout.root.resolve("gradle.properties").readText())
        assertFalse(rootSettings().contains("projectDir"))
    }

    @Test
    fun `the checkout includes build-logic only`() {
        assertEquals("25", System.getProperty("java.specification.version"))
        copyCheckout(projectDir)

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("projects")
            .build()

        val output = result.output
        assertTrue(output.contains("Root project 'hl-payment'"), output)
        assertTrue(output.contains("Included build ':build-logic'"), output)
        assertTrue(output.contains("No sub-projects"), output)
        assertFalse(output.contains("':platform"), output)
        assertFalse(output.contains("':services"), output)
    }

    @Test
    fun `a project repository fails configuration`() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            dependencyResolutionManagement {
                repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
                repositories {
                    mavenCentral()
                }
            }

            include("sample")
            """.trimIndent() + "\n",
        )
        val sample = projectDir.resolve("sample")
        sample.mkdirs()
        sample.resolve("build.gradle.kts").writeText(
            """
            repositories {
                mavenCentral()
            }
            """.trimIndent() + "\n",
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(":sample:help")
            .buildAndFail()

        assertTrue(result.output.contains("project repositories"), result.output)
    }

    @Test
    fun `root settings register the included build and the toolchain resolver`() {
        val settings = rootSettings()
        assertTrue(settings.contains("""includeBuild("build-logic")"""), settings)
        assertTrue(settings.contains("org.gradle.toolchains.foojay-resolver-convention"), settings)
        assertTrue(settings.contains("""rootProject.name = "hl-payment""""), settings)
        assertTrue(settings.contains("""enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")"""), settings)
        assertTrue(settings.contains("mavenCentral()"), settings)
        assertTrue(settings.contains("RepositoriesMode.FAIL_ON_PROJECT_REPOS"), settings)
        assertFalse(settings.contains("projectDir"), settings)
    }

    private fun rootSettings(): String = Checkout.root.resolve("settings.gradle.kts").readText()

    private fun projectCoordinates(): String =
        """
        group=com.hl.payment
        version=0.0.1-SNAPSHOT
        """.trimIndent() + "\n"

    private fun copyCheckout(target: File) {
        listOf(
            "settings.gradle.kts",
            "gradle.properties",
            "gradle/libs.versions.toml",
            "build-logic/settings.gradle.kts",
            "build-logic/build.gradle.kts",
        ).forEach { relative ->
            val destination = target.resolve(relative)
            destination.parentFile.mkdirs()
            Checkout.root.resolve(relative).copyTo(destination)
        }
    }
}
