package com.hl.payment.buildlogic

import java.io.File

internal object Checkout {
    val catalogFile: File
        get() {
            val path = System.getProperty("catalog.path")
                ?: error("catalog.path system property is required")
            return File(path)
        }

    val root: File
        get() = catalogFile.parentFile.parentFile

    val resolvedSpringBoot: String
        get() = System.getProperty("resolved.spring-boot")
            ?: error("resolved.spring-boot system property is required")
}
