package net.fly.acetylcholine.agent;

import java.io.InputStream;

final class AgentClassInjector {

    private static final String[] REQUIRED_CLASSES = {
            "net.fly.acetylcholine.agent.AcetylcholineAgent",
            "net.fly.acetylcholine.agent.DependencyBypass",
            "net.fly.acetylcholine.agent.LayerChain",
            "net.fly.acetylcholine.agent.ModListDecoration",
            "net.fly.acetylcholine.agent.AgentLog"
    };

    private AgentClassInjector() {}

    static void ensureVisible(ClassLoader loader) {
        if (loader == null) {
            return;
        }
        try {
            Class.forName("net.fly.acetylcholine.agent.AcetylcholineAgent", false, loader);
            return;
        } catch (ClassNotFoundException ignored) {
        }
        for (String className : REQUIRED_CLASSES) {
            define(loader, className);
        }
    }

    private static void define(ClassLoader loader, String className) {
        String resource = className.substring(className.lastIndexOf('.') + 1) + ".class";
        try (InputStream inputStream = AcetylcholineAgent.class.getResourceAsStream(resource)) {
            if (inputStream == null) {
                throw new IllegalStateException("Could not locate " + resource);
            }
            AcetylcholineAgent.defineInto(loader, className, inputStream.readAllBytes());
        } catch (LinkageError ignored) {
        } catch (Exception e) {
            AgentLog.error("Failed to define " + className + " into target classloader", e);
        }
    }
}
