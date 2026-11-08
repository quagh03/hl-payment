package com.hl.payment.buildlogic

import java.io.File
import java.util.Properties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GradleWrapperTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `the gradle version lives in the wrapper`() {
        val properties = Properties()
        Checkout.root.resolve("gradle/wrapper/gradle-wrapper.properties").inputStream().use {
            properties.load(it)
        }
        val distributionUrl = properties.getProperty("distributionUrl")
        val checksum = properties.getProperty("distributionSha256Sum")
        val version = Regex("""gradle-(\d+\.\d+(?:\.\d+)?)-bin\.zip""")
            .find(distributionUrl)
            ?.groupValues
            ?.get(1)

        assertTrue(
            distributionUrl.startsWith("https://services.gradle.org/distributions/gradle-"),
            distributionUrl,
        )
        assertTrue(version != null, distributionUrl)
        assertTrue(runsOnJava25(version!!), version)
        assertTrue(checksum.matches(Regex("[0-9a-fA-F]{64}")), checksum)

        val report = CatalogReport.load(Checkout.catalogFile, projectDir)
        assertEquals(false, report.hasVersion("gradle"))

        val violations = versionLiterals(Checkout.root)
        assertEquals(emptyList<String>(), violations)
    }

    private fun runsOnJava25(version: String): Boolean {
        val parts = version.split(".").map { it.toInt() }
        val major = parts[0]
        val minor = parts.getOrElse(1) { 0 }
        // Gradle 9.1.0 is the first release whose notes list Java 25 as a runtime.
        return major > 9 || (major == 9 && minor >= 1)
    }

    private fun versionLiterals(root: File): List<String> {
        val violations = mutableListOf<String>()
        root.walkTopDown()
            .onEnter { directory ->
                directory.name != "build" && directory.name != ".gradle" && directory.name != ".git"
            }
            .filter { file -> file.isFile && file.name.endsWith(".gradle.kts") }
            .forEach { file ->
                val relative = file.relativeTo(root).invariantSeparatorsPath
                file.readLines().forEachIndexed { index, raw ->
                    val code = raw.replace(LINE_COMMENT, "").trim()
                    if (code.isEmpty()) {
                        return@forEachIndexed
                    }
                    val toolchainResolver = TOOLCHAIN_RESOLVER_LINE.matches(code)
                    val location = relative + ":" + (index + 1) + ": " + code
                    if (toolchainResolver && relative != "settings.gradle.kts") {
                        violations += location
                    }
                    val literal = VERSION_ASSIGNMENT.containsMatchIn(code) || MODULE_COORDINATE.containsMatchIn(code)
                    if (!toolchainResolver && literal) {
                        violations += location
                    }
                }
            }
        return violations
    }
}

private val LINE_COMMENT = Regex("(?<!:)//.*")

private val TOOLCHAIN_RESOLVER_LINE = Regex(
    "^id\\(\"org\\.gradle\\.toolchains\\.foojay-resolver-convention\"\\) version \"[^\"]+\"" + '$',
)

private val VERSION_ASSIGNMENT = Regex(
    "(?<![\\w.])version(?:\\s*\\(|\\s*=|\\s+)\\s*[\"'][^\"']+[\"']",
)

private val MODULE_COORDINATE = Regex(
    "[\"'][^\"'\\s]+:[^\"'\\s]+:[^\"'\\s]+[\"']",
)
