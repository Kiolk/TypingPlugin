plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

// Dynamic versioning from Git Tag or default to 1.0-SNAPSHOT
version = providers.environmentVariable("GITHUB_REF_NAME")
    .getOrElse("1.0.0")
    .let { if (it.startsWith("v")) it.substring(1) else it }

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
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "Typing Training"
        ideaVersion {
            sinceBuild = "241"
            untilBuild = ""
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // Use "default" for stable releases, "beta" or "alpha" for testing
        channels = listOf(providers.environmentVariable("PUBLISH_CHANNEL").getOrElse("default"))
    }
}

kotlin {
    jvmToolchain(17)
}
