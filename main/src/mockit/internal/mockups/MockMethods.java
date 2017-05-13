/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.mockups;

import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;
import static java.lang.reflect.Modifier.*;

import mockit.internal.*;
import mockit.internal.reflection.*;
import mockit.internal.state.*;
import mockit.internal.util.*;
import mockit.internal.reflection.GenericTypeReflection.*;
import static mockit.internal.util.ObjectMethods.*;

/**
 * A container for the mock methods "collected" from a mockup class, separated in two sets: one with all the mock
 * methods, and another with just the subset of static methods.
 */
final class MockMethods
{
   @Nonnull final Class<?> realClass;
   private final boolean targetTypeIsAClass;
   private final boolean reentrantRealClass;
   @Nonnull private final List<MockMethod> methods;
   @Nullable private MockMethod adviceMethod;
   @Nonnull private final GenericTypeReflection typeParametersToTypeArguments;
   @Nonnull private String mockClassInternalName;
   @Nullable private List<MockState> mockStates;

   final class MockMethod
   {
      private final int access;
      @Nonnull final String name;
      @Nonnull final String desc;
      final boolean isAdvice;
      final boolean hasInvocationParameter;
      @Nonnull final String mockDescWithoutInvocationParameter;
      private boolean hasMatchingRealMethod;
      @Nullable private GenericSignature mockSignature;
      private int indexForMockState;
      private boolean nativeRealMethod;

      private MockMethod(int access, @Nonnull String name, @Nonnull String desc)
      {
         this.access = access;
         this.name = name;
         this.desc = desc;

         if (desc.contains("Lmockit/Invocation;")) {
            hasInvocationParameter = true;
            mockDescWithoutInvocationParameter = '(' + desc.substring(20);
            isAdvice = "$advice".equals(name) && "()Ljava/lang/Object;".equals(mockDescWithoutInvocationParameter);
         }
         else {
            hasInvocationParameter = false;
            mockDescWithoutInvocationParameter = desc;
            isAdvice = false;
         }

         hasMatchingRealMethod = false;
         indexForMockState = -1;
      }

      boolean isMatch(int realAccess, @Nonnull String realName, @Nonnull String realDesc, @Nullable String signature)
      {
         if (name.equals(realName) && hasMatchingParameters(realDesc, signature)) {
            hasMatchingRealMethod = true;
            nativeRealMethod = isNative(realAccess);
            return true;
         }

         return false;
      }

      private boolean hasMatchingParameters(@Nonnull String methodDesc, @Nullable String signature)
      {
         boolean sameParametersIgnoringGenerics = mockDescWithoutInvocationParameter.equals(methodDesc);

         if (sameParametersIgnoringGenerics || signature == null) {
            return sameParametersIgnoringGenerics;
         }

         if (mockSignature == null) {
            mockSignature = typeParametersToTypeArguments.parseSignature(mockDescWithoutInvocationParameter);
         }

         return mockSignature.satisfiesGenericSignature(signature);
      }

      @Nonnull Class<?> getRealClass() { return realClass; }
      @Nonnull String getMockNameAndDesc() { return name + desc; }
      int getIndexForMockState() { return indexForMockState; }

      boolean isStatic() { return Modifier.isStatic(access); }
      boolean isPublic() { return Modifier.isPublic(access); }
      boolean isForGenericMethod() { return mockSignature != null; }
      boolean isForNativeMethod() { return nativeRealMethod; }
      boolean requiresMockState() { return hasInvocationParameter || reentrantRealClass; }
      boolean canBeReentered() { return targetTypeIsAClass && !nativeRealMethod; }

      @Override @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
      public boolean equals(Object obj)
      {
         MockMethod other = (MockMethod) obj;
         return realClass == other.getRealClass() && name.equals(other.name) && desc.equals(other.desc);
      }

      @Override
      public int hashCode()
      {
         return 31 * (31 * realClass.hashCode() + name.hashCode()) + desc.hashCode();
      }
   }

   MockMethods(@Nonnull Class<?> realClass, @Nullable Type targetType)
   {
      this.realClass = realClass;

      if (targetType == null || realClass == targetType) {
         targetTypeIsAClass = true;
      }
      else {
         Class<?> targetClass = Utilities.getClassType(targetType);
         targetTypeIsAClass = !targetClass.isInterface();
      }

      reentrantRealClass = targetTypeIsAClass && MockingBridge.instanceOfClassThatParticipatesInClassLoading(realClass);
      methods = new ArrayList<MockMethod>();
      typeParametersToTypeArguments = new GenericTypeReflection(realClass, targetType);
      mockClassInternalName = "";
   }

   @Nonnull Class<?> getRealClass() { return realClass; }

   @Nullable
   MockMethod addMethod(boolean fromSuperClass, int access, @Nonnull String name, @Nonnull String desc)
   {
      if (fromSuperClass && isMethodAlreadyAdded(name, desc)) {
         return null;
      }

      MockMethod mockMethod = new MockMethod(access, name, desc);

      if (mockMethod.isAdvice) {
         adviceMethod = mockMethod;
      }
      else {
         methods.add(mockMethod);
      }

      return mockMethod;
   }

   private boolean isMethodAlreadyAdded(@Nonnull String name, @Nonnull String desc)
   {
      int p = desc.lastIndexOf(')');
      String params = desc.substring(0, p + 1);

      for (MockMethod mockMethod : methods) {
         if (mockMethod.name.equals(name) && mockMethod.desc.startsWith(params)) {
            return true;
         }
      }

      return false;
   }

   void addMockState(@Nonnull MockState mockState)
   {
      if (mockStates == null) {
         mockStates = new ArrayList<MockState>(4);
      }

      mockState.mockMethod.indexForMockState = mockStates.size();
      mockStates.add(mockState);
   }

   @Nullable List<MockState> getMockStates() { return mockStates; }

   /**
    * Finds a mock method with the same signature of a given real method, if previously collected from the mockup class.
    * This operation can be performed only once for any given mock method in this container, so that after the last real
    * method is processed there should be no mock methods left unused in the container.
    */
   @Nullable
   MockMethod findMethod(int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature)
   {
      for (MockMethod mockMethod : methods) {
         if (mockMethod.isMatch(access, name, desc, signature)) {
            return mockMethod;
         }
      }

      if (
         adviceMethod != null && !isNative(access) && !"$init".equals(name) && !"$clinit".equals(name) &&
         !isMethodFromObject(name, desc)
      ) {
         return adviceMethod;
      }

      return null;
   }

   @Nonnull String getMockClassInternalName() { return mockClassInternalName; }

   void setMockClassInternalName(@Nonnull String mockClassInternalName)
   {
      this.mockClassInternalName = mockClassInternalName.intern();
   }

   boolean hasUnusedMocks()
   {
      if (adviceMethod != null) {
         return true;
      }

      for (MockMethod method : methods) {
         if (!method.hasMatchingRealMethod) {
            return true;
         }
      }

      return false;
   }

   void registerMockStates(@Nonnull Object mockUp, boolean forStartupMock)
   {
      if (mockStates != null) {
         MockStates allMockStates = TestRun.getMockStates();

         if (forStartupMock) {
            allMockStates.addStartupMockUpAndItsMockStates(mockUp, mockStates);
         }
         else {
            allMockStates.addMockUpAndItsMockStates(mockUp, mockStates);
         }
      }
   }
}
