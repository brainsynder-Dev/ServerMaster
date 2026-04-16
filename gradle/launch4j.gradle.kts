val launch4jDir: String? = findProperty("launch4jDir") as String?

val distDir = layout.buildDirectory.dir("dist")
val scriptDir = layout.buildDirectory.dir("scripts")
val configXml = layout.buildDirectory.file("launch4j/config.xml")
val jarFile = layout.buildDirectory.file("libs/ServerMaster.jar")
val exeFile = layout.buildDirectory.file("libs/ServerMaster.exe")
val jreDir = layout.buildDirectory.dir("libs/jre")
val iconPath = "${projectDir}/src/main/resources/images/servermaster.ico"

if (launch4jDir != null) {
    val downloadJre = tasks.register("downloadJre") {
        group = "distribution"
        description = "Downloads and extracts a Temurin 21 JRE into build/libs/jre/ for bundling"

        outputs.dir(jreDir)

        onlyIf { !jreDir.get().file("release").asFile.exists() }

        doLast {
            val jreDest = jreDir.get().asFile
            jreDest.mkdirs()

            val tmpZip = File(jreDest.parent, "jre-download.zip")
            val apiUrl = "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse"

            logger.lifecycle("  Downloading Temurin 21 JRE from Adoptium (this may take a moment)...")

            val connection = java.net.URI(apiUrl).toURL().openConnection() as java.net.HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connect()
            connection.inputStream.use { input ->
                tmpZip.outputStream().use { output -> input.copyTo(output) }
            }

            logger.lifecycle("  Extracting JRE...")

            project.copy {
                from(zipTree(tmpZip)) {
                    eachFile {
                        relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
                    }
                    includeEmptyDirs = false
                }
                into(jreDest)
            }

            tmpZip.delete()
            logger.lifecycle("  JRE ready at: ${jreDest.absolutePath}")
        }
    }

    val generateConfig = tasks.register("generateLaunch4jConfig") {
        group = "distribution"
        description = "Generates the Launch4J XML configuration file"

        dependsOn(downloadJre)
        outputs.file(configXml)

        doLast {
            val version = project.version.toString()
            val fileVersion = if (version.count { it == '.' } < 3) "$version.0" else version

            val xml = configXml.get().asFile
            xml.parentFile.mkdirs()
            xml.writeText(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <launch4jConfig>
                  <dontWrapJar>false</dontWrapJar>
                  <headerType>gui</headerType>
                  <jar>${jarFile.get().asFile.absolutePath}</jar>
                  <outfile>${exeFile.get().asFile.absolutePath}</outfile>
                  <errTitle></errTitle>
                  <cmdLine></cmdLine>
                  <chdir>.</chdir>
                  <priority>normal</priority>
                  <downloadUrl>https://github.com/brainsynder-Dev/ServerMaster/releases</downloadUrl>
                  <supportUrl>https://github.com/brainsynder-Dev/ServerMaster</supportUrl>
                  <stayAlive>false</stayAlive>
                  <restartOnCrash>false</restartOnCrash>
                  <manifest></manifest>
                  <icon>$iconPath</icon>
                  <singleInstance>
                    <mutexName>ServerMaster</mutexName>
                    <windowTitle>ServerMaster</windowTitle>
                  </singleInstance>
                  <jre>
                    <path>jre</path>
                    <bundledJre64Bit>true</bundledJre64Bit>
                    <bundledJreAsFallback>false</bundledJreAsFallback>
                    <requiresJdk>false</requiresJdk>
                    <requires64Bit>false</requires64Bit>
                    <minVersion>21</minVersion>
                    <maxVersion></maxVersion>
                    <initialHeapSize>512</initialHeapSize>
                    <maxHeapSize>1024</maxHeapSize>
                  </jre>
                  <versionInfo>
                    <fileVersion>$fileVersion</fileVersion>
                    <txtFileVersion>$version</txtFileVersion>
                    <fileDescription>ServerMaster</fileDescription>
                    <copyright>Copyright (C) 2025-2026 BSDevelopment</copyright>
                    <productVersion>$fileVersion</productVersion>
                    <txtProductVersion>$version</txtProductVersion>
                    <productName>ServerMaster</productName>
                    <companyName>BSDevelopment</companyName>
                    <internalName>ServerMaster</internalName>
                    <originalFilename>ServerMaster.exe</originalFilename>
                    <trademarks></trademarks>
                    <language>ENGLISH_US</language>
                  </versionInfo>
                </launch4jConfig>
            """.trimIndent()
            )
        }
    }

    tasks.register<Exec>("packageExe") {
        group = "distribution"
        description = "Wraps ServerMaster.jar into ServerMaster.exe using Launch4J (with bundled JRE)"

        dependsOn("shadowJar", generateConfig, downloadJre)

        val isWindows = System.getProperty("os.name").lowercase().contains("win")

        if (isWindows) {
            commandLine("$launch4jDir/launch4jc.exe", configXml.get().asFile.absolutePath)
        } else {
            commandLine("java", "-jar", "$launch4jDir/launch4j.jar", configXml.get().asFile.absolutePath)
        }

        inputs.file(jarFile)
        inputs.file(configXml)
        outputs.file(exeFile)
    }

    val plainExeFile = layout.buildDirectory.file("dist/ServerMaster.exe")
    val plainConfigXml = layout.buildDirectory.file("launch4j/config-plain.xml")

    val generatePlainConfig = tasks.register("generatePlainConfig") {
        group = "distribution"
        description = "Generates the Launch4J XML configuration for the plain (no JRE) EXE"

        outputs.file(plainConfigXml)

        doLast {
            val version = project.version.toString()
            val fileVersion = if (version.count { it == '.' } < 3) "$version.0" else version

            val xml = plainConfigXml.get().asFile
            xml.parentFile.mkdirs()
            xml.writeText(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <launch4jConfig>
                  <dontWrapJar>false</dontWrapJar>
                  <headerType>gui</headerType>
                  <jar>${jarFile.get().asFile.absolutePath}</jar>
                  <outfile>${plainExeFile.get().asFile.absolutePath}</outfile>
                  <errTitle></errTitle>
                  <cmdLine></cmdLine>
                  <chdir>.</chdir>
                  <priority>normal</priority>
                  <downloadUrl>https://github.com/brainsynder-Dev/ServerMaster/releases</downloadUrl>
                  <supportUrl>https://github.com/brainsynder-Dev/ServerMaster</supportUrl>
                  <stayAlive>false</stayAlive>
                  <restartOnCrash>false</restartOnCrash>
                  <manifest></manifest>
                  <icon>$iconPath</icon>
                  <singleInstance>
                    <mutexName>ServerMaster</mutexName>
                    <windowTitle>ServerMaster</windowTitle>
                  </singleInstance>
                  <jre>
                    <path>%JAVA_HOME%;%PATH%</path>
                    <requiresJdk>false</requiresJdk>
                    <requires64Bit>false</requires64Bit>
                    <minVersion>21</minVersion>
                    <maxVersion></maxVersion>
                    <initialHeapSize>512</initialHeapSize>
                    <maxHeapSize>1024</maxHeapSize>
                  </jre>
                  <versionInfo>
                    <fileVersion>$fileVersion</fileVersion>
                    <txtFileVersion>$version</txtFileVersion>
                    <fileDescription>ServerMaster</fileDescription>
                    <copyright>Copyright (C) 2025-2026 BSDevelopment</copyright>
                    <productVersion>$fileVersion</productVersion>
                    <txtProductVersion>$version</txtProductVersion>
                    <productName>ServerMaster</productName>
                    <companyName>BSDevelopment</companyName>
                    <internalName>ServerMaster</internalName>
                    <originalFilename>ServerMaster.exe</originalFilename>
                    <trademarks></trademarks>
                    <language>ENGLISH_US</language>
                  </versionInfo>
                </launch4jConfig>
            """.trimIndent()
            )
        }
    }

    tasks.register<Exec>("packageExePlain") {
        group = "distribution"
        description = "Builds ServerMaster.exe (requires system Java 21+, no JRE bundled)"

        dependsOn("shadowJar", generatePlainConfig)

        val isWindows = System.getProperty("os.name").lowercase().contains("win")

        if (isWindows) {
            commandLine("$launch4jDir/launch4jc.exe", plainConfigXml.get().asFile.absolutePath)
        } else {
            commandLine("java", "-jar", "$launch4jDir/launch4j.jar", plainConfigXml.get().asFile.absolutePath)
        }

        inputs.file(jarFile)
        inputs.file(plainConfigXml)
        outputs.file(plainExeFile)
    }

} else {
    tasks.register("packageExe") {
        group = "distribution"
        description = "Wraps ServerMaster.jar into ServerMaster.exe using Launch4J (not configured)"

        doLast {
            logger.lifecycle("")
            logger.lifecycle("  packageExe skipped — Launch4J is not configured.")
            logger.lifecycle("  To enable, add this to your local gradle.properties:")
            logger.lifecycle("    launch4jDir=C:/path/to/launch4j")
            logger.lifecycle("")
        }
    }
}

tasks.register<Zip>("packageWindowsDist") {
    group = "distribution"
    description = "Packages ServerMaster.exe + bundled JRE into ServerMaster-Bundled.zip"

    dependsOn("packageExe")

    archiveFileName.set("ServerMaster-Bundled.zip")
    destinationDirectory.set(distDir)

    onlyIf {
        if (launch4jDir == null) {
            logger.lifecycle("  packageWindowsDist skipped — set launch4jDir in gradle.properties")
            false
        } else true
    }

    from(exeFile) { into("ServerMaster") }
    from(jreDir) { into("ServerMaster/jre") }
}