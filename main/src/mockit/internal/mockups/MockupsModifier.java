/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.mockups;

import javax.annotation.*;
import static java.lang.reflect.Modifier.*;

import mockit.*;
import mockit.external.asm.*;
import mockit.internal.*;
import mockit.internal.mockups.MockMethods.*;
import mockit.internal.state.*;
import mockit.internal.util.*;
import static mockit.external.asm.Opcodes.*;

/**
 * Responsible for generating all necessary bytecode in the redefined (real) class.
 * Such code will redirect calls made on "real" methods to equivalent calls on the corresponding "mock" methods.
 * The original code won't be executed by the running JVM until the class redefinition is undone.
 * <p/>
 * Methods in the real class with no corresponding mock methods are unaffected.
 * <p/>
 * Any fields (static or not) in the real class remain untouched.
 */
final class MockupsModifier extends BaseClassModifier
{
   private static final int ABSTRACT_OR_SYNTHETIC = ACC_ABSTRACT + ACC_SYNTHETIC;

   @Nonnull private final MockMethods mockMethods;
   private final boolean useMockingBridgeForUpdatingMockState;
   @Nonnull private final Class<?> mockedClass;
   private MockMethod mockMethod;
   private boolean isConstructor;

   /**
    * Initializes the modifier for a given real/mock class pair.
    * <p/>
    * The mock instance provided will receive calls for any instance methods defined in the mock class.
    * Therefore, it needs to be later recovered by the modified bytecode inside the real method.
    * To enable this, the mock instance is added to a global data structure made available through the
    * {@link TestRun#getMock(String, Object)} method.
    *
    * @param cr the class file reader for the real class
    * @param realClass the class to be mocked-up, or a base type of an implementation class to be mocked-up
    * @param mockUp an instance of the mockup class
    * @param mockMethods contains the set of mock methods collected from the mock class; each mock method is identified
    * by a pair composed of "name" and "desc", where "name" is the method name, and "desc" is the JVM internal
    * description of the parameters; once the real class modification is complete this set will be empty, unless no
    * corresponding real method was found for any of its method identifiers
    */
   MockupsModifier(
      @Nonnull ClassReader cr, @Nonnull Class<?> realClass, @Nonnull MockUp<?> mockUp, @Nonnull MockMethods mockMethods)
   {
      super(cr);
      mockedClass = realClass;
      this.mockMethods = mockMethods;

      ClassLoader classLoaderOfRealClass = realClass.getClassLoader();
      useMockingBridgeForUpdatingMockState = ClassLoad.isClassLoaderWithNoDirectAccess(classLoaderOfRealClass);
      inferUseOfMockingBridge(classLoaderOfRealClass, mockUp);
   }

   private void inferUseOfMockingBridge(@Nullable ClassLoader classLoaderOfRealClass, @Nonnull Object mock)
   {
      setUseMockingBridge(classLoaderOfRealClass);

      if (!useMockingBridge && !isPublic(mock.getClass().getModifiers())) {
         useMockingBridge = true;
      }
   }

