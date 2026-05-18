package net.fly.acetylcholine.agent;

import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.Set;

final class ModuleOpener {

    private ModuleOpener() {}

    static void openJavaLangToAgent(Instrumentation inst) {
        try {
            Module javaBase = Object.class.getModule();
            Module agentModule = AcetylcholineAgent.class.getModule();
            inst.redefineModule(
                    javaBase,
                    Set.of(),
                    Map.of(),
                    Map.of("java.lang", Set.of(agentModule)),
                    Set.of(),
                    Map.of()
            );
        } catch (Exception e) {
            AgentLog.error("Failed to open java.lang to agent module", e);
        }
    }

    static void openJavaLangToAcetylcholine(Instrumentation inst, ClassLoader loader) {
        try {
            ModuleLayer layer = loader.getUnnamedModule().getLayer();
            if (layer == null) {
                return;
            }

            Module acetylcholineModule = null;
            for (Module module : layer.modules()) {
                if (module.getName() != null && module.getName().startsWith("acetylcholine.")) {
                    acetylcholineModule = module;
                    break;
                }
            }

            if (acetylcholineModule == null) {
                return;
            }

            Module javaBase = Object.class.getModule();
            inst.redefineModule(
                    javaBase,
                    Set.of(),
                    Map.of(),
                    Map.of(
                            "java.lang", Set.of(acetylcholineModule),
                            "java.lang.reflect", Set.of(acetylcholineModule)
                    ),
                    Set.of(),
                    Map.of()
            );
        } catch (Exception e) {
            AgentLog.error("Failed to open java.lang to Acetylcholine module", e);
        }
    }
}
