package com.hl.payment.buildlogic

import java.io.File
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class VersionCatalogTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `boot kafka flyway and testcontainers are declared once`() {
        val report = CatalogReport.load(Checkout.catalogFile, projectDir)
        val springBoot = report.requiredVersion("spring-boot")

        assertTrue(springBoot.isNotBlank())
        assertEquals("", report.strictVersion("spring-boot"))
        assertEquals("", report.preferredVersion("spring-boot"))
        assertEquals(springBoot, report.libraryVersion("spring-boot-gradle-plugin"))
        assertEquals(
            "org.springframework.boot:spring-boot-gradle-plugin",
            report.libraryModule("spring-boot-gradle-plugin"),
        )
        assertEquals("org.springframework.boot", report.pluginId("spring-boot"))
        assertEquals(springBoot, report.pluginVersion("spring-boot"))
        assertEquals(
            "org.springframework.boot:spring-boot-dependencies",
            report.libraryModule("spring-boot-dependencies"),
        )
        assertEquals(springBoot, report.libraryVersion("spring-boot-dependencies"))

        versionlessLibraries.forEach { (alias, module) ->
            assertEquals("", report.libraryVersion(alias), alias)
            assertEquals(module, report.libraryModule(alias), alias)
        }

        assertFalse(report.hasVersion("testcontainers"))
        assertEquals("", report.libraryVersion("testcontainers-junit-jupiter"))
        assertEquals(
            "org.testcontainers:testcontainers-junit-jupiter",
            report.libraryModule("testcontainers-junit-jupiter"),
        )

        val jspecify = report.requiredVersion("jspecify")
        assertTrue(jspecify.isNotBlank())
        assertEquals(jspecify, report.libraryVersion("jspecify"))
        assertEquals("org.jspecify:jspecify", report.libraryModule("jspecify"))
    }

    @Test
    fun `build-logic reads the same catalog`() {
        val settings = Checkout.root.resolve("build-logic/settings.gradle.kts").readText()
        assertTrue(settings.contains("""from(files("../gradle/libs.versions.toml"))"""))

        val buildFile = Checkout.root.resolve("build-logic/build.gradle.kts").readText()
        assertTrue(buildFile.contains("libs.versions.spring.boot"))

        val report = CatalogReport.load(Checkout.catalogFile, projectDir)
        assertEquals(report.requiredVersion("spring-boot"), Checkout.resolvedSpringBoot)
    }

    @Test
    fun `error prone entries stay together`() {
        val report = CatalogReport.load(Checkout.catalogFile, projectDir)
        val comment = catalogComment(Checkout.catalogFile)
        val decision = Regex("""Error Prone supports the Java 25 toolchain: (yes|no)""")
            .find(comment)
            ?.groupValues
            ?.get(1)
        assertTrue(decision == "yes" || decision == "no", comment)

        val present = listOf(
            report.hasVersion("errorprone"),
            report.hasVersion("errorprone-plugin"),
            report.libraryModule("errorprone") != "ABSENT",
            report.pluginId("errorprone") != "ABSENT",
        )
        assertTrue(present.all { it } || present.none { it }, present.toString())

        if (decision == "yes") {
            assertTrue(present.all { it })
            val errorProne = report.requiredVersion("errorprone")
            val plugin = report.requiredVersion("errorprone-plugin")
            assertTrue(errorProne.isNotBlank())
            assertTrue(plugin.isNotBlank())
            assertEquals(errorProne, report.libraryVersion("errorprone"))
            assertEquals("com.google.errorprone:error_prone_core", report.libraryModule("errorprone"))
            assertEquals("net.ltgt.errorprone", report.pluginId("errorprone"))
            assertEquals(plugin, report.pluginVersion("errorprone"))
        } else {
            assertTrue(present.none { it })
        }
    }

    @Test
    fun `catalog comment records the boot patch and the testcontainers decision`() {
        val report = CatalogReport.load(Checkout.catalogFile, projectDir)
        val comment = catalogComment(Checkout.catalogFile)
        val boot = Regex("""Checked against spring-boot (\S+) on (\d{4}-\d{2}-\d{2}):""")
            .find(comment)
        assertTrue(boot != null, comment)
        assertEquals(report.requiredVersion("spring-boot"), boot!!.groupValues[1])

        val managed = Regex("""org\.testcontainers managed by spring-boot-dependencies: (yes|no)""")
            .find(comment)
            ?.groupValues
            ?.get(1)
        assertTrue(managed == "yes" || managed == "no", comment)
        if (managed == "yes") {
            assertFalse(report.hasVersion("testcontainers"))
            assertEquals("", report.libraryVersion("testcontainers-junit-jupiter"))
        } else {
            val testcontainers = report.requiredVersion("testcontainers")
            assertTrue(testcontainers.isNotBlank())
            assertEquals(testcontainers, report.libraryVersion("testcontainers-junit-jupiter"))
        }
    }

    private fun catalogComment(catalogFile: File): String =
        catalogFile.readLines().takeWhile { it.startsWith("#") }.joinToString("\n")

    private companion object {
        val versionlessLibraries = listOf(
            "spring-kafka" to "org.springframework.kafka:spring-kafka",
            "flyway-core" to "org.flywaydb:flyway-core",
            "flyway-database-postgresql" to "org.flywaydb:flyway-database-postgresql",
            "spring-boot-starter-actuator" to "org.springframework.boot:spring-boot-starter-actuator",
            "spring-boot-starter-test" to "org.springframework.boot:spring-boot-starter-test",
            "junit-jupiter" to "org.junit.jupiter:junit-jupiter",
            "junit-platform-launcher" to "org.junit.platform:junit-platform-launcher",
        )
    }
}
