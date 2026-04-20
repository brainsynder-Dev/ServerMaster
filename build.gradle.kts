import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val javafxVersion = "21.0.1"

plugins {
    application
    `java-library`
    alias(libs.plugins.shadow)
    id("org.openjfx.javafxplugin") version "0.0.14"
    id("io.freefair.lombok") version "9.0.0"
}

group = "org.bsdevelopment.servermaster"
version = "1.6.7"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

application {
    mainClass.set("org.bsdevelopment.servermaster.Launcher")
    applicationName = "ServerMaster"
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes["Implementation-Version"] = project.version.toString()
    }
}


javafx {
    version = javafxVersion
    modules(
        "javafx.base",
        "javafx.controls",
        "javafx.fxml",
        "javafx.graphics"
    )
}

dependencies {
    implementation(libs.cssfx)
    implementation(libs.atlantafx)

    implementation("org.kordamp.ikonli:ikonli-core:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-javafx:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-fontawesome5-pack:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-feather-pack:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-material2-pack:12.3.1")

    implementation("org.fxmisc.richtext:richtextfx:0.10.9")
    implementation(libs.minimaljson)

    implementation("com.jeff-media:javafinder:1.4.4")
    implementation("com.github.oshi:oshi-core:6.9.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.0.1")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("ServerMaster")
    archiveClassifier.set("")
    archiveVersion.set("")

    manifest {
        attributes["Main-Class"] = application.mainClass.get()
        attributes["Implementation-Version"] = project.version.toString()
    }

    mergeServiceFiles()
}

tasks.named("build") {
    dependsOn("shadowJar")
}

tasks.named("startScripts") { enabled = false }
tasks.named("distZip") { enabled = false }
tasks.named("distTar") { enabled = false }
tasks.named("startShadowScripts") { enabled = false }
tasks.named("shadowDistTar") { enabled = false }
tasks.named("shadowDistZip") { enabled = false }

apply(from = "gradle/launch4j.gradle.kts")
apply(from = "gradle/linux.gradle.kts")
