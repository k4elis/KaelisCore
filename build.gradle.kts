plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "fr.kaelis"
version = "1.0.0"
description = "Plugin survie complet pour serveurs Minecraft"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")
}

dependencies {
    // Paper API 1.21.1
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    
    // Vault API for economy integration
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    
    // PlaceholderAPI
    compileOnly("me.clip:placeholderapi:2.11.6")
    
    // LuckPerms API
    compileOnly("net.luckperms:api:5.4")
    
    // Database
    implementation("com.zaxxer:HikariCP:5.1.0")
    
    // Discord integration
    implementation("net.dv8tion:JDA:5.0.0-beta.24") {
        exclude(module = "opus-java")
    }
    
    // JSON processing (included in Paper but needed for compilation)
    compileOnly("com.google.code.gson:gson:2.10.1")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
    
    processResources {
        val props = mapOf(
            "name" to project.name,
            "version" to project.version,
            "description" to project.description,
            "apiVersion" to "1.21"
        )
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
    
    shadowJar {
        archiveClassifier.set("")
        relocate("com.zaxxer.hikari", "fr.kaelis.kaeliscore.libs.hikari")
        relocate("net.dv8tion.jda", "fr.kaelis.kaeliscore.libs.jda")
        
        minimize {
            exclude(dependency("net.dv8tion:JDA:.*"))
        }
    }
    
    build {
        dependsOn(shadowJar)
    }
}
