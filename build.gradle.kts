plugins {
    java
    id("io.freefair.lombok") version "8.11"
    id("com.gradleup.shadow") version "8.3.5"
    id("maven-publish")
}

group = "io.lightstudios.coins"
version = "0.1.1"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "jitpack"
        url = uri("https://jitpack.io")
    }

    maven {
        name = "codemc"
        url = uri("https://repo.codemc.org/repository/maven-public")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    compileOnly("com.github.lightPlugins:lightCore:0.4.4")
    compileOnly("net.milkbowl.vault:VaultUnlockedAPI:2.9")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    processResources {
        from(sourceSets.main.get().resources.srcDirs()) {
            filesMatching("plugin.yml") {
                expand(
                    "name" to rootProject.name,
                    "version" to rootProject.version
                )

            }
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveClassifier.set("")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(tasks.shadowJar.get()) {
                classifier = null
            }
            groupId = "com.github.lightPlugins"
            artifactId = "lightCoins"
            version = rootProject.version.toString()
        }
    }
}

tasks.named("publishMavenPublicationToMavenLocal") {
    dependsOn(tasks.shadowJar)
    dependsOn(tasks.jar)
}