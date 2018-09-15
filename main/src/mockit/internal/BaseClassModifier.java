/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal;

import javax.annotation.*;
import static java.lang.reflect.Modifier.*;

import mockit.asm.*;
import mockit.internal.expectations.*;
import mockit.internal.util.*;
import static mockit.asm.Opcodes.*;

public class BaseClassModifier extends WrappingClassVisitor
{
   private static final int METHOD_ACCESS_MASK = 0xFFFF - Access.ABSTRACT - Access.NATIVE;
   protected static final JavaType VOID_TYPE = ObjectType.create("java/lang/Void");

   @Nonnull
   protected final MethodVisitor methodAnnotationsVisitor = new MethodVisitor() {
      @Override
      public AnnotationVisitor visitAnnotation(@Nonnull String desc) { return mw.visitAnnotation(desc); }
   };

   protected MethodWriter mw;
   protected boolean useClassLoadingBridge;
   protected String superClassName;
   protected String classDesc;
   protected int methodAccess;
   protected String methodName;
   protected String methodDesc;

   protected BaseClassModifier(@Nonnull byte[] code) {
      super(new ClassWriter(code));
   }

   protected BaseClassModifier(@Nonnull ClassReader classReader) {
      super(new ClassWriter(classReader));
   }

   protected final void setUseClassLoadingBridge(@Nullable ClassLoader classLoader) {
      useClassLoadingBridge = ClassLoad.isClassLoaderWithNoDirectAccess(classLoader);
   }

