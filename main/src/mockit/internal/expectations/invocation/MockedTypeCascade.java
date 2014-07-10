/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import java.lang.reflect.*;
import java.util.*;

import org.jetbrains.annotations.*;

import mockit.internal.expectations.mocking.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

public final class MockedTypeCascade
{
   private final boolean fromMockField;
   @NotNull private final Type mockedType;
   @NotNull private final Map<String, Type> cascadedTypesAndMocks;

   public MockedTypeCascade(boolean fromMockField, @NotNull Type mockedType)
   {
      this.fromMockField = fromMockField;
      this.mockedType = mockedType;
      cascadedTypesAndMocks = new HashMap<String, Type>(4);
   }

   public boolean isSharedBetweenTests() { return fromMockField; }

   @Nullable
   public static Object getMock(
      @NotNull String mockedTypeDesc, @NotNull String mockedMethodNameAndDesc, @Nullable Object mockInstance,
      @NotNull String returnTypeDesc, @Nullable String genericReturnTypeDesc)
   {
      char typeCode = returnTypeDesc.charAt(0);

      if (typeCode != 'L') {
         return null;
      }

      MockedTypeCascade cascade = TestRun.getExecutingTest().getMockedTypeCascade(mockedTypeDesc, mockInstance);

      if (cascade == null) {
         return null;
      }

      String resolvedReturnTypeDesc = null;

      if (genericReturnTypeDesc != null) {
         resolvedReturnTypeDesc = getGenericReturnType(genericReturnTypeDesc, cascade);
      }

      if (resolvedReturnTypeDesc == null) {
         resolvedReturnTypeDesc = getReturnTypeIfCascadingSupportedForIt(returnTypeDesc);
      }

      if (resolvedReturnTypeDesc == null) {
         return null;
      }

      return cascade.getCascadedMock(mockedMethodNameAndDesc, resolvedReturnTypeDesc);
   }

   @Nullable
   private static String getGenericReturnType(@NotNull String genericReturnTypeDesc, @NotNull MockedTypeCascade cascade)
   {
      Type mockedType = cascade.mockedType;

      if (!(mockedType instanceof ParameterizedType)) {
         mockedType = ((Class<?>) mockedType).getGenericSuperclass();
      }

      if (mockedType instanceof ParameterizedType) {
         return getGenericReturnTypeWithTypeArguments(genericReturnTypeDesc, (ParameterizedType) mockedType);
      }
      else {
         return getReturnTypeIfCascadingSupportedForIt(genericReturnTypeDesc);
      }
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
      TypeVariable<?>[] typeParameters = ((Class<?>) mockedGenericType.getRawType()).getTypeParameters();
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
      String typeName = returnType.getName().replace('.', '/');
      return isTypeSupportedForCascading(typeName) ? typeName : null;
   }

   private static boolean isTypeSupportedForCascading(@NotNull String typeName)
   {
      return !typeName.startsWith("java/lang/") || typeName.contains("/Process") || typeName.endsWith("/Runnable");
   }

   @Nullable
   private static String getReturnTypeIfCascadingSupportedForIt(@NotNull String typeDesc)
   {
      String typeName = getInternalTypeName(typeDesc);
      return isTypeSupportedForCascading(typeName) ? typeName : null;
   }

   @NotNull
   private Object getCascadedMock(@NotNull String methodNameAndDesc, @NotNull String returnTypeInternalName)
   {
      Type returnType = cascadedTypesAndMocks.get(returnTypeInternalName);

      if (returnType == null) {
         returnType = registerIntermediateCascadingType(methodNameAndDesc, returnTypeInternalName);
      }

      return createNewCascadedInstanceOrUseNonCascadedOneIfAvailable(returnType);
   }

   @NotNull
   private Type registerIntermediateCascadingType(@NotNull String methodNameAndDesc, @NotNull String returnTypeDesc)
   {
      Type returnType;

      Class<?> cascadingClass = mockedType instanceof Class<?> ?
         (Class<?>) mockedType : (Class<?>) ((ParameterizedType) mockedType).getRawType();
      Method cascadingMethod = new RealMethodOrConstructor(cascadingClass, methodNameAndDesc).getMember();
      returnType = cascadingMethod.getGenericReturnType();

      if (returnType instanceof TypeVariable<?>) {
         GenericTypeReflection typeReflection = new GenericTypeReflection(cascadingClass, mockedType);
         returnType = typeReflection.resolveReturnType((TypeVariable<?>) returnType);
      }

      cascadedTypesAndMocks.put(returnTypeDesc, returnType);
      TestRun.getExecutingTest().addCascadingType(returnTypeDesc, fromMockField, returnType);
      return returnType;
   }

   @NotNull
   private Object createNewCascadedInstanceOrUseNonCascadedOneIfAvailable(@NotNull Type mockedReturnType)
   {
      InstanceFactory instanceFactory = TestRun.mockFixture().findInstanceFactory(mockedReturnType);

      if (instanceFactory == null) {
         CascadingTypeRedefinition typeRedefinition = new CascadingTypeRedefinition(mockedReturnType);
         instanceFactory = typeRedefinition.redefineType();
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

   public void discardCascadedMocks() { cascadedTypesAndMocks.clear(); }
}
