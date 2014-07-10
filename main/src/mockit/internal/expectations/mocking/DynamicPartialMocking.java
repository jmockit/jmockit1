/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.reflect.*;
import java.util.*;

import mockit.external.asm4.*;
import mockit.internal.*;
import mockit.internal.state.*;
import mockit.internal.util.*;
import static mockit.internal.util.Utilities.*;

import org.jetbrains.annotations.*;

public final class DynamicPartialMocking
{
   @NotNull public final List<Object> targetInstances;
   @NotNull private final Map<Class<?>, byte[]> modifiedClassfiles;
   private final boolean nonStrict;

   public DynamicPartialMocking(boolean nonStrict)
   {
      targetInstances = new ArrayList<Object>(2);
      modifiedClassfiles = new HashMap<Class<?>, byte[]>();
      this.nonStrict = nonStrict;
   }

   public void redefineTypes(@NotNull Object[] classesOrInstancesToBePartiallyMocked)
   {
      for (Object classOrInstance : classesOrInstancesToBePartiallyMocked) {
         generateModifiedClassfileForTargetType(classOrInstance);
      }

      new RedefinitionEngine().redefineMethods(modifiedClassfiles);
      modifiedClassfiles.clear();
   }

   private void generateModifiedClassfileForTargetType(@NotNull Object classOrInstance)
   {
      Class<?> targetClass;

      if (classOrInstance instanceof Class) {
         targetClass = (Class<?>) classOrInstance;
         validateTargetClassType(targetClass);
         registerAsMocked(targetClass);
         ensureThatClassIsInitialized(targetClass);
         generateModifiedClassfilesForClassAndItsSuperClasses(targetClass, false);
      }
      else {
         targetClass = GeneratedClasses.getMockedClass(classOrInstance);
         validateTargetClassType(targetClass);
         registerAsMocked(classOrInstance);
         generateModifiedClassfilesForClassAndItsSuperClasses(targetClass, true);
         targetInstances.add(classOrInstance);
      }

      TestRun.mockFixture().registerMockedClass(targetClass);
   }

   private void validateTargetClassType(@NotNull Class<?> targetClass)
   {
      if (
         targetClass.isInterface() || targetClass.isAnnotation() || targetClass.isArray() ||
         targetClass.isPrimitive() || AutoBoxing.isWrapperOfPrimitiveType(targetClass) ||
         GeneratedClasses.isGeneratedImplementationClass(targetClass)
      ) {
         throw new IllegalArgumentException("Invalid type for dynamic mocking: " + targetClass);
      }
      else if (!modifiedClassfiles.containsKey(targetClass) && TestRun.mockFixture().isMockedClass(targetClass)) {
         throw new IllegalArgumentException("Class is already mocked: " + targetClass);
      }
   }

   private void registerAsMocked(@NotNull Class<?> mockedClass)
   {
      if (nonStrict) {
         ExecutingTest executingTest = TestRun.getExecutingTest();
         Class<?> classToRegister = mockedClass;

         do {
            executingTest.registerAsNonStrictlyMocked(classToRegister);
            classToRegister = classToRegister.getSuperclass();
         }
         while (classToRegister != null && classToRegister != Object.class && classToRegister != Proxy.class);
      }
   }

   private void registerAsMocked(@NotNull Object mock)
   {
      if (nonStrict) {
         TestRun.getExecutingTest().registerAsNonStrictlyMocked(mock);
      }
   }

   private void generateModifiedClassfilesForClassAndItsSuperClasses(@NotNull Class<?> realClass, boolean methodsOnly)
   {
      generatedModifiedClassfile(realClass, methodsOnly);
      Class<?> superClass = realClass.getSuperclass();

      if (superClass != null && superClass != Object.class && superClass != Proxy.class) {
         generateModifiedClassfilesForClassAndItsSuperClasses(superClass, methodsOnly);
      }
   }

   private void generatedModifiedClassfile(@NotNull Class<?> realClass, boolean methodsOnly)
   {
      ClassReader classReader = ClassFile.createReaderOrGetFromCache(realClass);

      ExpectationsModifier modifier = new ExpectationsModifier(realClass.getClassLoader(), classReader, null);
      modifier.useDynamicMocking(methodsOnly);

      classReader.accept(modifier, 0);
      byte[] modifiedClass = modifier.toByteArray();

      modifiedClassfiles.put(realClass, modifiedClass);
   }
}
