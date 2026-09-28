package demo.Backend.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DotenvLoader {

    private DotenvLoader() {
    }

    public static void load() {
        Path envFile = locateEnvFile();
        if (envFile == null) {
            return;
        }
        try {
            for (String line : Files.readAllLines(envFile)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, separator).trim();
                String value = trimmed.substring(separator + 1).trim();
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                if (System.getProperty(key) == null && System.getenv(key) == null) {
                    System.setProperty(key, value);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + envFile, exception);
        }
    }

    private static Path locateEnvFile() {
        Path workingDirectory = Path.of(System.getProperty("user.dir"));
        Path[] candidates = {
                workingDirectory.resolve(".env"),
                workingDirectory.resolve("Backend").resolve(".env")
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
