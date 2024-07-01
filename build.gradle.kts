plugins {
    java
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"

    id("xyz.jpenilla.run-paper") version "2.2.2"
}

group = "com.redmagic"
version = "1.0"

val mcVersion = "1.21"

repositories {
    mavenCentral()
    maven {
        name = "spigotmc-repo"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
    maven {
        name = "undefinedapiRepo"
        url = uri("https://repo.undefinedcreation.com/repo")
    }
    maven("https://repo.codemc.io/repository/maven-snapshots/")
}


dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.undefined:api:0.5.47:mapped")

    implementation("com.google.code.gson:gson:2.11.0")
}

tasks {

    shadowJar {
        archiveFileName.set("${this.project.name}-shadow.jar")
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "21"
    }
    runServer {
        minecraftVersion(mcVersion)
    }
}

kotlin{
    jvmToolchain(21)
}