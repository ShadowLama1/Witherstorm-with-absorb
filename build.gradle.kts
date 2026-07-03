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
    // Offizielles Paper 26.1.2 API-Format für automatische Build-Zuweisung
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
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
