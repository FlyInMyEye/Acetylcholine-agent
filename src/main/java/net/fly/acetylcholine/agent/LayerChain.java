package net.fly.acetylcholine.agent;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LayerChain {

    private static final String LAYER_FILE = "acetylcholine-layer.json";
    private static final Pattern SOURCE_VERSION_PATTERN = Pattern.compile("\"sourceVersion\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern TARGET_VERSION_PATTERN = Pattern.compile("\"targetVersion\"\\s*:\\s*\"([^\"]+)\"");

    private static volatile Map<String, String> nextVersionBySource;

    private LayerChain() {}

    static String promoteVersion(String sourceVersion) {
        String current = sourceVersion;
        Map<String, String> hops = nextVersionBySource();
        while (true) {
            String next = hops.get(current);
            if (next == null) {
                return current;
            }
            current = next;
        }
    }

    static void reset() {
        nextVersionBySource = null;
    }

    private static Map<String, String> nextVersionBySource() {
        Map<String, String> current = nextVersionBySource;
        if (current != null) {
            return current;
        }
        synchronized (LayerChain.class) {
            if (nextVersionBySource == null) {
                nextVersionBySource = loadLayerChain();
            }
            return nextVersionBySource;
        }
    }

    private static Map<String, String> loadLayerChain() {
        Map<String, String> hops = new HashMap<>();
        for (Path modsDir : new Path[]{Path.of("run", "mods"), Path.of("mods")}) {
            if (!Files.isDirectory(modsDir)) {
                continue;
            }
            readLayers(modsDir, hops);
        }
        return hops;
    }

    private static void readLayers(Path modsDir, Map<String, String> hops) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir, "*.jar")) {
            for (Path jarPath : stream) {
                readLayer(jarPath, hops);
            }
        } catch (Exception e) {
            AgentLog.error("Failed scanning mods directory " + modsDir, e);
        }
    }

    private static void readLayer(Path jarPath, Map<String, String> hops) {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry layerEntry = jar.getJarEntry(LAYER_FILE);
            if (layerEntry == null) {
                return;
            }
            try (InputStream inputStream = jar.getInputStream(layerEntry)) {
                String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                String source = extractJsonValue(json, SOURCE_VERSION_PATTERN);
                String target = extractJsonValue(json, TARGET_VERSION_PATTERN);
                if (source != null && target != null) {
                    hops.put(source, target);
                }
            }
        } catch (Exception e) {
            AgentLog.error("Failed reading layer metadata from " + jarPath.getFileName(), e);
        }
    }

    private static String extractJsonValue(String json, Pattern pattern) {
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }
}
