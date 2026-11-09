package com.hl.payment.buildlogic

import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DuplicateLibsCatalogTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `a second libs catalog fails configuration`() {
        val catalogDir = projectDir.resolve("gradle")
        catalogDir.mkdirs()
        Checkout.catalogFile.copyTo(catalogDir.resolve("libs.versions.toml"))
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            dependencyResolutionManagement {
                versionCatalogs {
                    create("libs") {
                        from(files("gradle/libs.versions.toml"))
                    }
                }
            }

            rootProject.name = "duplicate-libs"
            """.trimIndent() + "\n",
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("help")
            .buildAndFail()

        assertTrue(
            result.output.contains("libs"),
            result.output,
        )
    }
}
