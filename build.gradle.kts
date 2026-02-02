import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij.platform") version "2.1.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
}

version = "1.0.5"

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
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        name = "Typing Training"
        description =
            """
            Improved stability and usability.
            - Added ability to drag typing dialog to any position on the screen.
            - Improved stability.
            """.trimIndent()
        changeNotes =
            """
            - Fixed incorrect representation of the time on the final statistic dialog.
            - Improved stability.
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
