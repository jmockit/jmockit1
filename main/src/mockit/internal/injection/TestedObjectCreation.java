/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import java.lang.reflect.*;
import java.lang.reflect.Type;
import javax.annotation.*;
import static java.lang.reflect.Modifier.*;

import mockit.external.asm.*;
import mockit.internal.classGeneration.*;
import mockit.internal.expectations.mocking.*;
import mockit.internal.state.*;

final class TestedObjectCreation
{
   @Nonnull private final InjectionState injectionState;
   @Nullable private final FullInjection fullInjection;
   @Nonnull private final Class<?> actualTestedClass;
   @Nonnull final TestedClass testedClass;

   TestedObjectCreation(
      @Nonnull InjectionState injectionState, @Nullable FullInjection fullInjection, @Nonnull Field testedField)
   {
      this.injectionState = injectionState;
      this.fullInjection = fullInjection;
      Class<?> declaredTestedClass = testedField.getType();
      Type declaredType = testedField.getGenericType();
      actualTestedClass = isAbstract(declaredTestedClass.getModifiers()) ?
         generateSubclass(declaredType, declaredTestedClass) : declaredTestedClass;
      testedClass = new TestedClass(declaredType, declaredTestedClass);
   }

   @Nonnull
   private Class<?> generateSubclass(@Nonnull final Type testedType, @Nonnull final Class<?> abstractClass)
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

   TestedObjectCreation(
      @Nonnull InjectionState injectionState, @Nullable FullInjection fullInjection,
      @Nonnull Class<?> implementationClass)
   {
      this.injectionState = injectionState;
      this.fullInjection = fullInjection;
      actualTestedClass = implementationClass;
      testedClass = new TestedClass(implementationClass, implementationClass);
   }

   @Nonnull
   Object create()
   {
      ConstructorSearch constructorSearch =
         new ConstructorSearch(injectionState, actualTestedClass, fullInjection != null);
      Constructor<?> constructor = constructorSearch.findConstructorToUse();

      if (constructor == null) {
         String description = constructorSearch.getDescription();
         throw new IllegalArgumentException(
            "No constructor in tested class that can be satisfied by available injectables" + description);
      }

      ConstructorInjection constructorInjection =
         new ConstructorInjection(testedClass, injectionState, fullInjection, constructor);

      return constructorInjection.instantiate(constructorSearch.parameterProviders);
   }
}
