package net.fly.acetylcholine.agent;

final class ModListDecoration {

    private static final String TRANSLATED_MODS_PROPERTY = "acetylcholine.translatedMods";

    private ModListDecoration() {}

    static boolean isTranslatedModId(String modId) {
        if (modId == null) {
            return false;
        }
        String prop = System.getProperty(TRANSLATED_MODS_PROPERTY, "");
        if (prop.isEmpty()) {
            return false;
        }
        for (String id : prop.split(",")) {
            if (id.equals(modId)) {
                return true;
            }
        }
        return false;
    }

    static String decorateVersionString(String version, Object modInfo) {
        try {
            String modId = (String) modInfo.getClass().getMethod("getModId").invoke(modInfo);
            if (isTranslatedModId(modId)) {
                return version + " - Translated";
            }
        } catch (Exception ignored) {
        }
        return version;
    }

    static int translatedColor(int defaultColor, Object modInfo) {
        try {
            String modId = (String) modInfo.getClass().getMethod("getModId").invoke(modInfo);
            if (isTranslatedModId(modId)) {
                return 0xFFD700;
            }
        } catch (Exception ignored) {
        }
        return defaultColor;
    }
}
