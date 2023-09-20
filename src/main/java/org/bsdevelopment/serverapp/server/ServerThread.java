package org.bsdevelopment.serverapp.server;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The {@code ServerThread} class represents a thread responsible for managing
 * the execution of a server process and handling input and output streams.
 * It allows for starting, stopping, and killing the server gracefully.
 */
public class ServerThread extends Thread {

    // Logger for logging events and errors
    private final Logger log = Logger.getLogger("");

    // ProcessBuilder for launching the server process
    private final ProcessBuilder builder;

    // The server associated with this thread
    private final Server server;

    // Callback to notify events related to server thread execution
    private final ServerThreadCallback callback;

    // The running server process
    private Process process;

    // PrintWriter for sending commands to the server process
    private PrintWriter printWriter;

    /**
     * Constructs a new {@code ServerThread} instance.
     *
     * @param server   The server associated with this thread.
     * @param pb       The ProcessBuilder for launching the server process.
     * @param callback The callback to notify events related to server thread execution.
     */
    public ServerThread(Server server, ProcessBuilder pb, ServerThreadCallback callback) {
        this.server = server;
        this.builder = pb;
        this.callback = callback;

        // Set the thread name for identification
        setName("ServerThread#" + server.getName());

        // Start the thread
        start();
    }

    @Override
    public void run() {
        try {
            // Start the server process
            process = builder.start();
            log.info("Starting server " + server.getName());
        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not start server " + server.getName() + "!", e);
        }

        // Create threads for redirecting the process's input and error streams
        new Thread(new StreamRedirector(process.getInputStream(), System.out)).start();
        new Thread(new StreamRedirector(process.getErrorStream(), System.err)).start();

        // Initialize the PrintWriter for sending commands to the server process
        printWriter = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));

        try {
            // Wait for the server process to complete
            int result = process.waitFor();

            // Notify the callback with the result
            callback.call(server, result);
        } catch (InterruptedException e) {
            // Notify the callback of interruption
            callback.call(server, -1);
            log.log(Level.SEVERE, "Server Thread for " + server.getName() + " got interrupted!", e);
        }
    }

    /**
     * Sends a message/command to the server process.
     *
     * @param message The message/command to send.
     */
    public void sendMessage(String message) {
        try {
            printWriter.println(message);
            printWriter.flush();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error while sending command " + message + " to " + server.getName(), e);
        }
    }

    /**
     * Stops the server gracefully by sending a "stop" command.
     */
    public void stopServer() {
        sendMessage("stop");
        System.out.println("[ServerMaster] Stopping the server...");
    }

    /**
     * Forcefully kills the server process.
     */
    public void killServer() {
        process.destroyForcibly();
        System.out.println("[ServerMaster] Force stopped (killed) the server");
    }

    /**
     * A private record class for redirecting input and error streams of the server process.
     */
    private record StreamRedirector(InputStream in, PrintStream out) implements Runnable {
        @Override
        public void run() {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            try {
                String line;
                while ((line = br.readLine()) != null) {
                    // Don't print empty lines or prompts
                    if (line.isEmpty() || line.equals(">")) continue;

                    out.println(" " + line);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
