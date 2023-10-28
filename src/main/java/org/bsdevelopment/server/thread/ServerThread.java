package org.bsdevelopment.server.thread;

import org.bsdevelopment.AppConfig;
import org.bsdevelopment.server.Server;
import org.bsdevelopment.utils.Utils;

import java.io.*;
import java.util.Objects;

/**
 * The {@code ServerThread} class represents a thread responsible for managing
 * the execution of a server process and handling input and output streams.
 * It allows for starting, stopping, and killing the server gracefully.
 */
public class ServerThread extends Thread {

    // ProcessBuilder for launching the server process
    private final ProcessBuilder builder;

    // The server associated with this thread
    private final Server server;

    // Callback to notify events related to server thread execution
    private final ServerThreadCallback callback;
    private ServerThreadCallback serverStopCallback;

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

    public void setServerStopCallback(ServerThreadCallback serverStopCallback) {
        this.serverStopCallback = serverStopCallback;
    }

    @Override
    public void run() {
        try {
            // Start the server process
            process = builder.start();
            AppConfig.sendAppMessage(() -> System.out.println(" Starting server " + server.getName()));
        } catch (IOException e) {
            AppConfig.sendAppMessage(() -> {
                System.out.println(" Could not start server " + server.getName() + "!");
                System.err.println(Utils.getReadableStacktrace(e));
            });
        }

        new Thread(new StreamRedirector(process.getInputStream(), System.out)).start();
        new Thread(new StreamRedirector(process.getErrorStream(), System.err)).start();

        // Initialize the PrintWriter for sending commands to the server process
        printWriter = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));

        try {
            // Wait for the server process to complete
            int result = process.waitFor();

            // Notify the callback with the result
            callback.call(server, result);
            if (this.serverStopCallback != null) this.serverStopCallback.call(server, result);
        } catch (InterruptedException e) {
            // Notify the callback of interruption
            callback.call(server, -1);
            if (this.serverStopCallback != null) this.serverStopCallback.call(server, -1);
            AppConfig.sendAppMessage(() -> {
                System.out.println(" Server Thread for " + server.getName() + " got interrupted!");
                System.err.println(Utils.getReadableStacktrace(e));
            });
        }
    }

    /**
     * Sends a message/command to the server process.
     *
     * @param message The message/command to send.
     */
    public void sendMessage(String message) {
        try {
            AppConfig.sendAppMessage(() -> System.out.println("[ServerManager] Sending Command:  " + message));
            printWriter.println(message);
            printWriter.flush();
        } catch (Exception e) {
            AppConfig.sendAppMessage(() -> {
                System.out.println(" Error while sending command " + message + " to " + server.getName());
                System.err.println(Utils.getReadableStacktrace(e));
            });
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
    private static final class StreamRedirector implements Runnable {
        private final InputStream in;
        private final PrintStream out;

        private StreamRedirector(InputStream in, PrintStream out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            try {
                String line;
                while ((line = br.readLine()) != null) {
                    // Don't print empty lines or prompts
                    if (line.isEmpty() || line.equals(">")) continue;

                    out.println(line);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        public InputStream in() {
            return in;
        }

        public PrintStream out() {
            return out;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (StreamRedirector) obj;
            return Objects.equals(this.in, that.in) &&
                    Objects.equals(this.out, that.out);
        }

        @Override
        public int hashCode() {
            return Objects.hash(in, out);
        }

        @Override
        public String toString() {
            return "StreamRedirector[" +
                    "in=" + in + ", " +
                    "out=" + out + ']';
        }

    }
}
