/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.state;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import javax.annotation.*;

import mockit.internal.expectations.mocking.*;
import mockit.internal.util.*;
import static mockit.external.asm.Type.*;
import static mockit.internal.util.Utilities.*;

public final class MockedTypeCascade
{
   @Nonnull private static final CascadingTypes CASCADING_TYPES = TestRun.getExecutingTest().getCascadingTypes();

   final boolean fromMockField;
   @Nonnull private final Type mockedType;
   @Nullable Class<?> mockedClass;
   @Nullable GenericTypeReflection genericReflection;
   @Nullable private final Object cascadedInstance;
   @Nonnull private final Map<String, Type> cascadedTypesAndMocks;

   MockedTypeCascade(boolean fromMockField, @Nonnull Type mockedType, @Nullable Object cascadedInstance)
   {
      this.fromMockField = fromMockField;
      this.mockedType = mockedType;
      this.cascadedInstance = cascadedInstance;
      cascadedTypesAndMocks = new ConcurrentHashMap<String, Type>(4);
   }

   @Nullable
   public static Object getMock(
      @Nonnull String mockedTypeDesc, @Nonnull String mockedMethodNameAndDesc, @Nullable Object mockInstance,
      @Nonnull String returnTypeDesc, @Nonnull Class<?> returnType)
   {
      MockedTypeCascade cascade = CASCADING_TYPES.getCascade(mockedTypeDesc, mockInstance);

      if (cascade == null) {
         return null;
      }

      String cascadedReturnTypeDesc = getReturnTypeIfCascadingSupportedForIt(returnTypeDesc);

      if (cascadedReturnTypeDesc == null) {
         return null;
      }

      return cascade.getCascadedInstance(mockedMethodNameAndDesc, cascadedReturnTypeDesc, returnType);
   }

   @Nullable
   public static Object getMock(
      @Nonnull String mockedTypeDesc, @Nonnull String mockedMethodNameAndDesc, @Nullable Object mockInstance,
      @Nonnull String returnTypeDesc, @Nullable String genericSignature)
   {
      char typeCode = returnTypeDesc.charAt(0);

      if (typeCode != 'L') {
         return null;
      }

      MockedTypeCascade cascade = CASCADING_TYPES.getCascade(mockedTypeDesc, mockInstance);

      if (cascade == null) {
         return null;
      }

      String resolvedReturnTypeDesc = null;

      if (genericSignature != null) {
         resolvedReturnTypeDesc = cascade.getGenericReturnType(genericSignature);
      }

      if (resolvedReturnTypeDesc == null) {
         resolvedReturnTypeDesc = getReturnTypeIfCascadingSupportedForIt(returnTypeDesc);

         if (resolvedReturnTypeDesc == null) {
            return null;
         }
      }
      else if (resolvedReturnTypeDesc.charAt(0) == '[') {
         return DefaultValues.computeForArrayType(resolvedReturnTypeDesc);
      }

      return cascade.getCascadedInstance(mockedMethodNameAndDesc, resolvedReturnTypeDesc);
   }

   @Nullable
   private String getGenericReturnType(@Nonnull String genericSignature)
   {
      String returnTypeName = getGenericReflection().resolveReturnType(genericSignature);
      return returnTypeName.charAt(0) == '[' || isTypeSupportedForCascading(returnTypeName) ? returnTypeName : null;
   }

   @Nonnull
   private synchronized GenericTypeReflection getGenericReflection()
   {
      GenericTypeReflection reflection = genericReflection;

      if (reflection == null) {
         Class<?> ownerClass = getClassWithCalledMethod();
         reflection = new GenericTypeReflection(ownerClass, mockedType);
         genericReflection = reflection;
      }

      return reflection;
   }

   @Nullable
   private static String getReturnTypeIfCascadingSupportedForIt(@Nonnull Class<?> returnType)
   {
      String typeName = getInternalName(returnType);
      return isTypeSupportedForCascading(typeName) ? typeName : null;
   }

   private static boolean isTypeSupportedForCascading(@Nonnull String typeName)
   {
      //noinspection SimplifiableIfStatement
      if (typeName.contains("/Process") || typeName.endsWith("/Runnable")) {
         return true;
      }

      return
         (!typeName.startsWith("java/lang/") || typeName.contains("management")) &&
         !typeName.startsWith("java/math/") &&
         !typeName.startsWith("java/util/concurrent/atomic/");
   }

