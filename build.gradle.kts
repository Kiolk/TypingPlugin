plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

version = "1.0.2"

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
        zipSigner()
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "Typing Training"
        ideaVersion {
            sinceBuild = "243"
        }
    }

    signing {
        // Robust handling for both literal newlines and the "\n" string
        val cert = providers.environmentVariable("CERTIFICATE_CHAIN")
            .orElse(providers.gradleProperty("certificateChain"))
            .map { it.replace("\\n", "\n") }
        
        val key = providers.environmentVariable("PRIVATE_KEY")
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
