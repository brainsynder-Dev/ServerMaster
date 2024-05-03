package org.bsdevelopment.servermaster.views.serverconsole.console;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.apache.commons.io.output.TeeOutputStream;
import org.bsdevelopment.servermaster.App;
import org.bsdevelopment.servermaster.utils.AdvString;
import org.bsdevelopment.servermaster.utils.AppUtilities;
import org.bsdevelopment.servermaster.utils.SpaceBreak;

import java.awt.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/*
 *  Create a simple console to display text messages.
 *
 *  Messages can be directed here from different sources. Each source can
 *  have its messages displayed in a different color.
 *
 *  Messages can either be appended to the console or inserted as the first
 *  line of the console
 *
 *  You can limit the number of lines to hold in the Document.
 */
public class MessageConsole {
    private final ServerLog serverLog;
    private final UI ui;

    /*
     *	Use the text component specified as a simply console to display
     *  text messages.
     *
     *  The messages can either be appended to the end of the console or
     *  inserted as the first line of the console.
     */
    public MessageConsole(ServerLog serverLog, UI ui) {
        this.serverLog = serverLog;
        this.ui = ui;
    }

    /*
     *  Redirect the output from the standard output to the console
     *  using the specified color and PrintStream. When a PrintStream
     *  is specified the message will be added to the Document before
     *  it is also written to the PrintStream.
     */
    public void redirectOut(Color textColor) {
        ConsoleOutputStream cos = new ConsoleOutputStream(textColor);
        logOutput(new PrintStream(cos, true), null);
    }

    /*
     *  Redirect the output from the standard error to the console
     *  using the specified color and PrintStream. When a PrintStream
     *  is specified the message will be added to the Document before
     *  it is also written to the PrintStream.
     */
    public void redirectErr(Color textColor, PrintStream printStream) {
        ConsoleOutputStream cos = new ConsoleOutputStream(textColor);
        logOutput(null, new PrintStream(cos, true));
    }

    public static void logOutput(OutputStream defaultOut, OutputStream defaultError) {
        try {
            final OutputStream logOutput = new BufferedOutputStream(new FileOutputStream(App.LOG_FILE));

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
                System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));

