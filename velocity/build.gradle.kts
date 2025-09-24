plugins {
    id("feather-server-api.java-conventions")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    maven {
        name = "velocity-snapshots"
        url = uri("https://repo.velocitypowered.com/snapshots/")
    }
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.1.1")
    annotationProcessor("com.velocitypowered:velocity-api:3.1.1")
    implementation(project(":api"))
    implementation(project(":common"))
    implementation(project(":messaging"))
}