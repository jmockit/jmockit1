/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection.constructor;

import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;

import mockit.asm.types.*;
import mockit.internal.injection.*;
import mockit.internal.injection.full.*;
import mockit.internal.state.*;
import mockit.internal.util.*;
import static mockit.internal.injection.InjectionPoint.*;
import static mockit.internal.injection.InjectionProvider.NULL;
import static mockit.internal.reflection.ConstructorReflection.*;
import static mockit.internal.util.Utilities.*;

public final class ConstructorInjection extends Injector
{
   @Nonnull private final Constructor<?> constructor;

   public ConstructorInjection(
      @Nonnull InjectionState injectionState, @Nullable FullInjection fullInjection, @Nonnull Constructor<?> constructor
   ) {
      super(injectionState, fullInjection);
      ensureThatMemberIsAccessible(constructor);
      this.constructor = constructor;
   }

   @Nullable
   public Object instantiate(@Nonnull List<InjectionProvider> parameterProviders, @Nonnull TestedClass testedClass, boolean required, boolean needToConstruct) {
      Type[] parameterTypes = constructor.getGenericParameterTypes();
      int n = parameterTypes.length;
      List<InjectionProvider> consumedInjectables = n == 0 ? null : injectionState.injectionProviders.saveConsumedInjectionProviders();
      Object[] arguments = n == 0 ? NO_ARGS : new Object[n];
      boolean varArgs = constructor.isVarArgs();

      if (varArgs) {
         n--;
      }

      for (int i = 0; i < n; i++) {
         @Nonnull InjectionProvider parameterProvider = parameterProviders.get(i);
         Object value;

         if (parameterProvider instanceof ConstructorParameter) {
            value = createOrReuseArgumentValue((ConstructorParameter) parameterProvider, required);

            if (value == null && !needToConstruct) {
               return null;
            }
         }
         else {
            value = getArgumentValueToInject(parameterProvider, i);
         }

         if (value != NULL) {
            Type parameterType = parameterTypes[i];
            arguments[i] = wrapInProviderIfNeeded(parameterType, value);
         }
      }

      if (varArgs) {
         Type parameterType = parameterTypes[n];
         arguments[n] = obtainInjectedVarargsArray(parameterType, testedClass);
      }

      if (consumedInjectables != null) {
         injectionState.injectionProviders.restoreConsumedInjectionProviders(consumedInjectables);
      }

      return invokeConstructor(arguments);
   }

   @Nullable
   private Object createOrReuseArgumentValue(@Nonnull ConstructorParameter constructorParameter, boolean required) {
      Object givenValue = constructorParameter.getValue(null);

      if (givenValue != null) {
         return givenValue;
      }

      assert fullInjection != null;

      Class<?> parameterClass = constructorParameter.getClassOfDeclaredType();
      Object newOrReusedValue = null;

      if (FullInjection.isInstantiableType(parameterClass)) {
         Type parameterType = constructorParameter.getDeclaredType();
         KindOfInjectionPoint kindOfInjectionPoint = kindOfInjectionPoint(constructor);
         injectionState.injectionProviders.setTypeOfInjectionPoint(parameterType, kindOfInjectionPoint);
         String qualifiedName = getQualifiedName(constructorParameter.getAnnotations());
         TestedClass nextTestedClass = new TestedClass(parameterType, parameterClass);

         newOrReusedValue = fullInjection.createOrReuseInstance(nextTestedClass, this, constructorParameter, qualifiedName);
      }
      else {
         fullInjection.setInjectionProvider(constructorParameter);
      }

      if (newOrReusedValue == null && required) {
         String parameterName = constructorParameter.getName();
         String message =
            "Missing @Tested or @Injectable" + missingValueDescription(parameterName) +
            "\r\n  when initializing " + fullInjection;
         IllegalStateException injectionFailure = new IllegalStateException(message);
         StackTrace.filterStackTrace(injectionFailure);
         throw injectionFailure;
      }

      return newOrReusedValue;
   }

   @Nonnull
   private Object getArgumentValueToInject(@Nonnull InjectionProvider injectable, int parameterIndex) {
      Object argument = injectionState.getValueToInject(injectable);

      if (argument == null) {
         String classDesc = getClassDesc();
         String constructorDesc = getConstructorDesc();
         String parameterName = ParameterNames.getName(classDesc, constructorDesc, parameterIndex);

         if (parameterName == null) {
            parameterName = injectable.getName();
         }

         throw new IllegalArgumentException("No injectable value available" + missingValueDescription(parameterName));
      }

      return argument;
   }

   @Nonnull
   private String getClassDesc() { return JavaType.getInternalName(constructor.getDeclaringClass()); }

   @Nonnull
   private String getConstructorDesc() {
      return "<init>" + JavaType.getConstructorDescriptor(constructor);
   }

   @Nonnull
   private Object obtainInjectedVarargsArray(@Nonnull Type parameterType, @Nonnull TestedClass testedClass) {
      Type varargsElementType = getTypeOfInjectionPointFromVarargsParameter(parameterType);
      KindOfInjectionPoint kindOfInjectionPoint = kindOfInjectionPoint(constructor);
      InjectionProviders injectionProviders = injectionState.injectionProviders;
      injectionProviders.setTypeOfInjectionPoint(varargsElementType, kindOfInjectionPoint);

      List<Object> varargValues = new ArrayList<>();
      InjectionProvider injectable;

      while ((injectable = injectionProviders.findNextInjectableForInjectionPoint(testedClass)) != null) {
         Object value = injectionState.getValueToInject(injectable);

         if (value != null) {
            value = wrapInProviderIfNeeded(varargsElementType, value);
            varargValues.add(value);
         }
      }

      Object varargArray = newArrayFromList(varargsElementType, varargValues);
      return varargArray;
   }

   @Nonnull
   private static Object newArrayFromList(@Nonnull Type elementType, @Nonnull List<Object> values) {
      Class<?> componentType = getClassType(elementType);
      int elementCount = values.size();
      Object array = Array.newInstance(componentType, elementCount);

      for (int i = 0; i < elementCount; i++) {
         Array.set(array, i, values.get(i));
      }

      return array;
   }

   @Nonnull
   private String missingValueDescription(@Nonnull String name) {
      String classDesc = getClassDesc();
      String constructorDesc = getConstructorDesc();
      String constructorDescription = new MethodFormatter(classDesc, constructorDesc).toString();
      int p = constructorDescription.indexOf('#');
      //noinspection DynamicRegexReplaceableByCompiledPattern
      String friendlyConstructorDesc = constructorDescription.substring(p + 1).replace("java.lang.", "");

      return " for parameter \"" + name + "\" in constructor " + friendlyConstructorDesc;
   }

   @Nonnull
   private Object invokeConstructor(@Nonnull Object[] arguments) {
      TestRun.exitNoMockingZone();

      try {
         return invokeAccessible(constructor, arguments);
      }
      finally {
         TestRun.enterNoMockingZone();
      }
   }
}