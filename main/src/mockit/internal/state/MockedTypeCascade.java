/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.state;

import java.lang.annotation.*;
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
   @Nullable private GenericTypeReflection genericReflection;
   @Nonnull private final Map<String, Type> cascadedTypesAndMocks;

   MockedTypeCascade(boolean fromMockField, @Nonnull Type mockedType)
   {
      this.fromMockField = fromMockField;
      this.mockedType = mockedType;
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

      return cascade.getCascadedInstance(mockedMethodNameAndDesc, resolvedReturnTypeDesc, mockInstance);
   }

   @Nullable
   private String getGenericReturnType(@Nonnull String genericSignature)
   {
      String returnTypeDesc = getGenericReflection().resolveReturnType(genericSignature);

      if (returnTypeDesc.charAt(0) == '[') {
         return returnTypeDesc;
      }

      String returnTypeName = returnTypeDesc.substring(1, returnTypeDesc.length() - 1);
      return isTypeSupportedForCascading(returnTypeName) ? returnTypeName : null;
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
      for (Annotation annotation : returnType.getAnnotations()) {
         if (annotation.annotationType().getName().startsWith("javax.persistence.")) {
            return null;
         }
      }

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
         (!typeName.startsWith("java/util/") ||
           typeName.endsWith("/Date") || typeName.endsWith("/Callable") || typeName.endsWith("Future") ||
           typeName.contains("logging"));
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
         CASCADING_TYPES.add(returnTypeInternalName, false, returnClass);
      }

      return createNewCascadedInstanceOrUseNonCascadedOneIfAvailable(methodNameAndDesc, returnClass);
   }

   @Nullable
   private Object getCascadedInstance(
      @Nonnull String methodNameAndDesc, @Nonnull String returnTypeInternalName, @Nullable Object mockInstance)
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
            if (mockInstance != null) {
               return mockInstance;
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
            CASCADING_TYPES.add(returnTypeInternalName, false, genericReturnType);
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
         genericReturnType = getGenericReflection().resolveTypeVariable((TypeVariable<?>) genericReturnType);
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
