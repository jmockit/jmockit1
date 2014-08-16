/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import java.lang.reflect.*;
import java.lang.reflect.Type;
import static java.lang.reflect.Modifier.*;

import mockit.external.asm.*;
import mockit.internal.classGeneration.*;
import mockit.internal.expectations.mocking.*;
import mockit.internal.state.*;
import static mockit.internal.expectations.injection.InjectionPoint.*;

import org.jetbrains.annotations.*;

final class TestedObjectCreation
{
   @NotNull private final InjectionState injectionState;
   @NotNull private final Class<?> declaredTestedClass;
   @NotNull private final Class<?> actualTestedClass;
   boolean constructorIsAnnotated;

   TestedObjectCreation(@NotNull InjectionState injectionState, @NotNull Field testedField)
   {
      this.injectionState = injectionState;
      declaredTestedClass = testedField.getType();
      actualTestedClass =
         isAbstract(declaredTestedClass.getModifiers()) ?
            generateSubclass(testedField.getGenericType()) : declaredTestedClass;
   }

   @NotNull
   private Class<?> generateSubclass(@NotNull final Type testedType)
   {
      Class<?> generatedSubclass = new ImplementationClass<Object>(declaredTestedClass) {
         @NotNull @Override
         protected ClassVisitor createMethodBodyGenerator(@NotNull ClassReader typeReader)
         {
            return new SubclassGenerationModifier(declaredTestedClass, testedType, typeReader, generatedClassName);
         }
      }.generateClass();

      TestRun.mockFixture().registerMockedClass(generatedSubclass);
      return generatedSubclass;
   }

   @NotNull
   Object create()
   {
      ConstructorSearch constructorSearch = new ConstructorSearch(injectionState, declaredTestedClass);
      Constructor<?> constructor =
         constructorSearch.findConstructorAccordingToAccessibilityAndAvailableInjectables(actualTestedClass);

      if (constructor == null) {
         throw new IllegalArgumentException(
            "No constructor in " + declaredTestedClass + " that can be satisfied by available injectables");
      }

      constructorIsAnnotated = INJECT_CLASS != null && constructor.isAnnotationPresent(INJECT_CLASS);

      ConstructorInjection constructorInjection = new ConstructorInjection(injectionState, constructor);
      return constructorInjection.instantiate(constructorSearch.getInjectables());
   }
}
