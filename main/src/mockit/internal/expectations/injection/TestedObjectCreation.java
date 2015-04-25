/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import java.lang.reflect.*;
import java.lang.reflect.Type;
import javax.annotation.*;
import static java.lang.reflect.Modifier.*;

import mockit.external.asm.*;
import mockit.internal.classGeneration.*;
import mockit.internal.expectations.mocking.*;
import mockit.internal.state.*;
import static mockit.internal.expectations.injection.InjectionPoint.*;

final class TestedObjectCreation
{
   @Nonnull private final InjectionState injectionState;
   @Nonnull private final Class<?> declaredTestedClass;
   @Nonnull private final Class<?> actualTestedClass;
   boolean constructorIsAnnotated;

   TestedObjectCreation(@Nonnull InjectionState injectionState, @Nonnull Field testedField)
   {
      this.injectionState = injectionState;
      declaredTestedClass = testedField.getType();
      actualTestedClass =
         isAbstract(declaredTestedClass.getModifiers()) ?
            generateSubclass(testedField.getGenericType()) : declaredTestedClass;
   }

   @Nonnull
   private Class<?> generateSubclass(@Nonnull final Type testedType)
   {
      Class<?> generatedSubclass = new ImplementationClass<Object>(declaredTestedClass) {
         @Nonnull @Override
         protected ClassVisitor createMethodBodyGenerator(@Nonnull ClassReader typeReader)
         {
            return new SubclassGenerationModifier(declaredTestedClass, testedType, typeReader, generatedClassName);
         }
      }.generateClass();

      TestRun.mockFixture().registerMockedClass(generatedSubclass);
      return generatedSubclass;
   }

   @Nonnull
   Object create()
   {
      ConstructorSearch constructorSearch = new ConstructorSearch(injectionState, actualTestedClass);
      Constructor<?> constructor = constructorSearch.findConstructorAccordingToAccessibilityAndAvailableInjectables();

      if (constructor == null) {
         throw new IllegalArgumentException(
            "No constructor in tested class that can be satisfied by available injectables" + constructorSearch);
      }

      constructorIsAnnotated = isAnnotated(constructor) != KindOfInjectionPoint.NotAnnotated;

      ConstructorInjection constructorInjection = new ConstructorInjection(injectionState, constructor);
      return constructorInjection.instantiate(constructorSearch.getInjectables());
   }
}
