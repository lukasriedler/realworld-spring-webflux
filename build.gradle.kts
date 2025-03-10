import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
    id("com.diffplug.spotless") version "6.6.1"
    id("org.springframework.boot") version "2.7.0"
}

group = "com.lukasriedler"
version = "0.1-ALPHA"

repositories {
    mavenCentral()
}

dependencies {
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = "1.6.2")
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-reactor", version = "1.6.2")
    implementation(group = "org.springframework.boot", name = "spring-boot-starter-webflux", version = "2.7.0")
    implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = "2.13.3")
    implementation(group = "com.auth0", name = "java-jwt", version = "3.19.2")

    testImplementation(kotlin("test"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
        ktlint("0.45.2")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
