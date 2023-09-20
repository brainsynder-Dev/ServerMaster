package org.bsdevelopment.serverapp.server;

/**
 * The {@code ServerThreadCallback} interface defines a callback mechanism
 * for notifying events related to server thread execution. Classes that
 * implement this interface can receive notifications when a server thread
 * completes its execution, providing information about the associated server
 * and the status code.
 */
public interface ServerThreadCallback {

    /**
     * Called when a server thread completes its execution.
     *
     * @param server     The server associated with the completed thread.
     * @param statusCode The status code indicating the result of the server thread execution.
     *                   Typically, a status code of 0 represents successful execution,
     *                   while non-zero values indicate errors or specific outcomes.
     */
    void call(Server server, int statusCode);
}
