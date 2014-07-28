package mockit.internal.expectations.injection;

import java.lang.reflect.*;
import java.lang.reflect.Type;
import static java.lang.reflect.Modifier.*;

import mockit.external.asm4.*;
import mockit.internal.*;
import mockit.internal.expectations.mocking.*;
import mockit.internal.state.*;
import mockit.internal.util.*;
import static mockit.external.asm4.ClassReader.*;
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
   private Class<?> generateSubclass(@NotNull Type testedType)
   {
      ClassReader classReader = ClassFile.createReaderOrGetFromCache(declaredTestedClass);
      String subclassName = GeneratedClasses.getNameForGeneratedClass(declaredTestedClass);

      ClassVisitor modifier = new SubclassGenerationModifier(testedType, classReader, subclassName);
      classReader.accept(modifier, SKIP_FRAMES);
      byte[] bytecode = modifier.toByteArray();

      Class<?> generatedSubclass =
         ImplementationClass.defineNewClass(declaredTestedClass.getClassLoader(), bytecode, subclassName);

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
