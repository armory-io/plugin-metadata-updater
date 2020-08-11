plugins {
  val kotlinVersion = "1.3.72"
  kotlin("jvm") version kotlinVersion
  id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
  application
}

repositories {
  mavenCentral()
  jcenter()
}

dependencies {
  implementation("com.github.ajalt:clikt:2.7.1")
  implementation("com.squareup.okhttp3:okhttp:4.7.2")
  implementation("io.github.microutils:kotlin-logging:1.7.9")
  implementation("org.eclipse.jgit:org.eclipse.jgit:5.7.0.202003110725-r")
  implementation("org.kohsuke:github-api:1.112")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.8")
  implementation("com.google.code.gson:gson:2.8.6")

  runtimeOnly("org.slf4j:slf4j-simple:1.7.30")

  testImplementation("io.strikt:strikt-core:0.22.1")
  testImplementation("dev.minutest:minutest:1.10.0")
  testImplementation("javax.servlet:javax.servlet-api:4.0.1")
  testImplementation("com.squareup.okhttp:mockwebserver:2.7.0")
  testImplementation("org.springframework.boot:spring-boot-starter-test:2.3.1.RELEASE")

  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.0")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.4.0")
}

tasks.withType<Test> {
  useJUnitPlatform {
    includeEngines("junit-jupiter")
  }
}

application {
  mainClassName = "io.armory.plugin.metadata.MainKt"
}

ktlint {
  enableExperimentalRules.set(true)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions.jvmTarget = "11"
}
