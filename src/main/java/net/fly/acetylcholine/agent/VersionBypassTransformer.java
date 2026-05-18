package net.fly.acetylcholine.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

final class VersionBypassTransformer implements ClassFileTransformer {

    private static final String MOD_SORTER = "net/minecraftforge/fml/loading/ModSorter";
    private static final String VERSION_SUPPORT_MATRIX = "net/minecraftforge/fml/loading/VersionSupportMatrix";

    private final Instrumentation inst;
    private volatile boolean moduleOpened;

    VersionBypassTransformer(Instrumentation inst) {
        this.inst = inst;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className == null) {
            return null;
        }

        if (!moduleOpened && loader != null && className.startsWith("net/fly/acetylcholine/")) {
            openAcetylcholineModule(loader);
        }

        if (className.equals(MOD_SORTER)) {
            AgentClassInjector.ensureVisible(loader);
            return ForgePatchers.transformModSorter(classfileBuffer);
        }

        if (className.equals(VERSION_SUPPORT_MATRIX)) {
            AgentClassInjector.ensureVisible(loader);
            return ForgePatchers.transformVersionSupportMatrix(classfileBuffer);
        }

        if (className.startsWith("net/minecraftforge/client/gui/") && className.contains("ModList")) {
            AgentClassInjector.ensureVisible(loader);
            return ForgePatchers.transformModListClass(className, classfileBuffer);
        }

        return null;
    }

    private synchronized void openAcetylcholineModule(ClassLoader loader) {
        if (moduleOpened) {
            return;
        }
        ModuleOpener.openJavaLangToAcetylcholine(inst, loader);
        moduleOpened = true;
    }
}
