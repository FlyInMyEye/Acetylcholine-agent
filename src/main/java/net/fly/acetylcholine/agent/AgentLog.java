package net.fly.acetylcholine.agent;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

final class AgentLog {

    private static final Path TRACE_FILE = Path.of("acetylcholine_agent-trace.log");

    private AgentLog() {}

    static void reset() {
        try {
            Files.deleteIfExists(TRACE_FILE);
        } catch (Exception ignored) {
        }
    }

    static void info(String message) {
        write(System.out, message, null);
    }

    static void error(String message, Throwable throwable) {
        write(System.err, message, throwable);
    }

    private static void write(java.io.PrintStream stream, String message, Throwable throwable) {
        String line = "[Acetylcholine Agent] " + message;
        stream.println(line);
        if (throwable != null) {
            throwable.printStackTrace(stream);
        }
        try {
            Files.writeString(
                    TRACE_FILE,
                    line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (Exception ignored) {
        }
    }
}
