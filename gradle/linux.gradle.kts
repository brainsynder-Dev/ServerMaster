val appVersion = project.version.toString()
val jarFile = layout.buildDirectory.file("libs/ServerMaster.jar")
val distDir = layout.buildDirectory.dir("dist")
val linuxBuildDir = layout.buildDirectory.dir("linux")
val iconPng = "${projectDir}/src/main/resources/images/servermaster.png"
val linuxJreDir = linuxBuildDir.map { it.dir("jre") }

val downloadLinuxJre = tasks.register("downloadLinuxJre") {
    group = "distribution"
    description = "Downloads and extracts a Temurin 21 JRE for Linux x64"

    outputs.dir(linuxJreDir)
    onlyIf { !linuxJreDir.get().file("release").asFile.exists() }

    doLast {
        val dest = linuxJreDir.get().asFile
        dest.mkdirs()

        val tmp = File(dest.parentFile, "jre-linux.tar.gz")
        val url = "https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jre/hotspot/normal/eclipse"

        logger.lifecycle("  Downloading Temurin 21 JRE for Linux...")
        val conn = java.net.URI(url).toURL().openConnection() as java.net.HttpURLConnection
        conn.instanceFollowRedirects = true
        conn.connect()
        conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }

        logger.lifecycle("  Extracting Linux JRE...")
        val tarExit = ProcessBuilder("tar", "-xzf", tmp.absolutePath, "-C", dest.absolutePath, "--strip-components=1")
            .inheritIO().start().waitFor()
        check(tarExit == 0) { "tar extraction failed with exit code $tarExit" }
        tmp.delete()
        logger.lifecycle("  Linux JRE ready at: ${dest.absolutePath}")
    }
}

tasks.register("packageLinuxAppImage") {
    group = "distribution"
    description = "Creates ServerMaster-x86_64.AppImage"

    dependsOn("shadowJar", downloadLinuxJre)
    inputs.file(jarFile)

    doLast {
        val appDir = linuxBuildDir.get().dir("AppDir").asFile
        val libDir = File(appDir, "usr/lib/servermaster")
        val iconsDir = File(appDir, "usr/share/icons/hicolor/256x256/apps")
        val appsDir = File(appDir, "usr/share/applications")

        libDir.mkdirs()
        iconsDir.mkdirs()
        appsDir.mkdirs()

        project.copy { from(jarFile); into(libDir) }
        project.copy { from(linuxJreDir); into(File(libDir, "jre")) }
        project.copy { from(iconPng); into(appDir); rename { "servermaster.png" } }
        project.copy { from(iconPng); into(iconsDir); rename { "servermaster.png" } }

        val desktop = "[Desktop Entry]\n" +
            "Name=ServerMaster\n" +
            "Exec=servermaster\n" +
            "Icon=servermaster\n" +
            "Type=Application\n" +
            "Categories=Utility;\n" +
            "Comment=Minecraft Server Manager\n"
        File(appDir, "servermaster.desktop").writeText(desktop)
        File(appsDir, "servermaster.desktop").writeText(desktop)

        val d = "$"
        val appRun = File(appDir, "AppRun")
        appRun.writeText(
            "#!/bin/sh\n" +
            "HERE=\"${d}(dirname \"${d}(readlink -f \"${d}0\")\")\"\n" +
            "exec \"${d}HERE/usr/lib/servermaster/jre/bin/java\" -jar \"${d}HERE/usr/lib/servermaster/ServerMaster.jar\" \"${d}@\"\n"
        )
        java.nio.file.Files.setPosixFilePermissions(
            appRun.toPath(),
            java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-xr-x")
        )

        distDir.get().asFile.mkdirs()
        val outputAppImage = distDir.get().file("ServerMaster-x86_64.AppImage").asFile
        val appImagePb = ProcessBuilder("appimagetool", appDir.absolutePath, outputAppImage.absolutePath)
            .inheritIO()
        appImagePb.environment()["ARCH"] = "x86_64"
        appImagePb.environment()["APPIMAGE_EXTRACT_AND_RUN"] = "1"
        val appImageExit = appImagePb.start().waitFor()
        check(appImageExit == 0) { "appimagetool failed with exit code $appImageExit" }
        logger.lifecycle("  AppImage created at: ${outputAppImage.absolutePath}")
    }
}

tasks.register("packageLinuxDeb") {
    group = "distribution"
    description = "Creates servermaster_<version>_amd64.deb"

    dependsOn("shadowJar", downloadLinuxJre)
    inputs.file(jarFile)

    doLast {
        val debRoot = linuxBuildDir.get().dir("deb").asFile
        val installDir = File(debRoot, "opt/servermaster")
        val debian = File(debRoot, "DEBIAN")
        val usrBin = File(debRoot, "usr/bin")
        val appsDir = File(debRoot, "usr/share/applications")
        val iconsDir = File(debRoot, "usr/share/icons/hicolor/256x256/apps")

        listOf(installDir, debian, usrBin, appsDir, iconsDir).forEach { it.mkdirs() }

        project.copy { from(jarFile); into(installDir) }
        project.copy { from(linuxJreDir); into(File(installDir, "jre")) }
        project.copy { from(iconPng); into(iconsDir); rename { "servermaster.png" } }

        File(appsDir, "servermaster.desktop").writeText(
            "[Desktop Entry]\n" +
            "Name=ServerMaster\n" +
            "Exec=/usr/bin/servermaster\n" +
            "Icon=servermaster\n" +
            "Type=Application\n" +
            "Categories=Utility;\n" +
            "Comment=Minecraft Server Manager\n" +
            "StartupNotify=true\n"
        )

        val posix755 = java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-xr-x")

        val d = "$"
        val launcher = File(usrBin, "servermaster")
        launcher.writeText(
            "#!/bin/sh\n" +
            "exec /opt/servermaster/jre/bin/java -jar /opt/servermaster/ServerMaster.jar \"${d}@\"\n"
        )
        java.nio.file.Files.setPosixFilePermissions(launcher.toPath(), posix755)

        File(debian, "postinst").apply {
            writeText(
                "#!/bin/sh\n" +
                "set -e\n" +
                "update-desktop-database /usr/share/applications || true\n" +
                "gtk-update-icon-cache -f /usr/share/icons/hicolor || true\n"
            )
            java.nio.file.Files.setPosixFilePermissions(toPath(), posix755)
        }

        File(debian, "control").writeText(
            "Package: servermaster\n" +
            "Version: $appVersion\n" +
            "Section: utils\n" +
            "Priority: optional\n" +
            "Architecture: amd64\n" +
            "Maintainer: BSDevelopment\n" +
            "Description: Minecraft Server Manager\n" +
            " Manage Minecraft server instances with an easy-to-use GUI.\n"
        )

        distDir.get().asFile.mkdirs()
        val debOut = "${distDir.get().asFile.absolutePath}/servermaster_${appVersion}_amd64.deb"
        val debExit = ProcessBuilder("fakeroot", "dpkg-deb", "--build", debRoot.absolutePath, debOut)
            .inheritIO().start().waitFor()
        check(debExit == 0) { "dpkg-deb failed with exit code $debExit" }
        logger.lifecycle("  .deb created at: $debOut")
    }
}
