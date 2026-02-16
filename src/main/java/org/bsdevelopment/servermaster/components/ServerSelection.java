package org.bsdevelopment.servermaster.components;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class ServerSelection {
    private String serverType;
    private String serverVersion;
    private String serverBuild;

    public ServerSelection(String serverType, String serverVersion, String serverBuild) {
        this.serverType = serverType;
        this.serverVersion = serverVersion;
        this.serverBuild = serverBuild;
    }
}
