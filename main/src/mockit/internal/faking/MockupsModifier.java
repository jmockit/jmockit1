/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.faking;

import javax.annotation.*;
import static java.lang.reflect.Modifier.*;

import mockit.*;
import mockit.external.asm.*;
import mockit.internal.*;
import mockit.internal.faking.FakeMethods.*;
import mockit.internal.state.*;
import mockit.internal.util.*;
import static mockit.external.asm.Opcodes.*;

/**
 * Responsible for generating all necessary bytecode in the redefined (real) class.
 * Such code will redirect calls made on "real" methods to equivalent calls on the corresponding "fake" methods.
 * The original code won't be executed by the running JVM until the class redefinition is undone.
 * <p/>
 * Methods in the real class with no corresponding fake methods are unaffected.
 * <p/>
 * Any fields (static or not) in the real class remain untouched.
 */
final class MockupsModifier extends BaseClassModifier
{
   private static final int ABSTRACT_OR_SYNTHETIC = ACC_ABSTRACT + ACC_SYNTHETIC;

   @Nonnull private final FakeMethods fakeMethods;
   private final boolean useMockingBridgeForUpdatingFakeState;
   @Nonnull private final Class<?> fakedClass;
   private FakeMethod fakeMethod;
   private boolean isConstructor;

   /**
    * Initializes the modifier for a given real/fake class pair.
    * <p/>
    * The fake instance provided will receive calls for any instance methods defined in the fake class.
    * Therefore, it needs to be later recovered by the modified bytecode inside the real method.
    * To enable this, the fake instance is added to a global data structure made available through the
    * {@link TestRun#getFake(String, Object)} method.
    *
    * @param cr the class file reader for the real class
    * @param realClass the class to be faked, or a base type of an implementation class to be faked
    * @param fake an instance of the fake class
    * @param fakeMethods contains the set of fake methods collected from the fake class; each fake method is identified
    * by a pair composed of "name" and "desc", where "name" is the method name, and "desc" is the JVM internal
    * description of the parameters; once the real class modification is complete this set will be empty, unless no
    * corresponding real method was found for any of its method identifiers
    */
   MockupsModifier(
      @Nonnull ClassReader cr, @Nonnull Class<?> realClass, @Nonnull MockUp<?> fake, @Nonnull FakeMethods fakeMethods)
   {
      super(cr);
      fakedClass = realClass;
      this.fakeMethods = fakeMethods;

      ClassLoader classLoaderOfRealClass = realClass.getClassLoader();
      useMockingBridgeForUpdatingFakeState = ClassLoad.isClassLoaderWithNoDirectAccess(classLoaderOfRealClass);
      inferUseOfMockingBridge(classLoaderOfRealClass, fake);
   }

   private void inferUseOfMockingBridge(@Nullable ClassLoader classLoaderOfRealClass, @Nonnull Object fake)
   {
      setUseMockingBridge(classLoaderOfRealClass);

      if (!useMockingBridge && !isPublic(fake.getClass().getModifiers())) {
         useMockingBridge = true;
      }
   }

   /**
    * If the specified method has a fake definition, then generates bytecode to redirect calls made to it to the fake
    * method. If it has no fake, does nothing.
    *
    * @param access not relevant
    * @param name together with desc, used to identity the method in given set of fake methods
    * @param signature not relevant
    * @param exceptions not relevant
    *
    * @return {@code null} if the method was redefined, otherwise a {@code MethodWriter} that writes out the visited
    * method code without changes
    */
   @Override
   public MethodVisitor visitMethod(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions)
   {
      if ((access & ABSTRACT_OR_SYNTHETIC) != 0) {
         if (isAbstract(access)) {
            // Marks a matching fake method (if any) as having the corresponding faked method.
            fakeMethods.findMethod(access, name, desc, signature);
         }

         return cw.visitMethod(access, name, desc, signature, exceptions);
      }

      isConstructor = "<init>".equals(name);

      if (isConstructor && isFakedSuperclass() || !hasFake(access, name, desc, signature)) {
         return cw.visitMethod(access, name, desc, signature, exceptions);
      }

      startModifiedMethodVersion(access, name, desc, signature, exceptions);

      if (isConstructor) {
         generateCallToSuperConstructor();
      }
      else if (isNative(methodAccess)) {
         generateCallToUpdateFakeState();
         generateCallToFakeMethod();
         generateMethodReturn();
         mw.visitMaxs(1, 0); // dummy values, real ones are calculated by ASM
         return methodAnnotationsVisitor;
      }

      generateDynamicCallToFake();
      return copyOriginalImplementationCode(isConstructor);
   }

   private boolean hasFake(int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature)
   {
      String fakeName = getCorrespondingFakeName(name);
      fakeMethod = fakeMethods.findMethod(access, fakeName, desc, signature);
      return fakeMethod != null;
   }

