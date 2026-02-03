import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij.platform") version "2.1.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
}

version = "1.0.6"

group = "com.github.kiolk.typingplugin"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.3")
        bundledPlugins("com.intellij.java")
        instrumentationTools()
        testFramework(TestFrameworkType.Platform)
        zipSigner()
    }
    implementation("org.jfree:jfreechart:1.5.6")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        name = "Typing Training"
        changeNotes =
            """
            - Added performance chart at the end of each session.
            - Implemented persistent statistics across IDE sessions.
            - Unified session summary and performance chart into a single dialog.
            - Added detailed logging for typing events and errors.
            - Improved chart visualization with whole number attempt axis.
            """.trimIndent()
        ideaVersion {
            sinceBuild = "241"
            untilBuild = "253.*"
        }
    }

    signing {
        val cert =
            providers.environmentVariable("CERTIFICATE_CHAIN")
                .orElse(providers.gradleProperty("certificateChain"))
                .map { it.replace("\\n", "\n") }

        val key =
            providers.environmentVariable("PRIVATE_KEY")
                .orElse(providers.gradleProperty("privateKey"))
                .map { it.replace("\\n", "\n") }

        certificateChain.set(cert)
        privateKey.set(key)
        password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD").orElse(providers.gradleProperty("privateKeyPassword")))
    }

    publishing {
        token.set(providers.environmentVariable("PUBLISH_TOKEN").orElse(providers.gradleProperty("publishToken")))
        channels.set(listOf(providers.environmentVariable("PUBLISH_CHANNEL").getOrElse("default")))
    }
}

kotlin {
    jvmToolchain(21)
}

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
    coloredOutput.set(true)
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    buildSearchableOptions {
        enabled = false
    }
}
