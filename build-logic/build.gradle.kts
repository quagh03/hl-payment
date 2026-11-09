plugins {
    `kotlin-dsl`
}

dependencies {
    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(gradleTestKit())
}

tasks.test {
    useJUnitPlatform()
    systemProperty(
        "catalog.path",
        rootDir.parentFile.resolve("gradle/libs.versions.toml").absolutePath,
    )
    systemProperty(
        "resolved.spring-boot",
        libs.versions.spring.boot.get(),
    )
}
