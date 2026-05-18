package net.fly.acetylcholine.agent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DependencyBypass {

    private static final Pattern VERSION_PATTERN = Pattern.compile("([0-9]+[.][0-9]+[.][0-9]+)");

    private DependencyBypass() {}

    static boolean shouldBypassDependency(String dependencyModId, String requestedRange, Object actualVersion) {
        if (!"minecraft".equals(dependencyModId) || requestedRange == null || actualVersion == null) {
            return false;
        }
        String sourceVersion = detectVersion(requestedRange);
        if (sourceVersion == null) {
            return false;
        }
        return actualVersion.toString().equals(LayerChain.promoteVersion(sourceVersion));
    }

    static boolean shouldBypassRange(String dependencyModId, String requestedRange, String actualVersion) {
        if (!"minecraft".equals(dependencyModId) || requestedRange == null || actualVersion == null) {
            return false;
        }
        String sourceVersion = detectVersion(requestedRange);
        if (sourceVersion == null) {
            return false;
        }
        return actualVersion.equals(LayerChain.promoteVersion(sourceVersion));
    }

    private static String detectVersion(String text) {
        Matcher matcher = VERSION_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }
}
