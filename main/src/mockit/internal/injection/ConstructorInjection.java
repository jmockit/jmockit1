/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;

import mockit.internal.state.*;
import mockit.internal.util.*;
import static mockit.internal.injection.InjectionPoint.*;
import static mockit.internal.util.ConstructorReflection.*;
import static mockit.internal.util.Utilities.*;

final class ConstructorInjection implements Injector
{
   @Nonnull private final TestedClass testedClass;
   @Nonnull private final InjectionState injectionState;
   @Nullable private final FullInjection fullInjection;
   @Nonnull private final Constructor<?> constructor;

   ConstructorInjection(
      @Nonnull TestedClass testedClass, @Nonnull InjectionState injectionState, @Nullable FullInjection fullInjection,
      @Nonnull Constructor<?> constructor)
   {
      this.testedClass = testedClass;
      this.injectionState = injectionState;
      this.fullInjection = fullInjection;
      this.constructor = constructor;
   }

   @Nonnull
   Object instantiate(@Nonnull List<InjectionPointProvider> parameterProviders)
   {
      Type[] parameterTypes = constructor.getGenericParameterTypes();
      int n = parameterTypes.length;
      Object[] arguments = n == 0 ? NO_ARGS : new Object[n];
      boolean varArgs = constructor.isVarArgs();

      if (varArgs) {
         n--;
      }

      for (int i = 0; i < n; i++) {
         Type parameterType = parameterTypes[i];
         InjectionPointProvider parameterProvider = parameterProviders.get(i);
         Object value;

         if (parameterProvider instanceof ConstructorParameter) {
            assert fullInjection != null;
            injectionState.setTypeOfInjectionPoint(parameterProvider.getDeclaredType());
            String qualifiedName = getQualifiedName(parameterProvider.getAnnotations());
            value = fullInjection.newInstance(testedClass, this, parameterProvider, qualifiedName);

            if (value == null) {
               throw new IllegalArgumentException(
                  "Missing @Tested object for constructor parameter: " +
                  parameterType + ' ' + parameterProvider.getName());
            }
         }
         else {
            value = getArgumentValueToInject(parameterProvider);
         }

         arguments[i] = wrapInProviderIfNeeded(parameterType, value);
      }

      if (varArgs) {
         Type parameterType = parameterTypes[n];
         arguments[n] = obtainInjectedVarargsArray(parameterType);
      }

      TestRun.exitNoMockingZone();

      try {
         return invoke(constructor, arguments);
      }
      finally {
         TestRun.enterNoMockingZone();
      }
   }

   @Nonnull
   private Object obtainInjectedVarargsArray(@Nonnull Type parameterType)
   {
      Type varargsElementType = getTypeOfInjectionPointFromVarargsParameter(parameterType);
      injectionState.setTypeOfInjectionPoint(varargsElementType);

      List<Object> varargValues = new ArrayList<Object>();
      InjectionPointProvider injectable;

      while ((injectable = injectionState.findNextInjectableForInjectionPoint()) != null) {
         Object value = injectionState.getValueToInject(injectable);

         if (value != null) {
            value = wrapInProviderIfNeeded(varargsElementType, value);
            varargValues.add(value);
         }
      }

      int elementCount = varargValues.size();
      Object varargArray = Array.newInstance(getClassType(varargsElementType), elementCount);

      for (int i = 0; i < elementCount; i++) {
         Array.set(varargArray, i, varargValues.get(i));
      }

      return varargArray;
   }

   @Nonnull
   private Object getArgumentValueToInject(@Nonnull InjectionPointProvider injectable)
   {
      Object argument = injectionState.getValueToInject(injectable);

      if (argument == null) {
         throw new IllegalArgumentException(
            "No injectable value available" + missingInjectableDescription(injectable.getName()));
      }

      return argument;
   }

   @Nonnull
   private String missingInjectableDescription(@Nonnull String name)
   {
      String classDesc = mockit.external.asm.Type.getInternalName(constructor.getDeclaringClass());
      String constructorDesc = "<init>" + mockit.external.asm.Type.getConstructorDescriptor(constructor);
      String constructorDescription = new MethodFormatter(classDesc, constructorDesc).toString();

      return " for parameter \"" + name + "\" in constructor " + constructorDescription.replace("java.lang.", "");
   }

   @Override
   public void fillOutDependenciesRecursively(@Nonnull Object dependency)
   {
      // TODO: implement/redesign
   }
}
