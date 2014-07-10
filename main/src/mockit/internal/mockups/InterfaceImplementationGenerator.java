/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.mockups;

import java.util.*;

import org.jetbrains.annotations.*;

import mockit.external.asm4.*;
import mockit.internal.*;

import static mockit.external.asm4.Opcodes.*;

public final class InterfaceImplementationGenerator extends BaseClassModifier
{
   private static final int ACC_CLASS = ACC_PUBLIC + ACC_FINAL;

   @NotNull private final List<String> implementedMethods;
   @NotNull private final String implementationClassName;
   private String[] initialSuperInterfaces;

   public InterfaceImplementationGenerator(@NotNull ClassReader classReader, @NotNull String implementationClassName)
   {
      super(classReader);
      implementedMethods = new ArrayList<String>();
      this.implementationClassName = implementationClassName.replace('.', '/');
   }

   @Override
   public void visit(
      int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superName,
      @Nullable String[] interfaces)
   {
      initialSuperInterfaces = interfaces;
      String[] implementedInterfaces = {name};
      super.visit(version, ACC_CLASS, implementationClassName, signature, superName, implementedInterfaces);
      generateNoArgsConstructor();
   }

   private void generateNoArgsConstructor()
   {
      mw = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
      mw.visitVarInsn(ALOAD, 0);
      mw.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
      generateEmptyImplementation();
   }

   @Override
   public void visitInnerClass(@NotNull String name, String outerName, String innerName, int access) {}

   @Override
   public void visitOuterClass(@NotNull String owner, @Nullable String name, @Nullable String desc) {}

   @Override
   public void visitAttribute(Attribute attr) {}

   @Override
   public void visitSource(@Nullable String source, @Nullable String debug) {}

   @Override @Nullable
   public FieldVisitor visitField(
      int access, @NotNull String name, @NotNull String desc, @Nullable String signature, @Nullable Object value)
   { return null; }

   @Override @Nullable
   public MethodVisitor visitMethod(
      int access, @NotNull String name, @NotNull String desc, @Nullable String signature, @Nullable String[] exceptions)
   {
      if (name.charAt(0) != '<') { // ignores an eventual "<clinit>" class initialization "method"
         generateMethodImplementation(name, desc, signature, exceptions);
      }

      return null;
   }

   private void generateMethodImplementation(
      @NotNull String name, @NotNull String desc, @Nullable String signature, @Nullable String[] exceptions)
   {
      String methodNameAndDesc = name + desc;

      if (!implementedMethods.contains(methodNameAndDesc)) {
         mw = cw.visitMethod(ACC_PUBLIC, name, desc, signature, exceptions);
         generateEmptyImplementation(desc);
         implementedMethods.add(methodNameAndDesc);
      }
   }

   @Override
   public void visitEnd()
   {
      for (String superInterface : initialSuperInterfaces) {
         new MethodGeneratorForImplementedSuperInterface(superInterface);
      }
   }

   private final class MethodGeneratorForImplementedSuperInterface extends ClassVisitor
   {
      String[] superInterfaces;

      MethodGeneratorForImplementedSuperInterface(@NotNull String interfaceName)
      {
         ClassFile.visitClass(interfaceName, this);
      }

      @Override
      public void visit(
         int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superName,
         @Nullable String[] interfaces)
      {
         superInterfaces = interfaces;
      }

      @Override @Nullable
      public FieldVisitor visitField(
         int access, @NotNull String name, @NotNull String desc, @Nullable String signature, @Nullable Object value)
      { return null; }

      @Override @Nullable
      public MethodVisitor visitMethod(
         int access, @NotNull String name, @NotNull String desc,
         @Nullable String signature, @Nullable String[] exceptions)
      {
         generateMethodImplementation(name, desc, signature, exceptions);
         return null;
      }

      @Override
      public void visitEnd()
      {
         for (String superInterface : superInterfaces) {
            new MethodGeneratorForImplementedSuperInterface(superInterface);
         }
      }
   }
}
