plugins {
    java
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

group = "com.temporal.agentic"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Temporal SDK
    implementation("io.temporal:temporal-sdk:1.22.4")
    implementation("io.temporal:temporal-testing:1.22.4")
    
    // JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.2")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.5")
    implementation("org.slf4j:slf4j-simple:2.0.5")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.3")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.temporal.agentic.worker.TemporalWorkerStarter")
}
