package org.bsdevelopment.servermaster.instance.server.thread;

import org.bsdevelopment.servermaster.instance.server.Server;

public interface ServerThreadCallback {
    void call(Server server, int statusCode);
}
