/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal;

import static java.lang.reflect.Modifier.*;

import mockit.external.asm.*;
import mockit.internal.state.*;
import mockit.internal.util.*;
import static mockit.external.asm.Opcodes.*;

import org.jetbrains.annotations.*;

@SuppressWarnings("ClassWithTooManyFields")
public class BaseClassModifier extends ClassVisitor
{
   private static final int METHOD_ACCESS_MASK = 0xFFFF - ACC_ABSTRACT - ACC_NATIVE;
   protected static final Type VOID_TYPE = Type.getType("Ljava/lang/Void;");

   @NotNull
   protected final MethodVisitor methodAnnotationsVisitor = new MethodVisitor()
   {
      @Override
      public AnnotationVisitor visitAnnotation(String desc, boolean visible)
      {
         return mw.visitAnnotation(desc, visible);
      }

      @Override
      public void visitLocalVariable(
         @NotNull String name, @NotNull String desc, String signature, @NotNull Label start, @NotNull Label end,
         int index)
      {
         registerParameterName(name, index);
      }

      @Override
      public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible)
      {
         return mw.visitParameterAnnotation(parameter, desc, visible);
      }
   };

   protected final void registerParameterName(@NotNull String name, int index)
   {
      ParameterNames.registerName(classDesc, methodAccess, methodName, methodDesc, name, index);
   }

   @NotNull protected final ClassWriter cw;
   protected MethodWriter mw;
   protected boolean useMockingBridge;
   protected String superClassName;
   protected String classDesc;
   protected int methodAccess;
   protected String methodName;
   protected String methodDesc;
   private boolean callToAnotherConstructorAlreadyDisregarded;

   protected BaseClassModifier(@NotNull ClassReader classReader)
   {
      super(new ClassWriter(classReader));
      //noinspection ConstantConditions
      cw = (ClassWriter) cv;
   }

   protected final void setUseMockingBridge(@Nullable ClassLoader classLoader)
   {
      useMockingBridge = classLoader == null;
   }

   @Override
   public void visit(
      int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superName,
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
      int access, @NotNull String name, @NotNull String desc, @Nullable String signature, @Nullable String[] exceptions)
   {
      mw = cw.visitMethod(access & METHOD_ACCESS_MASK, name, desc, signature, exceptions);

      methodAccess = access;
      methodName = name;
      methodDesc = desc;
      callToAnotherConstructorAlreadyDisregarded = false;

      if (isNative(access)) {
         TestRun.mockFixture().addRedefinedClassWithNativeMethods(classDesc);
      }
   }

   protected final void generateCallToSuperConstructor()
   {
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

   protected final void generateReturnWithObjectAtTopOfTheStack(@NotNull String mockedMethodDesc)
   {
      Type returnType = Type.getReturnType(mockedMethodDesc);
      TypeConversion.generateCastFromObject(mw, returnType);
      mw.visitInsn(returnType.getOpcode(IRETURN));
   }

   public static boolean generateCodeToPassThisOrNullIfStaticMethod(@NotNull MethodWriter mw, int access)
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

   public static void generateCodeToCreateArrayOfObject(@NotNull MethodWriter mw, int arrayLength)
   {
      mw.visitIntInsn(BIPUSH, arrayLength);
      mw.visitTypeInsn(ANEWARRAY, "java/lang/Object");
   }

   public static void generateCodeToFillArrayWithParameterValues(
      @NotNull MethodWriter mw, @NotNull Type[] parameterTypes, int initialArrayIndex, int initialParameterIndex)
   {
      int i = initialArrayIndex;
      int j = initialParameterIndex;

      for (Type parameterType : parameterTypes) {
         mw.visitInsn(DUP);
         mw.visitIntInsn(BIPUSH, i++);
         mw.visitVarInsn(parameterType.getOpcode(ILOAD), j);
         TypeConversion.generateCastToObject(mw, parameterType);
         mw.visitInsn(AASTORE);
         j += parameterType.getSize();
      }
   }

   protected final void generateCodeToObtainInstanceOfMockingBridge(@NotNull MockingBridge mockingBridge)
   {
      mw.visitFieldInsn(GETSTATIC, "org/omg/IOP/IORHelper", mockingBridge.id, "Ljava/lang/reflect/InvocationHandler;");
   }

   protected final void generateCodeToFillArrayElement(int arrayIndex, @Nullable Object value)
   {
      mw.visitInsn(DUP);
      mw.visitIntInsn(BIPUSH, arrayIndex);

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

   private void pushDefaultValueForType(@NotNull Type type)
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

   private void generateCreationOfEmptyArray(@NotNull Type arrayType)
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

   protected final void generateEmptyImplementation(@NotNull String desc)
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

   protected final void disregardIfInvokingAnotherConstructor(
      int opcode, @NotNull String owner, @NotNull String name, @NotNull String desc, boolean itf)
   {
      if (
         callToAnotherConstructorAlreadyDisregarded ||
         opcode != INVOKESPECIAL || !"<init>".equals(name) ||
         !owner.equals(superClassName) && !owner.equals(classDesc)
      ) {
         mw.visitMethodInsn(opcode, owner, name, desc, itf);
      }
      else {
         mw.visitInsn(POP);
         callToAnotherConstructorAlreadyDisregarded = true;
      }
   }

   public final boolean wasModified() { return methodName != null; }
}