   @Nullable
   private static String getReturnTypeIfCascadingSupportedForIt(@Nonnull String typeDesc)
   {
      String typeName = typeDesc.substring(1, typeDesc.length() - 1);
      return isTypeSupportedForCascading(typeName) ? typeName : null;
   }

   @Nullable
   private Object getCascadedInstance(
      @Nonnull String methodNameAndDesc, @Nonnull String returnTypeInternalName, @Nonnull Class<?> returnClass)
   {
      if (!cascadedTypesAndMocks.containsKey(returnTypeInternalName)) {
         cascadedTypesAndMocks.put(returnTypeInternalName, returnClass);
         CASCADING_TYPES.add(returnTypeInternalName, false, returnClass, null);
      }

      return createNewCascadedInstanceOrUseNonCascadedOneIfAvailable(methodNameAndDesc, returnClass);
   }

   @Nullable
   private Object getCascadedInstance(@Nonnull String methodNameAndDesc, @Nonnull String returnTypeInternalName)
   {
      Type returnType = cascadedTypesAndMocks.get(returnTypeInternalName);
      Class<?> returnClass;

      if (returnType == null) {
         Class<?> cascadingClass = getClassWithCalledMethod();

         Type genericReturnType;
         try { genericReturnType = getGenericReturnType(cascadingClass, methodNameAndDesc); }
         catch (NoSuchMethodException ignore) { return null; }

         Class<?> resolvedReturnType = getClassType(genericReturnType);

         if (resolvedReturnType.isAssignableFrom(cascadingClass)) {
            if (cascadedInstance != null) {
               return cascadedInstance;
            }

            returnType = mockedType;
            returnClass = cascadingClass;
         }
         else {
            Object defaultReturnValue = DefaultValues.computeForType(resolvedReturnType);

            if (defaultReturnValue != null) {
               return defaultReturnValue;
            }

            cascadedTypesAndMocks.put(returnTypeInternalName, genericReturnType);
            CASCADING_TYPES.add(returnTypeInternalName, false, genericReturnType, null);
            returnType = genericReturnType;
            returnClass = resolvedReturnType;
         }
      }
      else {
         returnClass = getClassType(returnType);
      }

      if (getReturnTypeIfCascadingSupportedForIt(returnClass) == null) {
         return null;
      }

      return createNewCascadedInstanceOrUseNonCascadedOneIfAvailable(methodNameAndDesc, returnType);
   }

   @Nonnull
   private Class<?> getClassWithCalledMethod()
   {
      if (mockedClass != null) {
         return mockedClass;
      }

      if (mockedType instanceof Class<?>) {
         return (Class<?>) mockedType;
      }

      return (Class<?>) ((ParameterizedType) mockedType).getRawType();
   }

   @Nonnull
   private Type getGenericReturnType(@Nonnull Class<?> cascadingClass, @Nonnull String methodNameAndDesc)
      throws NoSuchMethodException
   {
      Method cascadingMethod = new RealMethodOrConstructor(cascadingClass, methodNameAndDesc).getMember();
      Type genericReturnType = cascadingMethod.getGenericReturnType();

      if (genericReturnType instanceof TypeVariable<?>) {
         //noinspection ConstantConditions
         genericReturnType = genericReflection.resolveTypeVariable((TypeVariable<?>) genericReturnType);
      }

      return genericReturnType;
   }

   @Nullable
   private static Object createNewCascadedInstanceOrUseNonCascadedOneIfAvailable(
      @Nonnull String methodNameAndDesc, @Nonnull Type mockedReturnType)
   {
      InstanceFactory instanceFactory = TestRun.mockFixture().findInstanceFactory(mockedReturnType);

      if (instanceFactory == null) {
         String methodName = methodNameAndDesc.substring(0, methodNameAndDesc.indexOf('('));
         CascadingTypeRedefinition typeRedefinition = new CascadingTypeRedefinition(methodName, mockedReturnType);
         instanceFactory = typeRedefinition.redefineType();

         if (instanceFactory == null) {
            return null;
         }
      }
      else {
         Object lastInstance = instanceFactory.getLastInstance();

         if (lastInstance != null) {
            return lastInstance;
         }
      }

      Object cascadedInstance = instanceFactory.create();
      instanceFactory.clearLastInstance();
      TestRun.getExecutingTest().addInjectableMock(cascadedInstance);
      return cascadedInstance;
   }

   void discardCascadedMocks() { cascadedTypesAndMocks.clear(); }
}
