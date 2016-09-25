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

final class ConstructorInjection extends Injector
{
   @Nonnull private final Constructor<?> constructor;

   ConstructorInjection(
      @Nonnull TestedClass testedClass, @Nonnull InjectionState injectionState, @Nullable FullInjection fullInjection,
      @Nonnull Constructor<?> constructor)
   {
      super(testedClass, injectionState, fullInjection);
      this.constructor = constructor;
   }

   @Nonnull
   Object instantiate(@Nonnull List<InjectionPointProvider> parameterProviders)
   {
      Type[] parameterTypes = constructor.getGenericParameterTypes();
      int n = parameterTypes.length;
      List<InjectionPointProvider> consumedInjectables = n == 0 ? null : injectionState.saveConsumedInjectables();
      Object[] arguments = n == 0 ? NO_ARGS : new Object[n];
      boolean varArgs = constructor.isVarArgs();

      if (varArgs) {
         n--;
      }

      for (int i = 0; i < n; i++) {
         InjectionPointProvider parameterProvider = parameterProviders.get(i);
         Object value;

         if (parameterProvider instanceof ConstructorParameter) {
            value = createOrReuseArgumentValue((ConstructorParameter) parameterProvider);
         }
         else {
            value = getArgumentValueToInject(parameterProvider);
         }

         Type parameterType = parameterTypes[i];
         arguments[i] = wrapInProviderIfNeeded(parameterType, value);
      }

      if (varArgs) {
         Type parameterType = parameterTypes[n];
         arguments[n] = obtainInjectedVarargsArray(parameterType);
      }

      if (consumedInjectables != null) {
         injectionState.restoreConsumedInjectables(consumedInjectables);
      }

      return invokeConstructor(arguments);
   }

   @Nonnull
   private Object createOrReuseArgumentValue(@Nonnull ConstructorParameter constructorParameter)
   {
      injectionState.setTypeOfInjectionPoint(constructorParameter.getDeclaredType());
      String qualifiedName = getQualifiedName(constructorParameter.getAnnotations());

      assert fullInjection != null;
      Object value = fullInjection.createOrReuseInstance(this, constructorParameter, qualifiedName);

      if (value == null) {
         String parameterName = constructorParameter.getName();
         throw new IllegalStateException("Missing @Tested or @Injectable" + missingValueDescription(parameterName));
      }

      return value;
   }

   @Nonnull
   private Object getArgumentValueToInject(@Nonnull InjectionPointProvider injectable)
   {
      Object argument = injectionState.getValueToInject(injectable);

      if (argument == null) {
         String parameterName = injectable.getName();
         throw new IllegalArgumentException("No injectable value available" + missingValueDescription(parameterName));
      }

      return argument;
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

      Object varargArray = newArrayFromList(varargsElementType, varargValues);
      return varargArray;
   }

   @Nonnull
   private static Object newArrayFromList(@Nonnull Type elementType, @Nonnull List<Object> values)
   {
      Class<?> componentType = getClassType(elementType);
      int elementCount = values.size();
      Object array = Array.newInstance(componentType, elementCount);

      for (int i = 0; i < elementCount; i++) {
         Array.set(array, i, values.get(i));
      }

      return array;
   }

   @Nonnull
   private String missingValueDescription(@Nonnull String name)
   {
      String classDesc = mockit.external.asm.Type.getInternalName(constructor.getDeclaringClass());
      String constructorDesc = "<init>" + mockit.external.asm.Type.getConstructorDescriptor(constructor);
      String constructorDescription = new MethodFormatter(classDesc, constructorDesc).toString();

      return " for parameter \"" + name + "\" in constructor " + constructorDescription.replace("java.lang.", "");
   }

   @Nonnull
   private Object invokeConstructor(@Nonnull Object[] arguments)
   {
      TestRun.exitNoMockingZone();

      try {
         return invoke(constructor, arguments);
      }
      finally {
         TestRun.enterNoMockingZone();
      }
   }

   @Override
   public void fillOutDependenciesRecursively(@Nonnull Object dependency)
   {
      // TODO: implement/redesign
   }
}
