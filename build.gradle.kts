plugins {
    java
}

group = "com.witherstorm"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Paper 26.1.1 (Minecraft 1.21.11). Unobfuscated API as of 26.1.
    compileOnly("io.papermc.paper:paper-api:26.1.1-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.jar {
    archiveBaseName.set("Witherstorm")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
}
