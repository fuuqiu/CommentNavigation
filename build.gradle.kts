plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.20"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "cn.tinyue"
version = "1.2.2"

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
            intellijIdeaCommunity("2025.1")
        }
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // 2025.1 平台的测试启动器仍引用 JUnit 4 类型，测试用例继续使用 Jupiter。
    testRuntimeOnly("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.14.9")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
    }
}

// 编译目标与 2025.1 的 Java 21 运行环境保持一致。
tasks.withType<JavaCompile>().configureEach { options.release.set(21) }
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild.set("251")
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
    pluginVerification {
        ides {
            current()
            providers.gradleProperty("verificationIdePath").orNull?.let { local(it) }
        }
    }
}

tasks.test { useJUnitPlatform() }

// 将唯一许可正文附入安装包，避免分发时丢失授权说明。
tasks.processResources {
    from("LICENSE.txt") { into("META-INF") }
}
