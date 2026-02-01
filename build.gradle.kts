plugins {
    kotlin("jvm") version "1.9.20"
    application
}

kotlin {
    jvmToolchain(17)
}

group = "com.temporal.agentic"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin stdlib
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    
    // Temporal SDK
    implementation("io.temporal:temporal-sdk:1.24.1")
    implementation("io.temporal:temporal-testing:1.24.1")
    
    // JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.16.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.1")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.temporal.agentic.worker.TemporalWorkerStarterKt")
}
