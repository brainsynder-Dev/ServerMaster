import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val javafxVersion = "21.0.1"

plugins {
    application
    `java-library`
    alias(libs.plugins.shadow)
    id("org.openjfx.javafxplugin") version "0.0.14"
    id("io.freefair.lombok") version "9.0.0"
//    id("org.beryx.runtime") version "2.0.1"
}

group = "org.bsdevelopment.servermaster"
version = "1.6"

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

//tasks.named<Jar>("jar") {
//    manifest {
//        attributes["Main-Class"] = application.mainClass.get()
//        attributes["Implementation-Version"] = project.version.toString()
//    }
//    // If you ONLY want the shadow jar output, uncomment:
//    // enabled = false
//}

tasks.named("build") {
    dependsOn("shadowJar")
}

tasks.named("startScripts") { enabled = false }
tasks.named("distZip") { enabled = false }
tasks.named("distTar") { enabled = false }
tasks.named("startShadowScripts") { enabled = false }
tasks.named("shadowDistTar") { enabled = false }
tasks.named("shadowDistZip") { enabled = false }

//runtime {
//    options = listOf(
//        "--strip-debug",
//        "--compress", "2",
//        "--no-header-files",
//        "--no-man-pages")
//
//    jpackage {
//        val os = org.gradle.internal.os.OperatingSystem.current()
//
//        val iconPath = when {
//            os.isWindows -> "src/main/resources/images/servermaster.ico"
//            os.isMacOsX -> "src/main/resources/images/servermaster.icns"
//            else -> "src/main/resources/images/servermaster.png"
//        }
//
//        if (os.isWindows) {
//            installerType = "msi"
//        }
//
//        imageOptions = listOf("--icon", iconPath)
//
//        installerOptions = buildList {
//            addAll(listOf(
//                "--vendor", "BSDevelopment",
//                "--description", "A server console to run different server versions and types all off of 1 server folder"
//            ))
//
//            if (os.isWindows) {
//                addAll(listOf(
//                    "--win-per-user-install",
//                    "--win-dir-chooser",
//                    "--win-menu",
//                    "--win-shortcut"
//                ))
//            } else if (os.isLinux) {
//                addAll(listOf(
//                    "--linux-package-name", "servermaster", "--linux-shortcut"
//                ))
//            } else if (os.isMacOsX) {
//                addAll(listOf(
//                    "--mac-package-name", "servermaster"
//                ))
//            }
//        }
//    }
//}

//apply(from = "gradle/r2-publish.gradle.kts")
