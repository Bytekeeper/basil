import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.3.31"

    kotlin("jvm") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.jpa") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.allopen") version kotlinVersion
    id("org.springframework.boot") version "2.1.5.RELEASE"
    id("io.spring.dependency-management") version "1.0.7.RELEASE"
    id("com.github.ben-manes.versions") version "0.21.0"
}

group = "ctr"
version = "1.0-SNAPSHOT"
extra["junit-jupiter.version"] = "5.4.1"
extra["mockito.version"] = "2.28.2"


repositories {
    mavenCentral()
    jcenter()
}

dependencyManagement {
    imports {
        mavenBom("org.apache.logging.log4j:log4j-bom:2.11.1")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.+")
    implementation(group = "org.postgresql", name = "postgresql", version = "42.2.5")
    implementation("org.flywaydb:flyway-core")
    implementation("org.apache.commons:commons-compress:1.18")
    implementation("org.tukaani:xz:1.8")
    implementation("io.micrometer:micrometer-registry-influx")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    testImplementation("com.h2database:h2")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.12.2")
    testImplementation("org.mockito:mockito-junit-jupiter")
}

configurations.all {
    exclude("org.springframework.boot", "spring-boot-starter-logging")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

allOpen {
    annotation("javax.persistence.Entity")
}
