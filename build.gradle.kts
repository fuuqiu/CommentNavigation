plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.20"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "cn.tinyue"
version = "1.2.1"

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        val localIdePath = providers.gradleProperty("localIdePath")
        if (localIdePath.isPresent) {
            local(localIdePath.get())
        } else {
            intellijIdea("2026.2.0.1")
        }
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.mockk:mockk:1.14.9")
}

kotlin { jvmToolchain(25) }

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild.set("262")
            untilBuild.set("262.*")
        }
    }
    signing {
        certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
        privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
        password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
    }
    publishing { token.set(providers.environmentVariable("PUBLISH_TOKEN")) }
    buildSearchableOptions.set(false)
}

tasks.test { useJUnitPlatform() }

// 将唯一许可正文附入安装包，避免分发时丢失授权说明。
tasks.processResources {
    from("LICENSE.txt") { into("META-INF") }
}