   @Nonnull
   private static String getCorrespondingFakeName(@Nonnull String name)
   {
      if ("<init>".equals(name)) {
         return "$init";
      }

      if ("<clinit>".equals(name)) {
         return "$clinit";
      }

      return name;
   }

   private boolean isFakedSuperclass() { return fakedClass != fakeMethods.getRealClass(); }

   private void generateDynamicCallToFake()
   {
      Label startOfRealImplementation = null;

      if (!isStatic(methodAccess) && !isConstructor && isFakedSuperclass()) {
         Class<?> targetClass = fakeMethods.getRealClass();

         if (fakedClass.getClassLoader() == targetClass.getClassLoader()) {
            startOfRealImplementation = new Label();
            mw.visitVarInsn(ALOAD, 0);
            mw.visitTypeInsn(INSTANCEOF, Type.getInternalName(targetClass));
            mw.visitJumpInsn(IFEQ, startOfRealImplementation);
         }
      }

      generateCallToUpdateFakeState();

      if (isConstructor) {
         generateConditionalCallForFakedConstructor();
      }
      else {
         generateConditionalCallForFakedMethod(startOfRealImplementation);
      }
   }

   private void generateCallToUpdateFakeState()
   {
      if (useMockingBridgeForUpdatingFakeState) {
         generateCallToControlMethodThroughMockingBridge();
         mw.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
         mw.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
      }
      else {
         mw.visitLdcInsn(fakeMethods.getFakeClassInternalName());
         generateCodeToPassThisOrNullIfStaticMethod();
         mw.visitIntInsn(SIPUSH, fakeMethod.getIndexForFakeState());
         mw.visitMethodInsn(
            INVOKESTATIC, "mockit/internal/state/TestRun", "updateFakeState",
            "(Ljava/lang/String;Ljava/lang/Object;I)Z", false);
      }
   }

   private void generateCallToControlMethodThroughMockingBridge()
   {
      generateCodeToObtainInstanceOfMockingBridge(FakeBridge.MB);

      // First and second "invoke" arguments:
      generateCodeToPassThisOrNullIfStaticMethod();
      mw.visitInsn(ACONST_NULL);

      // Create array for call arguments (third "invoke" argument):
      generateCodeToCreateArrayOfObject(mw, 2);

      int i = 0;
      generateCodeToFillArrayElement(i++, fakeMethods.getFakeClassInternalName());
      generateCodeToFillArrayElement(i, fakeMethod.getIndexForFakeState());

      generateCallToInvocationHandler();
   }

   private void generateConditionalCallForFakedMethod(@Nullable Label startOfRealImplementation)
   {
      if (startOfRealImplementation == null) {
         //noinspection AssignmentToMethodParameter
         startOfRealImplementation = new Label();
      }

      mw.visitJumpInsn(IFEQ, startOfRealImplementation);
      generateCallToFakeMethod();
      generateMethodReturn();
      mw.visitLabel(startOfRealImplementation);
   }

   private void generateConditionalCallForFakedConstructor()
   {
      generateCallToFakeMethod();

      int jumpInsnOpcode;

      if (shouldUseMockingBridge()) {
         mw.visitLdcInsn(VOID_TYPE);
         jumpInsnOpcode = IF_ACMPEQ;
      }
      else {
         jumpInsnOpcode = fakeMethod.hasInvocationParameter ? IFNE : IFEQ;
      }

      Label startOfRealImplementation = new Label();
      mw.visitJumpInsn(jumpInsnOpcode, startOfRealImplementation);
      mw.visitInsn(RETURN);
      mw.visitLabel(startOfRealImplementation);
   }

   private void generateCallToFakeMethod()
   {
      if (shouldUseMockingBridge()) {
         generateCallToFakeMethodThroughMockingBridge();
      }
      else {
         generateDirectCallToFakeMethod();
      }
   }

   private boolean shouldUseMockingBridge() { return useMockingBridge || !fakeMethod.isPublic(); }

   private void generateCallToFakeMethodThroughMockingBridge()
   {
      generateCodeToObtainInstanceOfMockingBridge(FakeMethodBridge.MB);

      // First and second "invoke" arguments:
      boolean isStatic = generateCodeToPassThisOrNullIfStaticMethod();
      mw.visitInsn(ACONST_NULL);

      // Create array for call arguments (third "invoke" argument):
      Type[] argTypes = Type.getArgumentTypes(methodDesc);
      generateCodeToCreateArrayOfObject(mw, 6 + argTypes.length);

      int i = 0;
      generateCodeToFillArrayElement(i++, fakeMethods.getFakeClassInternalName());
      generateCodeToFillArrayElement(i++, classDesc);
      generateCodeToFillArrayElement(i++, methodAccess);

      if (fakeMethod.isAdvice) {
         generateCodeToFillArrayElement(i++, methodName);
         generateCodeToFillArrayElement(i++, methodDesc);
      }
      else {
         generateCodeToFillArrayElement(i++, fakeMethod.name);
         generateCodeToFillArrayElement(i++, fakeMethod.desc);
      }

      generateCodeToFillArrayElement(i++, fakeMethod.getIndexForFakeState());

      generateCodeToFillArrayWithParameterValues(mw, argTypes, i, isStatic ? 0 : 1);
      generateCallToInvocationHandler();
   }

