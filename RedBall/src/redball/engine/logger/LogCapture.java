package redball.engine.logger;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayDeque;

public class LogCapture {
    private static final ArrayDeque<LogLine> logs = new ArrayDeque<>();
    private static final PrintStream originalOut = System.out;
    private static final PrintStream originalErr = System.err;

    private static PrintStream captureLog(boolean isError) {
        PrintStream capture = new PrintStream(new OutputStream() {
            private final StringBuilder buffer = new StringBuilder();

            @Override
            public void write(int b) {
                char c = (char) b;
                buffer.append(c);
                if (c == '\n') flush();
            }

            @Override
            public void write(byte[] b, int off, int len) {
                buffer.append(new String(b, off, len));
                flush();
            }

            public void flush() {
                logs.addLast(new LogLine(buffer.toString(), isError));
                buffer.setLength(0);
            }
        });
        return capture;
    }

    public static void start() {
        System.setOut(captureLog(false));
        System.setErr(captureLog(true));
    }

    public static ArrayDeque<LogLine> getLogs() {
        return logs;
    }

    public static void clear() {
        logs.clear();
    }

    public static void stop() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
}