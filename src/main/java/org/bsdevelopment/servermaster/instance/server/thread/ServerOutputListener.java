package org.bsdevelopment.servermaster.instance.server.thread;

import org.bsdevelopment.servermaster.instance.server.Server;

@FunctionalInterface
public interface ServerOutputListener {

    enum Stream {
        STDOUT,
        STDERR
    }

    void onLine(Server server, Stream stream, String line);
}