   private void generateDirectCallToFakeMethod()
   {
      String fakeClassDesc = fakeMethods.getFakeClassInternalName();
      int invokeOpcode;

      if (fakeMethod.isStatic()) {
         invokeOpcode = INVOKESTATIC;
      }
      else {
         generateCodeToObtainFakeInstance(fakeClassDesc);
         invokeOpcode = INVOKEVIRTUAL;
      }

      boolean canProceedIntoConstructor = generateArgumentsForFakeMethodInvocation();
      mw.visitMethodInsn(invokeOpcode, fakeClassDesc, fakeMethod.name, fakeMethod.desc, false);

      if (canProceedIntoConstructor) {
         mw.visitMethodInsn(
            INVOKEVIRTUAL, "mockit/internal/faking/MockInvocation", "shouldProceedIntoConstructor", "()Z", false);
      }
   }

   private void generateCodeToObtainFakeInstance(@Nonnull String fakeClassDesc)
   {
      mw.visitLdcInsn(fakeClassDesc);
      generateCodeToPassThisOrNullIfStaticMethod();
      mw.visitMethodInsn(
         INVOKESTATIC, "mockit/internal/state/TestRun", "getFake",
         "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;", false);
      mw.visitTypeInsn(CHECKCAST, fakeClassDesc);
   }

   private boolean generateArgumentsForFakeMethodInvocation()
   {
      String fakedDesc = fakeMethod.isAdvice ? methodDesc : fakeMethod.fakeDescWithoutInvocationParameter;
      Type[] argTypes = Type.getArgumentTypes(fakedDesc);
      int varIndex = isStatic(methodAccess) ? 0 : 1;
      boolean canProceedIntoConstructor = false;

      if (fakeMethod.hasInvocationParameter) {
         generateCallToCreateNewFakeInvocation(argTypes, varIndex);

         // When invoking a constructor, the invocation object will need to be consulted for proceeding:
         if (isConstructor) {
            mw.visitInsn(fakeMethod.isStatic() ? DUP : DUP_X1);
            canProceedIntoConstructor = true;
         }
      }

      if (!fakeMethod.isAdvice) {
         boolean forGenericMethod = fakeMethod.isForGenericMethod();

         for (Type argType : argTypes) {
            int opcode = argType.getOpcode(ILOAD);
            mw.visitVarInsn(opcode, varIndex);

            if (forGenericMethod && argType.getSort() >= Type.ARRAY) {
               mw.visitTypeInsn(CHECKCAST, argType.getInternalName());
            }

            varIndex += argType.getSize();
         }
      }

      return canProceedIntoConstructor;
   }

   private void generateCallToCreateNewFakeInvocation(@Nonnull Type[] argTypes, int initialParameterIndex)
   {
      generateCodeToPassThisOrNullIfStaticMethod();

      int argCount = argTypes.length;

      if (argCount == 0) {
         mw.visitInsn(ACONST_NULL);
      }
      else {
         generateCodeToCreateArrayOfObject(mw, argCount);
         generateCodeToFillArrayWithParameterValues(mw, argTypes, 0, initialParameterIndex);
      }

      mw.visitLdcInsn(fakeMethods.getFakeClassInternalName());
      mw.visitIntInsn(SIPUSH, fakeMethod.getIndexForFakeState());
      mw.visitLdcInsn(classDesc);
      mw.visitLdcInsn(methodName);
      mw.visitLdcInsn(methodDesc);

      mw.visitMethodInsn(
         INVOKESTATIC, "mockit/internal/faking/MockInvocation", "create",
         "(Ljava/lang/Object;[Ljava/lang/Object;Ljava/lang/String;I" +
         "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lmockit/internal/faking/MockInvocation;", false);
   }

   private void generateMethodReturn()
   {
      if (shouldUseMockingBridge() || fakeMethod.isAdvice) {
         generateReturnWithObjectAtTopOfTheStack(methodDesc);
      }
      else {
         Type returnType = Type.getReturnType(methodDesc);
         mw.visitInsn(returnType.getOpcode(IRETURN));
      }
   }
}
