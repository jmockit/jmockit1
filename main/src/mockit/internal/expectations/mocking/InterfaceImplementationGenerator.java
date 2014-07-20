/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.reflect.*;
import java.lang.reflect.Type;
import java.util.*;

import org.jetbrains.annotations.*;

import static mockit.external.asm4.Opcodes.*;

import mockit.external.asm4.*;
import mockit.internal.*;

final class InterfaceImplementationGenerator extends MockedTypeModifier
{
   private static final int CLASS_ACCESS = ACC_PUBLIC + ACC_FINAL;

   @NotNull private final List<String> implementedMethods;
   @NotNull private final String implementationClassName;
   @NotNull private final MockedTypeInfo mockedTypeInfo;
   private String interfaceName;
   private String[] initialSuperInterfaces;

   InterfaceImplementationGenerator(
      @NotNull ClassReader classReader, @NotNull Type mockedType, @NotNull String implementationClassName)
   {
      super(classReader);
      implementedMethods = new ArrayList<String>();
      this.implementationClassName = implementationClassName.replace('.', '/');
      mockedTypeInfo = new MockedTypeInfo(mockedType);
   }

   @Override
   public void visit(
      int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superName,
      @Nullable String[] interfaces)
   {
      interfaceName = name;
      initialSuperInterfaces = interfaces;

      String classSignature = signature + mockedTypeInfo.implementationSignature;
      super.visit(version, CLASS_ACCESS, implementationClassName, classSignature, superName, new String[] {name});

      generateDefaultConstructor();
   }

   private void generateDefaultConstructor()
   {
      mw = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
      mw.visitVarInsn(ALOAD, 0);
      mw.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
      generateEmptyImplementation();
   }

   @Override
   public void visitInnerClass(@NotNull String name, String outerName, String innerName, int access) {}

   @Override
   public void visitOuterClass(@NotNull String owner, String name, String desc) {}

   @Override
   public void visitAttribute(Attribute attr) {}

   @Override
   public void visitSource(String source, String debug) {}

   @Override @Nullable
   public FieldVisitor visitField(
      int access, @NotNull String name, @NotNull String desc, @Nullable String signature, @Nullable Object value)
   { return null; }

   @Override @Nullable
   public MethodVisitor visitMethod(
      int access, @NotNull String name, @NotNull String desc, @Nullable String signature, @Nullable String[] exceptions)
   {
      boolean isClassInitializationMethod = name.charAt(0) == '<';
      boolean isJava8StaticMethod = Modifier.isStatic(access);

      if (!isClassInitializationMethod && !isJava8StaticMethod) {
         generateMethodImplementation(access, name, desc, signature, exceptions);
      }

      return null;
   }

   @SuppressWarnings("AssignmentToMethodParameter")
   private void generateMethodImplementation(
      int access, @NotNull String name, @NotNull String desc, @Nullable String signature, @Nullable String[] exceptions)
   {
      String methodNameAndDesc = name + desc;

      if (!implementedMethods.contains(methodNameAndDesc)) {
         if (signature != null) {
            signature = mockedTypeInfo.genericTypeMap.resolveReturnType(signature);
         }

         mw = super.visitMethod(ACC_PUBLIC, name, desc, signature, exceptions);
         generateDirectCallToHandler(interfaceName, access, name, desc, signature);
         generateReturnWithObjectAtTopOfTheStack(desc);
         mw.visitMaxs(1, 0);

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
      @Nullable String[] superInterfaces;

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

      @Nullable @Override
      public FieldVisitor visitField(
         int access, @NotNull String name, @NotNull String desc, String signature, Object value) { return null; }

      @Nullable @Override
      public MethodVisitor visitMethod(
         int access, @NotNull String name, @NotNull String desc, String signature, String[] exceptions)
      {
         if (!"<clinit>".equals(name)) {
            generateMethodImplementation(access, name, desc, signature, exceptions);
         }

         return null;
      }

      @Override
      public void visitEnd()
      {
         assert superInterfaces != null;

         for (String superInterface : superInterfaces) {
            new MethodGeneratorForImplementedSuperInterface(superInterface);
         }
      }
   }
}
