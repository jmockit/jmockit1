/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.util.*;
import javax.annotation.*;
import static java.lang.reflect.Modifier.*;

import mockit.external.asm.*;
import mockit.internal.*;
import mockit.internal.expectations.*;
import mockit.internal.util.*;
import static mockit.external.asm.Opcodes.*;
import static mockit.internal.MockingFilters.*;
import static mockit.internal.expectations.mocking.MockedTypeModifier.*;
import static mockit.internal.util.ObjectMethods.isMethodFromObject;
import static mockit.internal.util.Utilities.*;

final class MockedClassModifier extends BaseClassModifier
{
   private static final boolean NATIVE_UNSUPPORTED = !HOTSPOT_VM;
   private static final int METHOD_ACCESS_MASK = Access.PRIVATE + Access.SYNTHETIC + Access.ABSTRACT;
   private static final int PUBLIC_OR_PROTECTED = Access.PUBLIC + Access.PROTECTED;

   @Nullable private final MockedType mockedType;
   private final boolean classFromNonBootstrapClassLoader;
   private String className;
   @Nullable private String baseClassNameForCapturedInstanceMethods;
   private boolean ignoreConstructors;
   private ExecutionMode executionMode;
   private boolean isProxy;
   @Nullable private String defaultFilters;
   @Nullable List<String> enumSubclasses;

   MockedClassModifier(
      @Nullable ClassLoader classLoader, @Nonnull ClassReader classReader, @Nullable MockedType typeMetadata)
   {
      super(classReader);
      mockedType = typeMetadata;
      classFromNonBootstrapClassLoader = classLoader != null;
      setUseClassLoadingBridge(classLoader);
      executionMode = ExecutionMode.Regular;
      useInstanceBasedMockingIfApplicable();
   }

   private void useInstanceBasedMockingIfApplicable()
   {
      if (mockedType != null && mockedType.injectable) {
         ignoreConstructors = mockedType.getMaxInstancesToCapture() <= 0;
         executionMode = ExecutionMode.PerInstance;
      }
   }

   public void setClassNameForCapturedInstanceMethods(@Nonnull String internalClassName)
   {
      baseClassNameForCapturedInstanceMethods = internalClassName;
   }

   public void useDynamicMocking(boolean methodsOnly)
   {
      ignoreConstructors = methodsOnly;
      executionMode = ExecutionMode.Partial;
   }

   @Override
   public void visit(
      int version, int access, @Nonnull String name, @Nullable String signature, @Nullable String superName,
      @Nullable String[] interfaces)
   {
      validateMockingOfJREClass(name);

      super.visit(version, access, name, signature, superName, interfaces);
      isProxy = "java/lang/reflect/Proxy".equals(superName);

      if (isProxy) {
         assert interfaces != null;
         className = interfaces[0];
         defaultFilters = null;
      }
      else {
         className = name;
         defaultFilters = filtersForClass(name);

         if (defaultFilters != null && defaultFilters.isEmpty()) {
            throw VisitInterruptedException.INSTANCE;
         }
      }
   }

   private void validateMockingOfJREClass(@Nonnull String internalName)
   {
      if (internalName.startsWith("java/")) {
         if (isUnmockable(internalName)) {
            throw new IllegalArgumentException("Class " + internalName.replace('/', '.') + " is not mockable");
         }

         if (executionMode == ExecutionMode.Regular && mockedType != null && isFullMockingDisallowed(internalName)) {
            String modifyingClassName = internalName.replace('/', '.');

            if (modifyingClassName.equals(mockedType.getClassType().getName())) {
               throw new IllegalArgumentException(
                  "Class " + internalName.replace('/', '.') + " cannot be @Mocked fully; " +
                  "instead, use @Injectable or partial mocking");
            }
         }
      }
   }

   @Override
   public void visitInnerClass(@Nonnull String name, @Nullable String outerName, @Nullable String innerName, int access)
   {
      cw.visitInnerClass(name, outerName, innerName, access);

      if (access == Access.ENUM + Access.STATIC) {
         if (enumSubclasses == null) {
            enumSubclasses = new ArrayList<String>();
         }

         enumSubclasses.add(name);
      }
   }

   @Nullable @Override
   public MethodVisitor visitMethod(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions)
   {
      if ((access & METHOD_ACCESS_MASK) != 0) {
         return unmodifiedBytecode(access, name, desc, signature, exceptions);
      }

      boolean visitingConstructor = "<init>".equals(name);
      String internalClassName = className;

      if (visitingConstructor) {
         if (isConstructorNotAllowedByMockingFilters(name)) {
            return unmodifiedBytecode(access, name, desc, signature, exceptions);
         }

         startModifiedMethodVersion(access, name, desc, signature, exceptions);
         generateCallToSuperConstructor();
      }
      else {
         if (isMethodNotToBeMocked(access, name, desc)) {
            return unmodifiedBytecode(access, name, desc, signature, exceptions);
         }

         if ("<clinit>".equals(name)) {
            return stubOutClassInitializationIfApplicable(access);
         }

         if (stubOutFinalizeMethod(access, name, desc)) {
            return null;
         }

         if (isMethodNotAllowedByMockingFilters(access, name)) {
            return unmodifiedBytecode(access, name, desc, signature, exceptions);
         }

         startModifiedMethodVersion(access, name, desc, signature, exceptions);

         if (baseClassNameForCapturedInstanceMethods != null) {
            internalClassName = baseClassNameForCapturedInstanceMethods;
         }
      }

      ExecutionMode actualExecutionMode = determineAppropriateExecutionMode(visitingConstructor);

      if (useClassLoadingBridge) {
         return generateCallToHandlerThroughMockingBridge(
            signature, internalClassName, visitingConstructor, actualExecutionMode);
      }

      generateDirectCallToHandler(mw, internalClassName, access, name, desc, signature, actualExecutionMode);
      generateDecisionBetweenReturningOrContinuingToRealImplementation();

      // Constructors of non-JRE classes can't be modified (unless running with "-noverify") in a way that
      // "super(...)/this(...)" get called twice, so we disregard such calls when copying the original bytecode.
      return copyOriginalImplementationCode(visitingConstructor);
   }