                try {
                    logOutput.close();
                } catch (IOException ignored) {
                }
            }));

            if (defaultOut != null) System.setOut(new PrintStream(new TeeOutputStream(defaultOut, logOutput)));
            if (defaultError != null) System.setErr(new PrintStream(new TeeOutputStream(defaultError, logOutput)));
        } catch (FileNotFoundException ex) {
            System.err.println(" Failed to create log file: " + App.LOG_FILE.getName());
        }
    }

    /*
     *	Class to intercept output from a PrintStream and add it to a Document.
     *  The output can optionally be redirected to a different PrintStream.
     *  The text displayed in the Document can be color coded to indicate
     *  the output source.
     */
    class ConsoleOutputStream extends ByteArrayOutputStream {
        private final Color textColor;

        /*
         *  Specify the option text color and PrintStream
         */
        public ConsoleOutputStream(Color textColor) {
            this.textColor = textColor;
        }

        /*
         *  Override this method to intercept the output text. Each line of text
         *  output will actually involve invoking this method twice:
         *
         *  a) for the actual text message
         *  b) for the newLine string
         *
         *  The message will be treated differently depending on whether the line
         *  will be appended or inserted into the Document
         */
        @Override
        public void flush() {
            String message = toString();

            if (message.isEmpty() || message.trim().isEmpty()) {
                ui.access(() -> {
                    Span span = new Span();
                    span.getStyle().set("padding-bottom", "19px");
                    serverLog.newMessage(span);
                });
                reset();
                return;
            }
            if (message.contains(" --- [io-") || message.contains(" --- [i-")) {
                reset();
                return;
            }

            AtomicBoolean severeError = new AtomicBoolean(false);
            AtomicReference<String> color = new AtomicReference<>((textColor == null) ? null : AppUtilities.toHex(textColor));
            if (message.contains("/WARN]:")
                    || message.contains(" WARN]:")
                    || message.contains("[WARN]:")) {
                color.set("var(--custom-warning-color)");
                error = false;
                previousColor = "";
            } else if (message.contains("/SEVERE]:")
                    || message.contains(" SEVERE]:")
                    || message.contains("[SEVERE]:")) {
                severeError.set(true);
                error = true;
                color.set("var(--custom-severe-color)");
                previousColor = color.get();
            } else if (message.contains("/INFO]:")
                    || message.contains(" INFO]:")
                    || message.contains("[INFO]:")) {
                error = false;
                previousColor = "";
            } else if (message.contains("/ERROR]:")
                    || message.contains(" ERROR]:")
                    || message.contains("[ERROR]:")) {
                error = true;
                color.set("var(--lumo-success-color)");
                previousColor = color.get();
            } else if (message.startsWith("[SERVER-MASTER]: ")
                    || message.startsWith("[ServerMaster]")
                    || message.startsWith("[SM]")) {
                color.set("var(--lumo-success-color)");
                error = false;
                previousColor = "";
            }

            if (message.contains(" INFO]: Done (")) {
                color.set("var(--custom-success-color");
                error = false;
                previousColor = "";
            }

            AtomicBoolean customMessage = new AtomicBoolean(false);
            if (message.startsWith("@WARN")) {
                color.set("var(--custom-warning-color)");
                message = message.replaceFirst("@WARN", "");
                customMessage.set(true);
            }else if (message.startsWith("@ERROR")) {
                error = true;
                color.set("var(--lumo-success-color)");
                message = message.replaceFirst("@ERROR", "");
                customMessage.set(true);
            }else if (message.startsWith("@COMMAND")) {
                color.set("var(--custom-command-color)");
                message = message.replaceFirst("@COMMAND", "");
                customMessage.set(true);
            }else if (message.startsWith("@INFO")) {
                color.set(null);
                message = message.replaceFirst("@INFO", "");
                customMessage.set(true);
            }


            AtomicReference<String> finalMessage = new AtomicReference<>(message);
            ui.access(() -> {
                HorizontalLayout layout = new HorizontalLayout();
                layout.setSpacing(false);
                layout.setPadding(false);
                layout.setMargin(false);

                if (finalMessage.get().contains("@U1")) {
                    layout.add(AppUtilities.unicodeSpan("unicode-down-right", color.get()));
                    finalMessage.set(finalMessage.get().replace("@U1", ""));
                }

                if (finalMessage.get().contains("@U2")) {
                    Span span = AppUtilities.unicodeSpan("unicode-horizontal", color.get());
                    span.getStyle().set("padding-right", "20px !important");
                    layout.add(span);
                    finalMessage.set(finalMessage.get().replace("@U2", ""));
                }

                if (finalMessage.get().contains("@U3")) {
                    Span span = AppUtilities.unicodeSpan("unicode-vertical", color.get());
                    span.getStyle().set("padding-right", "20px !important");
                    layout.add(span);
                    finalMessage.set(finalMessage.get().replace("@U3", ""));
                }

                if (finalMessage.get().contains("@U4")) {
                    layout.add(AppUtilities.unicodeSpan("unicode-up-right", color.get()));
                    finalMessage.set(finalMessage.get().replace("@U4", ""));
                }

                if (finalMessage.get().contains("@U5")) {
                    for (int i = 0; i != 12; i++) layout.add(AppUtilities.unicodeSpan("unicode-horizontal", color.get()));
                    finalMessage.set(finalMessage.get().replace("@U5", ""));
                }

                if (finalMessage.get().contains("@SPACE")) {
                    String spacing = AdvString.between("@SPACE", "@", finalMessage.get());
                    layout.add(new SpaceBreak(spacing+"px"));
                    finalMessage.set(finalMessage.get().replace("@SPACE"+spacing+"@", ""));
                }

                Span text = new Span(finalMessage.get());
                text.addClassName("consoleText");
                if (error && (!previousColor.isEmpty())) color.set(previousColor);

                if (color.get() != null) {
                    text.getStyle().set("color", color.get());
                    if (severeError.get()) text.getStyle().set("text-decoration", "underline red wavy");
                }
                if (customMessage.get()) {
                    text.getStyle().set("line-height", "19px");
                }

                if (finalMessage.get().trim().startsWith("at ")) text.getStyle().set("padding-left", "20px");
                layout.add(text);
                serverLog.newMessage(layout);
            });

            reset();
        }

        boolean error = false;
        String previousColor = "";
    }
}