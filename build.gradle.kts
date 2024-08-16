plugins {
    id("java")
    id("com.github.ben-manes.versions") version "0.51.0"
    application
}

group = "uk.dioxic.mongo.secrets"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val log4Version= "2.23.1"
val junitVersion="5.11.0"
val assertjVersion="3.26.3"
val mongoVersion= "5.1.3"
val mongoCryptVersion= "1.11.0"

dependencies {
    implementation("info.picocli:picocli:4.7.6")
    implementation("org.mongodb:mongodb-driver-sync:$mongoVersion")
    implementation("org.mongodb:mongodb-crypt:$mongoCryptVersion")
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation(platform("org.apache.logging.log4j:log4j-bom:$log4Version"))
    implementation("org.apache.logging.log4j:log4j-core")
    implementation("org.apache.logging.log4j:log4j-api")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl")

    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
}

application {
    mainClass.set("uk.dioxic.mongo.secrets.Cli")
    applicationDefaultJvmArgs = listOf("-ea")
}

tasks.test {
    useJUnitPlatform()
}