package net.fly.acetylcholine.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

final class ForgePatchers {

    private static final String AGENT_OWNER = "net/fly/acetylcholine/agent/AcetylcholineAgent";

    private ForgePatchers() {}

    static byte[] transformModSorter(byte[] classfileBuffer) {
        try {
            ClassNode classNode = read(classfileBuffer);
            for (MethodNode method : classNode.methods) {
                if (method.name.equals("modVersionNotContained") &&
                        method.desc.equals("(Lnet/minecraftforge/forgespi/language/IModInfo$ModVersion;Ljava/util/Map;)Z")) {
                    method.instructions.insert(modSorterBypass());
                    return write(classNode);
                }
            }
            return null;
        } catch (Exception e) {
            AgentLog.error("Error transforming ModSorter", e);
            return null;
        }
    }

    static byte[] transformVersionSupportMatrix(byte[] classfileBuffer) {
        try {
            ClassNode classNode = read(classfileBuffer);
            for (MethodNode method : classNode.methods) {
                if (method.name.equals("testVersionSupportMatrix") &&
                        method.desc.equals("(Lorg/apache/maven/artifact/versioning/VersionRange;Ljava/lang/String;Ljava/lang/String;Ljava/util/function/BiPredicate;)Z")) {
                    method.instructions.insert(versionSupportMatrixBypass());
                    return write(classNode);
                }
            }
            return null;
        } catch (Exception e) {
            AgentLog.error("Error transforming VersionSupportMatrix", e);
            return null;
        }
    }

    static byte[] transformModListClass(String className, byte[] classfileBuffer) {
        try {
            ClassNode classNode = read(classfileBuffer);
            String modInfoField = findModInfoField(classNode);
            if (modInfoField == null) {
                return null;
            }

            boolean patched = false;
            for (MethodNode method : classNode.methods) {
                if (patchModListMethod(classNode, method, modInfoField)) {
                    patched = true;
                }
            }

            return patched ? write(classNode) : null;
        } catch (Exception e) {
            AgentLog.error("Error transforming mod-list class " + className, e);
            return null;
        }
    }

    private static boolean patchModListMethod(ClassNode classNode, MethodNode method, String modInfoField) {
        boolean decorated = false;
        for (var insn : method.instructions.toArray()) {
            if (!(insn instanceof MethodInsnNode min)
                    || min.getOpcode() != Opcodes.INVOKESTATIC
                    || !"net/minecraftforge/common/util/MavenVersionStringHelper".equals(min.owner)
                    || !"artifactVersionToString".equals(min.name)
                    || !min.desc.endsWith(")Ljava/lang/String;")) {
                continue;
            }
            InsnList inject = new InsnList();
            inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
            inject.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, modInfoField, "Lnet/minecraftforge/forgespi/language/IModInfo;"));
            inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    AGENT_OWNER,
                    "decorateVersionString",
                    "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;", false));
            method.instructions.insert(insn, inject);
            method.maxStack = Math.max(method.maxStack, 2);
            decorated = true;
            break;
        }
        if (!decorated) {
            return false;
        }
        for (var insn : method.instructions.toArray()) {
            if (!(insn instanceof LdcInsnNode ldc)
                    || !(ldc.cst instanceof Integer color)
                    || color != 0xCCCCCC) {
                continue;
            }
            InsnList inject = new InsnList();
            inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
            inject.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, modInfoField, "Lnet/minecraftforge/forgespi/language/IModInfo;"));
            inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    AGENT_OWNER,
                    "translatedColor",
                    "(ILjava/lang/Object;)I", false));
            method.instructions.insert(insn, inject);
            method.maxStack = Math.max(method.maxStack, 2);
            break;
        }
        return true;
    }

    private static InsnList modSorterBypass() {
        LabelNode continueLabel = new LabelNode();
        InsnList inject = new InsnList();
        inject.add(new VarInsnNode(Opcodes.ALOAD, 1));
        inject.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                "net/minecraftforge/forgespi/language/IModInfo$ModVersion",
                "getModId", "()Ljava/lang/String;", true));
        inject.add(new VarInsnNode(Opcodes.ALOAD, 1));
        inject.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                "net/minecraftforge/forgespi/language/IModInfo$ModVersion",
                "getVersionRange", "()Lorg/apache/maven/artifact/versioning/VersionRange;", true));
        inject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "java/lang/Object", "toString", "()Ljava/lang/String;", false));
        inject.add(new VarInsnNode(Opcodes.ALOAD, 2));
        inject.add(new LdcInsnNode("minecraft"));
        inject.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true));
        inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                AGENT_OWNER,
                "shouldBypassDependency",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)Z", false));
        inject.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
        inject.add(new InsnNode(Opcodes.ICONST_0));
        inject.add(new InsnNode(Opcodes.IRETURN));
        inject.add(continueLabel);
        return inject;
    }

    private static InsnList versionSupportMatrixBypass() {
        LabelNode continueLabel = new LabelNode();
        InsnList inject = new InsnList();
        inject.add(new VarInsnNode(Opcodes.ALOAD, 1));
        inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
        inject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "java/lang/Object", "toString", "()Ljava/lang/String;", false));
        inject.add(new VarInsnNode(Opcodes.ALOAD, 2));
        inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                AGENT_OWNER,
                "shouldBypassRange",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z", false));
        inject.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
        inject.add(new InsnNode(Opcodes.ICONST_1));
        inject.add(new InsnNode(Opcodes.IRETURN));
        inject.add(continueLabel);
        return inject;
    }

    private static String findModInfoField(ClassNode classNode) {
        for (var field : classNode.fields) {
            if ("Lnet/minecraftforge/forgespi/language/IModInfo;".equals(field.desc)) {
                return field.name;
            }
        }
        return null;
    }

    private static ClassNode read(byte[] classfileBuffer) {
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);
        return classNode;
    }

    private static byte[] write(ClassNode classNode) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }
}
