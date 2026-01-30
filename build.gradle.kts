plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

version = "1.0.1"

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
            // Removing untilBuild entirely. 
            // If not specified, the Marketplace assumes no upper limit for the current branch 
            // or you can set it to a very high version like "999.*" if you want to be explicit.
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
