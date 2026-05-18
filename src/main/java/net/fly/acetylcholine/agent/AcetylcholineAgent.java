package net.fly.acetylcholine.agent;

import java.lang.instrument.Instrumentation;

public class AcetylcholineAgent {

    public static void premain(String args, Instrumentation inst) {
        AgentLog.reset();
        AgentLog.info("Initializing Acetylcholine agent");
        ModuleOpener.openJavaLangToAgent(inst);
        DefineClassBridge.setup();
        inst.addTransformer(new VersionBypassTransformer(inst), true);
    }

    public static void agentmain(String args, Instrumentation inst) {
        premain(args, inst);
    }

    public static Class<?> defineInto(ClassLoader loader, String name, byte[] bytes) throws Exception {
        return DefineClassBridge.defineInto(loader, name, bytes);
    }

    public static boolean shouldBypassDependency(String dependencyModId, String requestedRange, Object actualVersion) {
        return DependencyBypass.shouldBypassDependency(dependencyModId, requestedRange, actualVersion);
    }

    public static boolean shouldBypassRange(String dependencyModId, String requestedRange, String actualVersion) {
        return DependencyBypass.shouldBypassRange(dependencyModId, requestedRange, actualVersion);
    }

    public static boolean isTranslatedModId(String modId) {
        return ModListDecoration.isTranslatedModId(modId);
    }

    public static String decorateVersionString(String version, Object modInfo) {
        return ModListDecoration.decorateVersionString(version, modInfo);
    }

    public static int translatedColor(int defaultColor, Object modInfo) {
        return ModListDecoration.translatedColor(defaultColor, modInfo);
    }
}
