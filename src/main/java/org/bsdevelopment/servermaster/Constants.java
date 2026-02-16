package org.bsdevelopment.servermaster;

import org.bsdevelopment.servermaster.utils.JavaInstallationManager;
import org.bsdevelopment.servermaster.utils.MemoryUnit;
import oshi.SystemInfo;

import java.io.File;
import java.nio.file.Path;
import java.util.logging.Logger;

public interface Constants {
    Logger LOGGER = Logger.getLogger("ServerMaster");
    Path WORKING_PATH = new File(".").toPath();

    SystemInfo SYSTEM_INFO = new SystemInfo();
    long MAX_RAM = SYSTEM_INFO.getHardware().getMemory().getAvailable();
    long MAX_GB = MemoryUnit.GIGABYTE.convert(MAX_RAM);

    JavaInstallationManager JAVA_MANAGER = new JavaInstallationManager();
}
