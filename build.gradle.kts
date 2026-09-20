plugins {
    application
    id("com.gradleup.shadow") version "9.5.0"
}

application {
    mainClass.set("com.mihastele.core.FileIntegrityMonitor")
}

group = "com.mihastele"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}