plugins {
    val kotlinVersion = "2.2.21"
    kotlin("jvm") version kotlinVersion
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
    application
}

repositories {
    mavenCentral()
}
kotlin {
    jvmToolchain(24) // Specifies the JDK version to use for compilation
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_24) // Sets the target JVM bytecode version
    }
}
dependencies {
    implementation("com.github.ajalt.clikt:clikt:5.0.3")
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("io.github.microutils:kotlin-logging:1.7.9")
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.4.0.202509020913-r")
    implementation("org.kohsuke:github-api:1.330")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.20.1")
    implementation("com.google.code.gson:gson:2.13.2")

    runtimeOnly("org.slf4j:slf4j-simple:1.7.30")

    testImplementation("io.strikt:strikt-core:0.35.1")
    testImplementation("dev.minutest:minutest:1.13.0")
    testImplementation("javax.servlet:javax.servlet-api:4.0.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.3.2")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.5.8")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.4.0")
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("junit-jupiter")
    }
}
application {
    mainClass = "io.armory.plugin.metadata.MainKt"
}

ktlint {
    enableExperimentalRules.set(true)
}
