package org.bsdevelopment.servermaster.utils.records;

import org.bsdevelopment.servermaster.server.utils.Version;

public record UpdateInfo(String releaseUrl, String title, Version version, String markdown) {
}
