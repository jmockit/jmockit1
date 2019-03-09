/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.classGeneration;

import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;
import static java.util.Arrays.*;

import mockit.asm.classes.*;
import mockit.asm.fields.*;
import mockit.asm.metadata.*;
import mockit.asm.metadata.ClassMetadataReader.*;
import mockit.asm.jvmConstants.*;
import mockit.asm.methods.*;
import mockit.asm.types.*;
import mockit.internal.*;
import mockit.internal.util.*;
import static mockit.asm.jvmConstants.Opcodes.*;

public class BaseSubclassGenerator extends BaseClassModifier
{
   private static final int CLASS_ACCESS_MASK = 0xFFFF - Access.ABSTRACT;
   private static final int CONSTRUCTOR_ACCESS_MASK = Access.PUBLIC + Access.PROTECTED;

   // Fixed initial state:
   @Nonnull Class<?> baseClass;
   @Nonnull private final String subclassName;
   @Nullable protected final MockedTypeInfo mockedTypeInfo;
   private final boolean copyConstructors;

   // Helper fields for mutable state:
   @Nonnull private final List<String> implementedMethods;
   @Nullable private String superClassOfSuperClass;
   private Set<String> superInterfaces;

   protected BaseSubclassGenerator(
      @Nonnull Class<?> baseClass, @Nonnull ClassReader cr, @Nullable Type genericMockedType, @Nonnull String subclassName,
      boolean copyConstructors
   ) {
      super(cr);
      this.baseClass = baseClass;
      this.subclassName = subclassName.replace('.', '/');
      mockedTypeInfo = genericMockedType == null ? null : new MockedTypeInfo(genericMockedType);
      this.copyConstructors = copyConstructors;
      implementedMethods = new ArrayList<>();
   }

   @Override
   public void visit(int version, int access, @Nonnull String name, @Nonnull ClassInfo additionalInfo) {
      ClassInfo subClassInfo = new ClassInfo();
      subClassInfo.superName = name;
      subClassInfo.signature = mockedTypeInfo == null ? additionalInfo.signature : mockedTypeInfo.implementationSignature;
      int subclassAccess = access & CLASS_ACCESS_MASK | Access.FINAL;

      super.visit(version, subclassAccess, subclassName, subClassInfo);

      superClassOfSuperClass = additionalInfo.superName;
      superInterfaces = new HashSet<>();

      String[] interfaces = additionalInfo.interfaces;

      if (interfaces.length > 0) {
         superInterfaces.addAll(asList(interfaces));
      }
   }

   @Override
   public final void visitInnerClass(@Nonnull String name, @Nullable String outerName, @Nullable String innerName, int access) {}

   @Override @Nullable
   public final FieldVisitor visitField(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable Object value
   ) { return null; }

   @Override @Nullable
   public MethodVisitor visitMethod(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions
   ) {
      if (copyConstructors && "<init>".equals(name)) {
         if ((access & CONSTRUCTOR_ACCESS_MASK) != 0) {
            generateConstructorDelegatingToSuper(desc, signature, exceptions);
         }
      }
      else {
         // Inherits from super-class when non-abstract; otherwise, creates implementation for abstract method.
         generateImplementationIfAbstractMethod(superClassName, access, name, desc, signature, exceptions);
      }

      return null;
   }

   private void generateConstructorDelegatingToSuper(@Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions) {
      mw = cw.visitMethod(Access.PUBLIC, "<init>", desc, signature, exceptions);
      mw.visitVarInsn(ALOAD, 0);
      int varIndex = 1;

      for (JavaType paramType : JavaType.getArgumentTypes(desc)) {
         int loadOpcode = paramType.getLoadOpcode();
         mw.visitVarInsn(loadOpcode, varIndex);
         varIndex++;
      }

      mw.visitMethodInsn(INVOKESPECIAL, superClassName, "<init>", desc, false);
      generateEmptyImplementation();
   }

   private void generateImplementationIfAbstractMethod(
      String className, int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions
   ) {
      if (!"<init>".equals(name)) {
         String methodNameAndDesc = name + desc;

         if (!implementedMethods.contains(methodNameAndDesc)) {
            if ((access & Access.ABSTRACT) != 0) {
               generateMethodImplementation(className, access, name, desc, signature, exceptions);
            }

            implementedMethods.add(methodNameAndDesc);
         }
      }
   }

   protected void generateMethodImplementation(
      String className, int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions
   ) {}

   @Override
   public void visitEnd() {
      generateImplementationsForInheritedAbstractMethods(superClassOfSuperClass);

      while (!superInterfaces.isEmpty()) {
         String superInterface = superInterfaces.iterator().next();
         generateImplementationsForAbstractMethods(superInterface, false);
         superInterfaces.remove(superInterface);
      }
   }

   private void generateImplementationsForInheritedAbstractMethods(@Nullable String superName) {
      if (superName != null) {
         generateImplementationsForAbstractMethods(superName, true);
      }
   }

   private void generateImplementationsForAbstractMethods(@Nonnull String typeName, boolean abstractClass) {
      if (!"java/lang/Object".equals(typeName)) {
         byte[] typeBytecode = ClassFile.getClassFile(typeName);
         ClassMetadataReader cmr = new ClassMetadataReader(typeBytecode);
         String[] interfaces = cmr.getInterfaces();

         if (interfaces != null) {
            superInterfaces.addAll(asList(interfaces));
         }

         for (MethodInfo method : cmr.getMethods()) {
            if (abstractClass) {
               generateImplementationIfAbstractMethod(typeName, method.accessFlags, method.name, method.desc, null, null);
            }
            else if (method.isAbstract()) {
               generateImplementationForInterfaceMethodIfMissing(typeName, method);
            }
         }

         if (abstractClass) {
            String superClass = cmr.getSuperClass();
            generateImplementationsForInheritedAbstractMethods(superClass);
         }
      }
   }

   private void generateImplementationForInterfaceMethodIfMissing(@Nonnull String typeName, @Nonnull MethodInfo method) {
      String name = method.name;
      String desc = method.desc;
      String methodNameAndDesc = name + desc;

      if (!implementedMethods.contains(methodNameAndDesc)) {
         if (!hasMethodImplementation(name, desc)) {
            generateMethodImplementation(typeName, method.accessFlags, name, desc, null, null);
         }

         implementedMethods.add(methodNameAndDesc);
      }
   }

   private boolean hasMethodImplementation(@Nonnull String name, @Nonnull String desc) {
      Class<?>[] paramTypes = TypeDescriptor.getParameterTypes(desc);

      try {
         Method method = baseClass.getMethod(name, paramTypes);
         return !method.getDeclaringClass().isInterface();
      }
      catch (NoSuchMethodException ignore) {
         return false;
      }
   }
}
