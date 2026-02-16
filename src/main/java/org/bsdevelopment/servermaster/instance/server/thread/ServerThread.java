package org.bsdevelopment.servermaster.instance.server.thread;

import lombok.Setter;
import org.bsdevelopment.servermaster.Constants;
import org.bsdevelopment.servermaster.instance.server.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.logging.Level;

public class ServerThread extends Thread {
    private final ProcessBuilder builder;
    private final Server server;
    private final ServerThreadCallback callback;
    @Setter private ServerThreadCallback serverStopCallback;
    private final ServerOutputListener outputListener;
    private Process process;
    private PrintWriter printWriter;

    public ServerThread(Server server, ProcessBuilder pb, ServerOutputListener outputListener, ServerThreadCallback callback) {
        this.server = server;
        this.builder = pb;
        this.callback = callback;
        this.outputListener = outputListener;

        setName("ServerThread#" + server.getName());
        setUncaughtExceptionHandler((thread, throwable) -> throwable.printStackTrace());

        start();
    }

    @Override
    public void run() {
        try {
            process = builder.start();
            Constants.LOGGER.info(() -> "Starting server " + server.getName());
        } catch (IOException e) {
            Constants.LOGGER.log(Level.SEVERE, "Unable to start the server", e);
            callback.call(server, -1);
            if (this.serverStopCallback != null) this.serverStopCallback.call(server, -1);
            return;
        }

        new Thread(new StreamRedirector(server, ServerOutputListener.Stream.STDOUT, process.getInputStream(), outputListener),
                getName() + "-stdout").start();
        new Thread(new StreamRedirector(server, ServerOutputListener.Stream.STDERR, process.getErrorStream(), outputListener),
                getName() + "-stderr").start();

        printWriter = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));

        try {
            int result = process.waitFor();

            callback.call(server, result);
            if (this.serverStopCallback != null) this.serverStopCallback.call(server, result);
        } catch (InterruptedException e) {
            callback.call(server, -1);
            if (this.serverStopCallback != null) this.serverStopCallback.call(server, -1);

            Constants.LOGGER.log(Level.WARNING, "Server thread interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    public void sendMessage(String message) {
        try {
            Constants.LOGGER.fine(() -> "Executing command: '" + message + "'");
            printWriter.println(message);
            printWriter.flush();
        } catch (Exception e) {
            Constants.LOGGER.log(Level.WARNING, "Unable to execute command: '" + message + "'", e);
        }
    }

    public void stopServer() {
        sendMessage("stop");
        Constants.LOGGER.info("Stopping the server...");
    }

    public void killServer() {
        process.destroyForcibly();
        Constants.LOGGER.info("Force stopped (killed) the server");
    }

    private record StreamRedirector(Server server, ServerOutputListener.Stream stream, InputStream in, ServerOutputListener listener) implements Runnable {

        @Override
            public void run() {
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                try {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.isEmpty() || line.equals(">")) continue;
                        if (listener != null) listener.onLine(server, stream, line);
                    }
                } catch (IOException ex) {
                    Constants.LOGGER.log(Level.FINE, "Stream redirector ended", ex);
                }
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) return true;
                if (obj == null || obj.getClass() != this.getClass()) return false;
                var that = (StreamRedirector) obj;
                return Objects.equals(this.in, that.in) &&
                        Objects.equals(this.listener, that.listener) &&
                        Objects.equals(this.server, that.server) &&
                        this.stream == that.stream;
            }

        @Override
            public String toString() {
                return "StreamRedirector[" +
                        "stream=" + stream + ", " +
                        "in=" + in + ']';
            }
        }
}
