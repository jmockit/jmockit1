/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.state;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.jetbrains.annotations.*;

import mockit.internal.expectations.mocking.*;
import mockit.internal.util.*;
import static mockit.external.asm.Type.getInternalName;
import static mockit.internal.util.Utilities.getClassType;

public final class MockedTypeCascade
{
   @NotNull private static final CascadingTypes CASCADING_TYPES = TestRun.getExecutingTest().getCascadingTypes();

   final boolean fromMockField;
   @NotNull private final Type mockedType;
   @Nullable Class<?> mockedClass;
   @Nullable private final Object cascadedInstance;
   @NotNull private final Map<String, Type> cascadedTypesAndMocks;

   MockedTypeCascade(boolean fromMockField, @NotNull Type mockedType, @Nullable Object cascadedInstance)
   {
      this.fromMockField = fromMockField;
      this.mockedType = mockedType;
      this.cascadedInstance = cascadedInstance;
      cascadedTypesAndMocks = new ConcurrentHashMap<String, Type>(4);
   }

   @Nullable
   public static Object getMock(
      @NotNull String mockedTypeDesc, @NotNull String mockedMethodNameAndDesc, @Nullable Object mockInstance,
      @NotNull String returnTypeDesc, @Nullable String genericReturnTypeDesc)
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

      if (genericReturnTypeDesc != null) {
         resolvedReturnTypeDesc = cascade.getGenericReturnType(genericReturnTypeDesc);
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
   private String getGenericReturnType(@NotNull String genericReturnTypeDesc)
   {
      Type cascadingType = mockedType;

      if (!(cascadingType instanceof ParameterizedType)) {
         cascadingType = ((Class<?>) cascadingType).getGenericSuperclass();
      }

      if (cascadingType instanceof ParameterizedType) {
         return getGenericReturnTypeWithTypeArguments(genericReturnTypeDesc, (ParameterizedType) cascadingType);
      }

      return getReturnTypeIfCascadingSupportedForIt(genericReturnTypeDesc);
   }

   @NotNull
   private static String getInternalTypeName(@NotNull String typeDesc)
   {
      int p = typeDesc.indexOf('(');
      return typeDesc.substring(p + 2, typeDesc.length() - 1);
   }

   @Nullable
   private static String getGenericReturnTypeWithTypeArguments(
      @NotNull String genericReturnTypeDesc, @NotNull ParameterizedType mockedGenericType)
   {
      String typeName = getInternalTypeName(genericReturnTypeDesc);
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
   private static String getReturnTypeIfCascadingSupportedForIt(@NotNull Class<?> returnType)
   {
      String typeName = getInternalName(returnType);
      return isTypeSupportedForCascading(typeName) ? typeName : null;
   }

   private static boolean isTypeSupportedForCascading(@NotNull String typeName)
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
   private static String getReturnTypeIfCascadingSupportedForIt(@NotNull String typeDesc)
   {
      String typeName = getInternalTypeName(typeDesc);
      return isTypeSupportedForCascading(typeName) ? typeName : null;
   }

   @Nullable
   private Object getCascadedInstance(@NotNull String methodNameAndDesc, @NotNull String returnTypeInternalName)
   {
      Type returnType = cascadedTypesAndMocks.get(returnTypeInternalName);
      Class<?> returnClass;

      if (returnType == null) {
         Class<?> cascadingClass = getClassWithCalledMethod();
         Type genericReturnType = getGenericReturnType(cascadingClass, methodNameAndDesc);
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

   @NotNull
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

   @NotNull
   private Type getGenericReturnType(@NotNull Class<?> cascadingClass, @NotNull String methodNameAndDesc)
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
      @NotNull String methodNameAndDesc, @NotNull Type mockedReturnType)
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
