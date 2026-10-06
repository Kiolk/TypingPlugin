import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.4.20"
    id("org.jetbrains.intellij.platform") version "2.19.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

version = "1.0.9"

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
        ideaVersion {
            sinceBuild = "241"
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        ides {
            recommended()
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
