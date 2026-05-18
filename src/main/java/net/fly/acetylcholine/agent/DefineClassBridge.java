package net.fly.acetylcholine.agent;

import java.lang.reflect.Method;
import java.security.ProtectionDomain;

final class DefineClassBridge {

    private static volatile Method defineClass;

    private DefineClassBridge() {}

    static void setup() {
        try {
            Method method = ClassLoader.class.getDeclaredMethod(
                    "defineClass",
                    String.class, byte[].class, int.class, int.class, ProtectionDomain.class
            );
            method.setAccessible(true);
            defineClass = method;
        } catch (Exception e) {
            defineClass = null;
            AgentLog.error("defineClass bridge setup failed", e);
        }
    }

    static Class<?> defineInto(ClassLoader loader, String name, byte[] bytes) throws Exception {
        Method method = defineClass;
        if (method == null) {
            throw new IllegalStateException("defineClass bridge is unavailable");
        }
        return (Class<?>) method.invoke(loader, name, bytes, 0, bytes.length, (ProtectionDomain) null);
    }
}
