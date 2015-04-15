/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
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
      Type cascadingType = mockedType;

      if (!(cascadingType instanceof ParameterizedType)) {
         cascadingType = ((Class<?>) cascadingType).getGenericSuperclass();
      }

      if (cascadingType instanceof ParameterizedType) {
         return getGenericReturnTypeWithTypeArguments(genericSignature, (ParameterizedType) cascadingType);
      }

      String returnTypeName = getInternalReturnTypeCodeAndName(genericSignature);
      return isTypeSupportedForCascading(returnTypeName) ? returnTypeName : null;
   }

   @Nonnull
   private static String getInternalTypeName(@Nonnull String typeDesc)
   {
      return typeDesc.substring(1, typeDesc.length() - 1);
   }

   @Nonnull
   private static String getInternalReturnTypeCodeAndName(@Nonnull String genericSignature)
   {
      int p = genericSignature.indexOf(')');
      return genericSignature.substring(p + 1, genericSignature.length() - 1);
   }

   @Nullable
   private static String getGenericReturnTypeWithTypeArguments(
      @Nonnull String genericSignature, @Nonnull ParameterizedType mockedGenericType)
   {
      String typeCodeAndName = getInternalReturnTypeCodeAndName(genericSignature);
      char typeCode = typeCodeAndName.charAt(0);
      String typeName = typeCodeAndName.substring(1);

      if (typeCode == 'L') {
         return isTypeSupportedForCascading(typeName) ? typeName : null;
      }

      TypeVariable<?>[] typeParameters = ((GenericDeclaration) mockedGenericType.getRawType()).getTypeParameters();
      Type[] actualTypeArguments = mockedGenericType.getActualTypeArguments();

      for (int i = 0; i < typeParameters.length; i++) {
         TypeVariable<?> typeParameter = typeParameters[i];

         if (typeName.equals(typeParameter.getName())) {
            Type actualType = actualTypeArguments[i];
            Class<?> actualClass;

            if (actualType instanceof Class<?>) {
               actualClass = (Class<?>) actualType;
            }
            else if (actualType instanceof WildcardType) {
               actualClass = (Class<?>) ((WildcardType) actualType).getUpperBounds()[0];
            }
            else if (actualType instanceof GenericArrayType) {
               Class<?> componentClass = getClassType((GenericArrayType) actualType);
               return getInternalName(componentClass);
            }
            else {
               return null;
            }

            return getReturnTypeIfCascadingSupportedForIt(actualClass);
         }
      }

      return null;
   }

   @Nullable
   private static String getReturnTypeIfCascadingSupportedForIt(@Nonnull Class<?> returnType)
   {
      String typeName = getInternalName(returnType);
      return isTypeSupportedForCascading(typeName) ? typeName : null;
   }

   private static boolean isTypeSupportedForCascading(@Nonnull String typeName)
   {
      if (typeName.contains("/Process") || typeName.endsWith("/Runnable")) {
         return true;
      }

      return
         !typeName.startsWith("java/lang/") &&
         !typeName.startsWith("java/math/") &&
         !typeName.startsWith("java/util/concurrent/atomic/");
   }

   @Nullable
   private static String getReturnTypeIfCascadingSupportedForIt(@Nonnull String typeDesc)
   {
      String typeName = getInternalTypeName(typeDesc);
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
         GenericTypeReflection typeReflection = new GenericTypeReflection(cascadingClass, mockedType);
         genericReturnType = typeReflection.resolveReturnType((TypeVariable<?>) genericReturnType);
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
