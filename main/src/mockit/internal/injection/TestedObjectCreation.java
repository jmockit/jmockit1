/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import java.lang.reflect.*;
import javax.annotation.*;
import static java.lang.reflect.Modifier.*;

import mockit.external.asm.*;
import mockit.internal.classGeneration.*;
import mockit.internal.expectations.mocking.*;
import mockit.internal.injection.constructor.*;
import mockit.internal.injection.full.*;
import mockit.internal.state.*;

public final class TestedObjectCreation
{
   @Nonnull private final InjectionState injectionState;
   @Nullable private final FullInjection fullInjection;
   @Nonnull final TestedClass testedClass;

   TestedObjectCreation(
      @Nonnull InjectionState injectionState, @Nullable FullInjection fullInjection,
      @Nonnull Type declaredType, @Nonnull Class<?> declaredClass)
   {
      this.injectionState = injectionState;
      this.fullInjection = fullInjection;
      Class<?> actualTestedClass = isAbstract(declaredClass.getModifiers()) ?
         generateSubclass(declaredType, declaredClass) : declaredClass;
      testedClass = new TestedClass(declaredType, actualTestedClass);
   }

   @Nonnull
   private static Class<?> generateSubclass(@Nonnull final Type testedType, @Nonnull final Class<?> abstractClass)
   {
      Class<?> generatedSubclass = new ImplementationClass<Object>(abstractClass) {
         @Nonnull @Override
         protected ClassVisitor createMethodBodyGenerator(@Nonnull ClassReader typeReader)
         {
            return new SubclassGenerationModifier(abstractClass, testedType, typeReader, generatedClassName, true);
         }
      }.generateClass();

      TestRun.mockFixture().registerMockedClass(generatedSubclass);
      return generatedSubclass;
   }

   public TestedObjectCreation(
      @Nonnull InjectionState injectionState, @Nullable FullInjection fullInjection,
      @Nonnull Class<?> implementationClass)
   {
      this.injectionState = injectionState;
      this.fullInjection = fullInjection;
      testedClass = new TestedClass(implementationClass, implementationClass);
   }

   @Nonnull
   public Object create()
   {
      ConstructorSearch constructorSearch = new ConstructorSearch(injectionState, testedClass, fullInjection != null);
      Constructor<?> constructor = constructorSearch.findConstructorToUse();

      if (constructor == null) {
         String description = constructorSearch.getDescription();
         throw new IllegalArgumentException(
            "No constructor in tested class that can be satisfied by available tested/injectable values" + description);
      }

      ConstructorInjection constructorInjection = new ConstructorInjection(injectionState, fullInjection, constructor);

      return constructorInjection.instantiate(constructorSearch.parameterProviders, testedClass);
   }
}
