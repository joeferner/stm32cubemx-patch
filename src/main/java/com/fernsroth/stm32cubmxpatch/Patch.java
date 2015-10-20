package com.fernsroth.stm32cubmxpatch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Patch {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: <gpio root dir>");
            return;
        }

        File gpioRootDir = new File(args[0]);
        File parameterUIClassFile = new File(gpioRootDir, "com/st/microxplorer/plugins/ip/gpio/gui/ParameterUI.class");
        if (!parameterUIClassFile.exists()) {
            System.err.println(parameterUIClassFile.getAbsolutePath() + " does not point to ParameterUI.class");
            return;
        }

        byte[] newClassBytes;
        try (FileInputStream in = new FileInputStream(parameterUIClassFile)) {
            ClassReader classReader = new ClassReader(in);
            ClassNode classNode = new ClassNode();
            classReader.accept(classNode, 0);

            alterClass(classNode);

            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(classWriter);
            newClassBytes = classWriter.toByteArray();
        }

        try (FileOutputStream out = new FileOutputStream(parameterUIClassFile)) {
            out.write(newClassBytes);
        }
    }

    private static void alterClass(ClassNode classNode) {
        for (int i = 0; i < classNode.methods.size(); i++) {
            MethodNode methodNode = (MethodNode) classNode.methods.get(i);
            if (methodNode.name.equals("<init>")) {
                removeParamBoundSupportInit(methodNode);
            } else if (methodNode.name.equals("addPropertyChangeListener") || methodNode.name.equals("removePropertyChangeListener")) {
                addEnsureParamBoundSupport(methodNode);
            }
        }
        classNode.methods.add(createEnsureParamBoundSupportMethod());
    }

    private static void removeParamBoundSupportInit(MethodNode methodNode) {
        for (int i = 0; i < methodNode.instructions.size(); i++) {
            AbstractInsnNode inst = methodNode.instructions.get(i);
            if (inst instanceof FieldInsnNode && ((FieldInsnNode) inst).name.equals("paramBoundSupport")) {
                methodNode.instructions.remove(inst);
                break;
            }
        }
    }

    private static MethodNode createEnsureParamBoundSupportMethod() {
        MethodNode ensureParamBoundSupportMethod = new MethodNode(Opcodes.ACC_PRIVATE, "ensureParamBoundSupport", "()V", null, null);
        LabelNode ifNotNullLabel = new LabelNode();
        InsnList instructions = new InsnList();
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        instructions.add(new FieldInsnNode(Opcodes.GETFIELD, "com/st/microxplorer/plugins/ip/gpio/gui/ParameterUI", "paramBoundSupport", "Ljava/beans/PropertyChangeSupport;"));
        instructions.add(new JumpInsnNode(Opcodes.IFNONNULL, ifNotNullLabel));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        instructions.add(new TypeInsnNode(Opcodes.NEW, "java/beans/PropertyChangeSupport"));
        instructions.add(new InsnNode(Opcodes.DUP));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/beans/PropertyChangeSupport", "<init>", "(Ljava/lang/Object;)V", false));
        instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, "com/st/microxplorer/plugins/ip/gpio/gui/ParameterUI", "paramBoundSupport", "Ljava/beans/PropertyChangeSupport;"));
        instructions.add(ifNotNullLabel);
        instructions.add(new InsnNode(Opcodes.RETURN));
        ensureParamBoundSupportMethod.instructions.add(instructions);
        return ensureParamBoundSupportMethod;
    }

    private static void addEnsureParamBoundSupport(MethodNode methodNode) {
        InsnList newInstructions = new InsnList();
        newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        newInstructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "com/st/microxplorer/plugins/ip/gpio/gui/ParameterUI", "ensureParamBoundSupport", "()V", false));
        methodNode.instructions.insertBefore(methodNode.instructions.get(0), newInstructions);
    }
}