   private boolean isConstructorNotAllowedByMockingFilters(@Nonnull String name)
   {
      return isProxy || ignoreConstructors || isUnmockableInvocation(defaultFilters, name);
   }

   private boolean isMethodNotToBeMocked(int access, @Nonnull String name, @Nonnull String desc)
   {
      return
         isNative(access) && (NATIVE_UNSUPPORTED || (access & PUBLIC_OR_PROTECTED) == 0) ||
         (isProxy || executionMode == ExecutionMode.Partial) && (
            isMethodFromObject(name, desc) || "annotationType".equals(name) && "()Ljava/lang/Class;".equals(desc)
         );
   }

   @Nonnull
   private MethodVisitor unmodifiedBytecode(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions)
   {
      return cw.visitMethod(access, name, desc, signature, exceptions);
   }

   @Nullable
   private MethodVisitor stubOutClassInitializationIfApplicable(int access)
   {
      startModifiedMethodVersion(access, "<clinit>", "()V", null, null);

      if (mockedType != null && mockedType.isClassInitializationToBeStubbedOut()) {
         generateEmptyImplementation();
         return null;
      }

      return mw;
   }

   private boolean stubOutFinalizeMethod(int access, @Nonnull String name, @Nonnull String desc)
   {
      if ("finalize".equals(name) && "()V".equals(desc)) {
         startModifiedMethodVersion(access, name, desc, null, null);
         generateEmptyImplementation();
         return true;
      }

      return false;
   }

   private boolean isMethodNotAllowedByMockingFilters(int access, @Nonnull String name)
   {
      return
         baseClassNameForCapturedInstanceMethods != null && (access & Access.STATIC) != 0 ||
         executionMode.isMethodToBeIgnored(access) || isUnmockableInvocation(defaultFilters, name);
   }

   @Nonnull
   private ExecutionMode determineAppropriateExecutionMode(boolean visitingConstructor)
   {
      if (executionMode == ExecutionMode.PerInstance) {
         if (visitingConstructor) {
            return ignoreConstructors ? ExecutionMode.Regular : ExecutionMode.Partial;
         }

         if (isStatic(methodAccess)) {
            return ExecutionMode.Partial;
         }
      }

      return executionMode;
   }

   @Nonnull
   private MethodVisitor generateCallToHandlerThroughMockingBridge(
      @Nullable String genericSignature, @Nonnull String internalClassName, boolean visitingConstructor,
      @Nonnull ExecutionMode actualExecutionMode)
   {
      generateCodeToObtainInstanceOfClassLoadingBridge(MockedBridge.MB);

      // First and second "invoke" arguments:
      boolean isStatic = generateCodeToPassThisOrNullIfStaticMethod();
      mw.visitInsn(ACONST_NULL);

      // Create array for call arguments (third "invoke" argument):
      JavaType[] argTypes = JavaType.getArgumentTypes(methodDesc);
      generateCodeToCreateArrayOfObject(mw, 6 + argTypes.length);

      int i = 0;
      generateCodeToFillArrayElement(i++, methodAccess);
      generateCodeToFillArrayElement(i++, internalClassName);
      generateCodeToFillArrayElement(i++, methodName);
      generateCodeToFillArrayElement(i++, methodDesc);
      generateCodeToFillArrayElement(i++, genericSignature);
      generateCodeToFillArrayElement(i++, actualExecutionMode.ordinal());

      generateCodeToFillArrayWithParameterValues(mw, argTypes, i, isStatic ? 0 : 1);
      generateCallToInvocationHandler();

      generateDecisionBetweenReturningOrContinuingToRealImplementation();

      // Copies the entire original implementation even for a constructor, in which case the complete bytecode inside
      // the constructor fails the strict verification activated by "-Xfuture". However, this is necessary to allow the
      // full execution of a bootstrap class constructor when the call was not meant to be mocked.
      return copyOriginalImplementationCode(visitingConstructor && classFromNonBootstrapClassLoader);
   }

   private void generateDecisionBetweenReturningOrContinuingToRealImplementation()
   {
      Label startOfRealImplementation = new Label();
      mw.visitInsn(DUP);
      mw.visitLdcInsn(VOID_TYPE);
      mw.visitJumpInsn(IF_ACMPEQ, startOfRealImplementation);
      generateReturnWithObjectAtTopOfTheStack(methodDesc);
      mw.visitLabel(startOfRealImplementation);
      mw.visitInsn(POP);
   }
}