   @Override
   public void visit(
      int version, int access, @Nonnull String name, @Nullable String signature, @Nullable String superName, @Nullable String[] interfaces
   ) {
      int modifiedVersion = version;
      int originalVersion = version & 0xFFFF;

      if (originalVersion < ClassVersion.V1_5) {
         // LDC instructions (see MethodVisitor#visitLdcInsn) are more capable in JVMs with support for class files of
         // version 49 (Java 5) or newer, so we "upgrade" it to avoid a VerifyError:
         modifiedVersion = ClassVersion.V1_5;
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
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions
   ) {
      mw = cw.visitMethod(access & METHOD_ACCESS_MASK, name, desc, signature, exceptions);

      methodAccess = access;
      methodName = name;
      methodDesc = desc;
   }

   public final boolean wasModified() { return methodName != null; }

   protected final void generateCallToSuperConstructor() {
      if (superClassName != null) {
         mw.visitVarInsn(ALOAD, 0);

         String constructorDesc;

         if ("java/lang/Object".equals(superClassName)) {
            constructorDesc = "()V";
         }
         else {
            constructorDesc = SuperConstructorCollector.INSTANCE.findConstructor(classDesc, superClassName);

            for (JavaType paramType : JavaType.getArgumentTypes(constructorDesc)) {
               pushDefaultValueForType(paramType);
            }
         }

         mw.visitMethodInsn(INVOKESPECIAL, superClassName, "<init>", constructorDesc, false);
      }
   }

   protected final void generateDirectCallToHandler(
      @Nonnull String className, int access, @Nonnull String name, @Nonnull String desc, @Nullable String genericSignature
   ) {
      generateDirectCallToHandler(className, access, name, desc, genericSignature, ExecutionMode.Regular);
   }

   protected final void generateDirectCallToHandler(
      @Nonnull String className, int access, @Nonnull String name, @Nonnull String desc, @Nullable String genericSignature,
      @Nonnull ExecutionMode executionMode
   ) {
      // First argument: the mock instance, if any.
      boolean isStatic = generateCodeToPassThisOrNullIfStaticMethod(access);

      // Second argument: method access flags.
      mw.visitLdcInsn(access);

      // Third argument: class name.
      mw.visitLdcInsn(className);

      // Fourth argument: method signature.
      mw.visitLdcInsn(name + desc);

      // Fifth argument: generic signature, or null if none.
      generateInstructionToLoadNullableString(genericSignature);

      // Sixth argument: indicate regular or special modes of execution.
      mw.visitLdcInsn(executionMode.ordinal());

      // Seventh argument: array with invocation arguments.
      JavaType[] argTypes = JavaType.getArgumentTypes(desc);
      int argCount = argTypes.length;

      if (argCount == 0) {
         mw.visitInsn(ACONST_NULL);
      }
      else {
         generateCodeToCreateArrayOfObject(argCount);
         generateCodeToFillArrayWithParameterValues(argTypes, 0, isStatic ? 0 : 1);
      }

      mw.visitMethodInsn(
         INVOKESTATIC, "mockit/internal/expectations/RecordAndReplayExecution", "recordOrReplay",
         "(Ljava/lang/Object;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;I[Ljava/lang/Object;)Ljava/lang/Object;", false);
   }

   private void generateInstructionToLoadNullableString(@Nullable String text) {
      if (text == null) {
         mw.visitInsn(ACONST_NULL);
      }
      else {
         mw.visitLdcInsn(text);
      }
   }

   protected final void generateReturnWithObjectAtTopOfTheStack(@Nonnull String mockedMethodDesc) {
      JavaType returnType = JavaType.getReturnType(mockedMethodDesc);
      TypeConversion.generateCastFromObject(mw, returnType);
      mw.visitInsn(returnType.getOpcode(IRETURN));
   }

   protected final boolean generateCodeToPassThisOrNullIfStaticMethod() {
      return generateCodeToPassThisOrNullIfStaticMethod(methodAccess);
   }

   private boolean generateCodeToPassThisOrNullIfStaticMethod(int access) {
      boolean isStatic = isStatic(access);

      if (isStatic) {
         mw.visitInsn(ACONST_NULL);
      }
      else {
         mw.visitVarInsn(ALOAD, 0);
      }

      return isStatic;
   }

   protected final void generateCodeToCreateArrayOfObject(@Nonnegative int arrayLength) {
      mw.visitIntInsn(SIPUSH, arrayLength);
      mw.visitTypeInsn(ANEWARRAY, "java/lang/Object");
   }

   protected final void generateCodeToFillArrayWithParameterValues(
      @Nonnull JavaType[] parameterTypes, @Nonnegative int initialArrayIndex, @Nonnegative int initialParameterIndex
   ) {
      int i = initialArrayIndex;
      int j = initialParameterIndex;

      for (JavaType parameterType : parameterTypes) {
         mw.visitInsn(DUP);
         mw.visitIntInsn(SIPUSH, i++);
         mw.visitVarInsn(parameterType.getOpcode(ILOAD), j);
         TypeConversion.generateCastToObject(mw, parameterType);
         mw.visitInsn(AASTORE);
         j += parameterType.getSize();
      }
   }

   protected final void generateCodeToObtainInstanceOfClassLoadingBridge(@Nonnull ClassLoadingBridge classLoadingBridge) {
      String hostClassName = ClassLoadingBridge.getHostClassName();
      mw.visitFieldInsn(GETSTATIC, hostClassName, classLoadingBridge.id, "Ljava/lang/reflect/InvocationHandler;");
   }

   protected final void generateCodeToFillArrayElement(int arrayIndex, @Nullable Object value) {
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

   private void pushDefaultValueForType(@Nonnull JavaType type) {
      if (type instanceof ArrayType) {
         generateCreationOfEmptyArray((ArrayType) type);
      }
      else {
         int constOpcode = type.getConstOpcode();

         if (constOpcode > 0) {
            mw.visitInsn(constOpcode);
         }
      }
   }

   private void generateCreationOfEmptyArray(@Nonnull ArrayType arrayType) {
      int dimensions = arrayType.getDimensions();

      for (int dimension = 0; dimension < dimensions; dimension++) {
         mw.visitInsn(ICONST_0);
      }

      if (dimensions > 1) {
         mw.visitMultiANewArrayInsn(arrayType.getDescriptor(), dimensions);
         return;
      }

      JavaType elementType = arrayType.getElementType();

      if (elementType instanceof ReferenceType) {
         mw.visitTypeInsn(ANEWARRAY, ((ReferenceType) elementType).getInternalName());
      }
      else {
         int typeCode = PrimitiveType.getArrayElementType((PrimitiveType) elementType);
         mw.visitIntInsn(NEWARRAY, typeCode);
      }
   }

   protected final void generateCallToInvocationHandler() {
      mw.visitMethodInsn(
         INVOKEINTERFACE, "java/lang/reflect/InvocationHandler", "invoke",
         "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;", true);
   }

   protected final void generateEmptyImplementation(@Nonnull String desc) {
      JavaType returnType = JavaType.getReturnType(desc);
      pushDefaultValueForType(returnType);
      mw.visitInsn(returnType.getOpcode(IRETURN));
      mw.visitMaxStack(1);
   }

   protected final void generateEmptyImplementation() {
      mw.visitInsn(RETURN);
      mw.visitMaxStack(1);
   }

   @Nonnull
   protected final MethodVisitor copyOriginalImplementationCode(boolean disregardCallToSuper) {
      if (disregardCallToSuper) {
         return new DynamicConstructorModifier();
      }

      if (isNative(methodAccess)) {
         generateEmptyImplementation(methodDesc);
         return methodAnnotationsVisitor;
      }

      return new DynamicModifier();
   }

   private class DynamicModifier extends WrappingMethodVisitor {
      DynamicModifier() { super(BaseClassModifier.this.mw); }

      @Override
      public final void visitLocalVariable(
         @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nonnull Label start, @Nonnull Label end,
         @Nonnegative int index
      ) {
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

   private final class DynamicConstructorModifier extends DynamicModifier {
      private boolean pendingCallToConstructorOfSameClass;
      private boolean callToAnotherConstructorAlreadyDisregarded;

      @Override
      public void visitTypeInsn(int opcode, @Nonnull String typeDesc) {
         if (!callToAnotherConstructorAlreadyDisregarded && opcode == NEW && typeDesc.equals(classDesc)) {
            pendingCallToConstructorOfSameClass = true;
         }

         mw.visitTypeInsn(opcode, typeDesc);
      }

      @Override
      public void visitMethodInsn(int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc, boolean itf) {
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
      public void visitTryCatchBlock(@Nonnull Label start, @Nonnull Label end, @Nonnull Label handler, @Nullable String type) {
         if (callToAnotherConstructorAlreadyDisregarded) {
            mw.visitTryCatchBlock(start, end, handler, type);
         }
      }
   }
}
