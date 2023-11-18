plugins {
    id("net.kyori.indra") version "3.1.3"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

version = "1.1.0"

repositories {
    mavenCentral()
    maven("https://maven.xpdustry.com/mindustry")
}

dependencies {
    implementation("com.github.Anuken.Arc:arc-core:v146")
    implementation("com.github.Anuken.Arc:arcnet:v146")
}

indra {
    mitLicense()
    javaVersions {
        target(17)
    }
}

tasks.shadowJar {
    archiveFileName.set("claj-server.jar")
    manifest.attributes["Main-Class"] = "com.xpdustry.claj.server.Main"
}

tasks.register<JavaExec>("runClajServer") {
    classpath(tasks.shadowJar)
    mainClass.set("com.xpdustry.claj.server.Main")
    args("8000")
    standardInput = System.`in`
}
