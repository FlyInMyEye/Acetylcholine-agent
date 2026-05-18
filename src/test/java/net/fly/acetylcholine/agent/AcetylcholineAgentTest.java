package net.fly.acetylcholine.agent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcetylcholineAgentTest {

    private final Path modsDir = Path.of("mods");

    @BeforeEach
    void setUp() throws Exception {
        Files.createDirectories(modsDir);
        resetLayerCache();
    }

    @AfterEach
    void tearDown() throws Exception {
        resetLayerCache();
        if (Files.isDirectory(modsDir)) {
            try (var paths = Files.walk(modsDir)) {
                paths.sorted(Comparator.reverseOrder())
                        .filter(path -> path.equals(modsDir) || path.getFileName().toString().startsWith("acet-test-layer-"))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception ignored) {
                            }
                        });
            }
        }
    }

    @Test
    void bypassDependencyOnlyWhenLayerChainReachesActualVersion() throws Exception {
        writeLayerJar("1.20.1", "1.20.2");

        assertFalse(AcetylcholineAgent.shouldBypassDependency("minecraft", "[1.20.1,1.20.1]", "1.20.3"));
        assertTrue(AcetylcholineAgent.shouldBypassDependency("minecraft", "[1.20.1,1.20.1]", "1.20.2"));
    }

    @Test
    void bypassRangeOnlyWhenLayerChainReachesActualVersion() throws Exception {
        writeLayerJar("1.20.1", "1.20.2");

        assertFalse(AcetylcholineAgent.shouldBypassRange("minecraft", "[1.20.1,1.20.1]", "1.20.3"));
        assertTrue(AcetylcholineAgent.shouldBypassRange("minecraft", "[1.20.1,1.20.1]", "1.20.2"));
    }

    @Test
    void bypassFollowsAdjacentLayerChain() throws Exception {
        writeLayerJar("1.20.1", "1.20.2");
        writeLayerJar("1.20.2", "1.20.3");

        assertTrue(AcetylcholineAgent.shouldBypassDependency("minecraft", "[1.20.1,1.20.1]", "1.20.3"));
        assertTrue(AcetylcholineAgent.shouldBypassRange("minecraft", "[1.20.1,1.20.1]", "1.20.3"));
    }

    private void writeLayerJar(String sourceVersion, String targetVersion) throws Exception {
        Path jar = modsDir.resolve("acet-test-layer-" + sourceVersion + "-" + targetVersion + ".jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new JarEntry("acetylcholine-layer.json"));
            String json = "{\"sourceVersion\":\"" + sourceVersion + "\",\"targetVersion\":\"" + targetVersion + "\"}";
            out.write(json.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
    }

    private static void resetLayerCache() throws Exception {
        LayerChain.reset();
    }
}
