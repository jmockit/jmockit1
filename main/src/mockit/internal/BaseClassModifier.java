/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal;

import javax.annotation.*;
import static java.lang.reflect.Modifier.*;

import mockit.external.asm.*;
import mockit.internal.state.*;
import mockit.internal.util.*;
import static mockit.external.asm.Opcodes.*;

public class BaseClassModifier extends ClassVisitor
{
   private static final int METHOD_ACCESS_MASK = 0xFFFF - ACC_ABSTRACT - ACC_NATIVE;
   protected static final Type VOID_TYPE = Type.getType("Ljava/lang/Void;");

   @Nonnull
   protected final MethodVisitor methodAnnotationsVisitor = new MethodVisitor() {
      @Override
      public AnnotationVisitor visitAnnotation(String desc, boolean visible)
      {
         return mw.visitAnnotation(desc, visible);
      }

      @Override
      public void visitLocalVariable(
         @Nonnull String name, @Nonnull String desc, String signature, @Nonnull Label start, @Nonnull Label end,
         int index)
      {}

      @Override
      public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) { return null; }
   };

   @Nonnull protected final ClassWriter cw;
   protected MethodWriter mw;
   protected boolean useMockingBridge;
   protected String superClassName;
   protected String classDesc;
   protected int methodAccess;
   protected String methodName;
   protected String methodDesc;

   protected BaseClassModifier(@Nonnull ClassReader classReader)
   {
      super(new ClassWriter(classReader));
      //noinspection ConstantConditions
      cw = (ClassWriter) cv;
   }

   protected final void setUseMockingBridge(@Nullable ClassLoader classLoader)
   {
      useMockingBridge = ClassLoad.isClassLoaderWithNoDirectAccess(classLoader);
   }

   @Override
   public void visit(
      int version, int access, @Nonnull String name, @Nullable String signature, @Nullable String superName,
      @Nullable String[] interfaces)
   {
      int modifiedVersion = version;
      int originalVersion = version & 0xFFFF;

      if (originalVersion < V1_5) {
         // LDC instructions (see MethodVisitor#visitLdcInsn) are more capable in JVMs with support for class files of
         // version 49 (Java 5) or newer, so we "upgrade" it to avoid a VerifyError:
         modifiedVersion = V1_5;
      }

      cw.visit(modifiedVersion, access, name, signature, superName, interfaces);
      superClassName = superName;
      classDesc = name;
   }

   /**
    * Just creates a new MethodWriter which will write out the method bytecode when visited.
    * <p/>
    * Removes any "abstract" or "native" modifiers for the modified version.
    */
   protected final void startModifiedMethodVersion(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions)
   {
      mw = cw.visitMethod(access & METHOD_ACCESS_MASK, name, desc, signature, exceptions);

      methodAccess = access;
      methodName = name;
      methodDesc = desc;

      if (isNative(access)) {
         TestRun.mockFixture().addRedefinedClassWithNativeMethods(classDesc);
      }
   }

   public final boolean wasModified() { return methodName != null; }

   protected final void generateCallToSuperConstructor()
   {
      if (superClassName != null) {
         mw.visitVarInsn(ALOAD, 0);

         String constructorDesc;

         if ("java/lang/Object".equals(superClassName)) {
            constructorDesc = "()V";
         }
         else {
            constructorDesc = SuperConstructorCollector.INSTANCE.findConstructor(classDesc, superClassName);

            for (Type paramType : Type.getArgumentTypes(constructorDesc)) {
               pushDefaultValueForType(paramType);
            }
         }

         mw.visitMethodInsn(INVOKESPECIAL, superClassName, "<init>", constructorDesc, false);
      }
   }

   protected final void generateReturnWithObjectAtTopOfTheStack(@Nonnull String mockedMethodDesc)
   {
      Type returnType = Type.getReturnType(mockedMethodDesc);
      TypeConversion.generateCastFromObject(mw, returnType);
      mw.visitInsn(returnType.getOpcode(IRETURN));
   }

   protected final boolean generateCodeToPassThisOrNullIfStaticMethod()
   {
      return generateCodeToPassThisOrNullIfStaticMethod(mw, methodAccess);
   }

   public static boolean generateCodeToPassThisOrNullIfStaticMethod(@Nonnull MethodWriter mw, int access)
   {
      boolean isStatic = isStatic(access);

      if (isStatic) {
         mw.visitInsn(ACONST_NULL);
      }
      else {
         mw.visitVarInsn(ALOAD, 0);
      }

      return isStatic;
   }

   public static void generateCodeToCreateArrayOfObject(@Nonnull MethodWriter mw, int arrayLength)
   {
      mw.visitIntInsn(SIPUSH, arrayLength);
      mw.visitTypeInsn(ANEWARRAY, "java/lang/Object");
   }

   public static void generateCodeToFillArrayWithParameterValues(
      @Nonnull MethodWriter mw, @Nonnull Type[] parameterTypes, int initialArrayIndex, int initialParameterIndex)
   {
      int i = initialArrayIndex;
      int j = initialParameterIndex;

      for (Type parameterType : parameterTypes) {
         mw.visitInsn(DUP);
         mw.visitIntInsn(SIPUSH, i++);
         mw.visitVarInsn(parameterType.getOpcode(ILOAD), j);
         TypeConversion.generateCastToObject(mw, parameterType);
         mw.visitInsn(AASTORE);
         j += parameterType.getSize();
      }
   }

   protected final void generateCodeToObtainInstanceOfMockingBridge(@Nonnull MockingBridge mockingBridge)
   {
      String hostClassName = MockingBridge.getHostClassName();
      mw.visitFieldInsn(GETSTATIC, hostClassName, mockingBridge.id, "Ljava/lang/reflect/InvocationHandler;");
   }

   protected final void generateCodeToFillArrayElement(int arrayIndex, @Nullable Object value)
   {
      mw.visitInsn(DUP);
      mw.visitIntInsn(SIPUSH, arrayIndex);

      if (value == null) {
         mw.visitInsn(ACONST_NULL);
      }
      else if (value instanceof Integer) {
         mw.visitIntInsn(SIPUSH, (Integer) value);
         mw.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
      }
      else if (value instanceof Boolean) {
         mw.visitInsn((Boolean) value ? ICONST_1 : ICONST_0);
         mw.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
      }
      else {
         mw.visitLdcInsn(value);
      }

      mw.visitInsn(AASTORE);
   }

   private void pushDefaultValueForType(@Nonnull Type type)
   {
      switch (type.getSort()) {
         case Type.VOID: break;
         case Type.BOOLEAN:
         case Type.CHAR:
         case Type.BYTE:
         case Type.SHORT:
         case Type.INT:    mw.visitInsn(ICONST_0); break;
         case Type.LONG:   mw.visitInsn(LCONST_0); break;
         case Type.FLOAT:  mw.visitInsn(FCONST_0); break;
         case Type.DOUBLE: mw.visitInsn(DCONST_0); break;
         case Type.ARRAY:  generateCreationOfEmptyArray(type); break;
         default:          mw.visitInsn(ACONST_NULL);
      }
   }

   private void generateCreationOfEmptyArray(@Nonnull Type arrayType)
   {
      int dimensions = arrayType.getDimensions();

      for (int dimension = 0; dimension < dimensions; dimension++) {
         mw.visitInsn(ICONST_0);
      }

      if (dimensions > 1) {
         mw.visitMultiANewArrayInsn(arrayType.getDescriptor(), dimensions);
         return;
      }

      Type elementType = arrayType.getElementType();
      int elementSort = elementType.getSort();

      if (elementSort == Type.OBJECT) {
         mw.visitTypeInsn(ANEWARRAY, elementType.getInternalName());
      }
      else {
         int typ = getArrayElementTypeCode(elementSort);
         mw.visitIntInsn(NEWARRAY, typ);
      }
   }

   private static int getArrayElementTypeCode(int elementSort)
   {
      switch (elementSort) {
          case Type.BOOLEAN: return T_BOOLEAN;
          case Type.CHAR:    return T_CHAR;
          case Type.BYTE:    return T_BYTE;
          case Type.SHORT:   return T_SHORT;
          case Type.INT:     return T_INT;
          case Type.FLOAT:   return T_FLOAT;
          case Type.LONG:    return T_LONG;
          default:           return T_DOUBLE;
      }
   }

   protected final void generateCallToInvocationHandler()
   {
      mw.visitMethodInsn(
         INVOKEINTERFACE, "java/lang/reflect/InvocationHandler", "invoke",
         "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;", true);
   }

   protected final void generateEmptyImplementation(@Nonnull String desc)
   {
      Type returnType = Type.getReturnType(desc);
      pushDefaultValueForType(returnType);
      mw.visitInsn(returnType.getOpcode(IRETURN));
      mw.visitMaxs(1, 0);
   }

   protected final void generateEmptyImplementation()
   {
      mw.visitInsn(RETURN);
      mw.visitMaxs(1, 0);
   }

   @Nonnull
   protected final MethodVisitor copyOriginalImplementationCode(boolean disregardCallToSuper)
   {
      if (disregardCallToSuper) {
         return new DynamicConstructorModifier();
      }

      if (isNative(methodAccess)) {
         generateEmptyImplementation(methodDesc);
         return methodAnnotationsVisitor;
      }

      return new DynamicModifier();
   }

   private class DynamicModifier extends MethodVisitor
   {
      DynamicModifier() { super(mw); }

      @Override
      public final void visitLocalVariable(
         @Nonnull String name, @Nonnull String desc, @Nullable String signature,
         @Nonnull Label start, @Nonnull Label end, int index)
      {
         // For some reason, the start position for "this" gets displaced by bytecode inserted at the beginning,
         // in a method modified by the EMMA tool. If not treated, this causes a ClassFormatError.
         if (end.position > 0 && start.position > end.position) {
            start.position = end.position;
         }

         // Ignores any local variable with required information missing, to avoid a VerifyError/ClassFormatError.
         if (start.position > 0 && end.position > 0) {
            mw.visitLocalVariable(name, desc, signature, start, end, index);
         }
      }
   }

   private final class DynamicConstructorModifier extends DynamicModifier
   {
      private boolean pendingCallToConstructorOfSameClass;
      private boolean callToAnotherConstructorAlreadyDisregarded;

      @Override
      public void visitTypeInsn(int opcode, @Nonnull String type)
      {
         if (!callToAnotherConstructorAlreadyDisregarded && opcode == NEW && type.equals(classDesc)) {
            pendingCallToConstructorOfSameClass = true;
         }

         mw.visitTypeInsn(opcode, type);
      }

      @Override
      public void visitMethodInsn(
         int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc, boolean itf)
      {
         if (pendingCallToConstructorOfSameClass) {
            if (opcode == INVOKESPECIAL && "<init>".equals(name) && owner.equals(classDesc)) {
               mw.visitMethodInsn(INVOKESPECIAL, owner, name, desc, itf);
               pendingCallToConstructorOfSameClass = false;
            }
         }
         else if (
            callToAnotherConstructorAlreadyDisregarded ||
            opcode != INVOKESPECIAL || !"<init>".equals(name) ||
            !owner.equals(superClassName) && !owner.equals(classDesc)
         ) {
            mw.visitMethodInsn(opcode, owner, name, desc, itf);
         }
         else {
            callToAnotherConstructorAlreadyDisregarded = true;
         }
      }

      @Override
      public void visitTryCatchBlock(Label start, Label end, Label handler, String type)
      {
         if (callToAnotherConstructorAlreadyDisregarded) {
            mw.visitTryCatchBlock(start, end, handler, type);
         }
      }
   }
}