   /**
    * If the specified method has a mock definition, then generates bytecode to redirect calls made to it to the mock
    * method. If it has no mock, does nothing.
    *
    * @param access not relevant
    * @param name together with desc, used to identity the method in given set of mock methods
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
            // Marks a matching mock method (if any) as having the corresponding mocked method.
            mockMethods.findMethod(access, name, desc, signature);
         }

         return cw.visitMethod(access, name, desc, signature, exceptions);
      }

      isConstructor = "<init>".equals(name);

      if (isConstructor && isMockedSuperclass() || !hasMock(access, name, desc, signature)) {
         return cw.visitMethod(access, name, desc, signature, exceptions);
      }

      startModifiedMethodVersion(access, name, desc, signature, exceptions);

      if (isConstructor) {
         generateCallToSuperConstructor();
      }
      else if (isNative(methodAccess)) {
         generateCallToUpdateMockState();
         generateCallToMockMethod();
         generateMethodReturn();
         mw.visitMaxs(1, 0); // dummy values, real ones are calculated by ASM
         return methodAnnotationsVisitor;
      }

      generateDynamicCallToMock();
      return copyOriginalImplementationCode(isConstructor);
   }

   private boolean hasMock(int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature)
   {
      String mockName = getCorrespondingMockName(name);
      mockMethod = mockMethods.findMethod(access, mockName, desc, signature);
      return mockMethod != null;
   }

   @Nonnull
   private static String getCorrespondingMockName(@Nonnull String name)
   {
      if ("<init>".equals(name)) {
         return "$init";
      }

      if ("<clinit>".equals(name)) {
         return "$clinit";
      }

      return name;
   }

   private boolean isMockedSuperclass() { return mockedClass != mockMethods.getRealClass(); }

   private void generateDynamicCallToMock()
   {
      Label startOfRealImplementation = null;

      if (!isStatic(methodAccess) && !isConstructor && isMockedSuperclass()) {
         Class<?> targetClass = mockMethods.getRealClass();

         if (mockedClass.getClassLoader() == targetClass.getClassLoader()) {
            startOfRealImplementation = new Label();
            mw.visitVarInsn(ALOAD, 0);
            mw.visitTypeInsn(INSTANCEOF, Type.getInternalName(targetClass));
            mw.visitJumpInsn(IFEQ, startOfRealImplementation);
         }
      }

      generateCallToUpdateMockState();

      if (isConstructor) {
         generateConditionalCallForMockedConstructor();
      }
      else {
         generateConditionalCallForMockedMethod(startOfRealImplementation);
      }
   }

   private void generateCallToUpdateMockState()
   {
      if (useMockingBridgeForUpdatingMockState) {
         generateCallToControlMethodThroughMockingBridge();
         mw.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
         mw.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
      }
      else {
         mw.visitLdcInsn(mockMethods.getMockClassInternalName());
         generateCodeToPassThisOrNullIfStaticMethod();
         mw.visitIntInsn(SIPUSH, mockMethod.getIndexForMockState());
         mw.visitMethodInsn(
            INVOKESTATIC, "mockit/internal/state/TestRun", "updateMockState",
            "(Ljava/lang/String;Ljava/lang/Object;I)Z", false);
      }
   }

   private void generateCallToControlMethodThroughMockingBridge()
   {
      generateCodeToObtainInstanceOfMockingBridge(MockupBridge.MB);

      // First and second "invoke" arguments:
      generateCodeToPassThisOrNullIfStaticMethod();
      mw.visitInsn(ACONST_NULL);

      // Create array for call arguments (third "invoke" argument):
      generateCodeToCreateArrayOfObject(mw, 2);

      int i = 0;
      generateCodeToFillArrayElement(i++, mockMethods.getMockClassInternalName());
      generateCodeToFillArrayElement(i, mockMethod.getIndexForMockState());

      generateCallToInvocationHandler();
   }

   private void generateConditionalCallForMockedMethod(@Nullable Label startOfRealImplementation)
   {
      if (startOfRealImplementation == null) {
         //noinspection AssignmentToMethodParameter
         startOfRealImplementation = new Label();
      }

      mw.visitJumpInsn(IFEQ, startOfRealImplementation);
      generateCallToMockMethod();
      generateMethodReturn();
      mw.visitLabel(startOfRealImplementation);
   }

   private void generateConditionalCallForMockedConstructor()
   {
      generateCallToMockMethod();

      int jumpInsnOpcode;

      if (shouldUseMockingBridge()) {
         mw.visitLdcInsn(VOID_TYPE);
         jumpInsnOpcode = IF_ACMPEQ;
      }
      else {
         jumpInsnOpcode = mockMethod.hasInvocationParameter ? IFNE : IFEQ;
      }

      Label startOfRealImplementation = new Label();
      mw.visitJumpInsn(jumpInsnOpcode, startOfRealImplementation);
      mw.visitInsn(RETURN);
      mw.visitLabel(startOfRealImplementation);
   }

   private void generateCallToMockMethod()
   {
      if (shouldUseMockingBridge()) {
         generateCallToMockMethodThroughMockingBridge();
      }
      else {
         generateDirectCallToMockMethod();
      }
   }

   private boolean shouldUseMockingBridge() { return useMockingBridge || !mockMethod.isPublic(); }

   private void generateCallToMockMethodThroughMockingBridge()
   {
      generateCodeToObtainInstanceOfMockingBridge(MockMethodBridge.MB);

      // First and second "invoke" arguments:
      boolean isStatic = generateCodeToPassThisOrNullIfStaticMethod();
      mw.visitInsn(ACONST_NULL);

      // Create array for call arguments (third "invoke" argument):
      Type[] argTypes = Type.getArgumentTypes(methodDesc);
      generateCodeToCreateArrayOfObject(mw, 6 + argTypes.length);

      int i = 0;
      generateCodeToFillArrayElement(i++, mockMethods.getMockClassInternalName());
      generateCodeToFillArrayElement(i++, classDesc);
      generateCodeToFillArrayElement(i++, methodAccess);

      if (mockMethod.isAdvice) {
         generateCodeToFillArrayElement(i++, methodName);
         generateCodeToFillArrayElement(i++, methodDesc);
      }
      else {
         generateCodeToFillArrayElement(i++, mockMethod.name);
         generateCodeToFillArrayElement(i++, mockMethod.desc);
      }

      generateCodeToFillArrayElement(i++, mockMethod.getIndexForMockState());

      generateCodeToFillArrayWithParameterValues(mw, argTypes, i, isStatic ? 0 : 1);
      generateCallToInvocationHandler();
   }

   private void generateDirectCallToMockMethod()
   {
      String mockClassDesc = mockMethods.getMockClassInternalName();
      int invokeOpcode;

      if (mockMethod.isStatic()) {
         invokeOpcode = INVOKESTATIC;
      }
      else {
         generateCodeToObtainMockUpInstance(mockClassDesc);
         invokeOpcode = INVOKEVIRTUAL;
      }

      boolean canProceedIntoConstructor = generateArgumentsForMockMethodInvocation();
      mw.visitMethodInsn(invokeOpcode, mockClassDesc, mockMethod.name, mockMethod.desc, false);

      if (canProceedIntoConstructor) {
         mw.visitMethodInsn(
            INVOKEVIRTUAL, "mockit/internal/mockups/MockInvocation", "shouldProceedIntoConstructor", "()Z", false);
      }
   }

   private void generateCodeToObtainMockUpInstance(@Nonnull String mockClassDesc)
   {
      mw.visitLdcInsn(mockClassDesc);
      generateCodeToPassThisOrNullIfStaticMethod();
      mw.visitMethodInsn(
         INVOKESTATIC, "mockit/internal/state/TestRun", "getMock",
         "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;", false);
      mw.visitTypeInsn(CHECKCAST, mockClassDesc);
   }

   private boolean generateArgumentsForMockMethodInvocation()
   {
      String mockedDesc = mockMethod.isAdvice ? methodDesc : mockMethod.mockDescWithoutInvocationParameter;
      Type[] argTypes = Type.getArgumentTypes(mockedDesc);
      int varIndex = isStatic(methodAccess) ? 0 : 1;
      boolean canProceedIntoConstructor = false;

      if (mockMethod.hasInvocationParameter) {
         generateCallToCreateNewMockInvocation(argTypes, varIndex);

         // When invoking a constructor, the invocation object will need to be consulted for proceeding:
         if (isConstructor) {
            mw.visitInsn(mockMethod.isStatic() ? DUP : DUP_X1);
            canProceedIntoConstructor = true;
         }
      }

      if (!mockMethod.isAdvice) {
         boolean forGenericMethod = mockMethod.isForGenericMethod();

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

   private void generateCallToCreateNewMockInvocation(@Nonnull Type[] argTypes, int initialParameterIndex)
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

      mw.visitLdcInsn(mockMethods.getMockClassInternalName());
      mw.visitIntInsn(SIPUSH, mockMethod.getIndexForMockState());
      mw.visitLdcInsn(classDesc);
      mw.visitLdcInsn(methodName);
      mw.visitLdcInsn(methodDesc);

      mw.visitMethodInsn(
         INVOKESTATIC, "mockit/internal/mockups/MockInvocation", "create",
         "(Ljava/lang/Object;[Ljava/lang/Object;Ljava/lang/String;I" +
         "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lmockit/internal/mockups/MockInvocation;", false);
   }

   private void generateMethodReturn()
   {
      if (shouldUseMockingBridge() || mockMethod.isAdvice) {
         generateReturnWithObjectAtTopOfTheStack(methodDesc);
      }
      else {
         Type returnType = Type.getReturnType(methodDesc);
         mw.visitInsn(returnType.getOpcode(IRETURN));
      }
   }
}
