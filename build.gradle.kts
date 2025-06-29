plugins {
    id("java")
}

group = "io.github.unjoinable"
version = "1.0.0-BETA"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.kyori:adventure-api:4.22.0")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("org.jspecify:jspecify:1.0.0")
}
