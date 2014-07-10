/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal;

import java.lang.reflect.Type;

import org.jetbrains.annotations.*;

import mockit.external.asm4.*;
import mockit.internal.util.*;

/**
 * Allows the creation of new implementation classes for interfaces and abstract classes.
 */
public abstract class ImplementationClass<T>
{
   @NotNull private final Type mockedType;
   private byte[] generatedBytecode;

   protected ImplementationClass(@NotNull Type mockedType) { this.mockedType = mockedType; }

   @NotNull public final Class<T> generateNewMockImplementationClassForInterface()
   {
      Class<?> mockedClass = Utilities.getClassType(mockedType);
      ClassReader interfaceReader = ClassFile.createClassFileReader(mockedClass);
      String mockClassName = GeneratedClasses.getNameForGeneratedClass(mockedClass);

      ClassVisitor modifier = createMethodBodyGenerator(interfaceReader, mockClassName);
      interfaceReader.accept(modifier, ClassReader.SKIP_DEBUG);

      generatedBytecode = modifier.toByteArray();

      @SuppressWarnings("unchecked")
      Class<T> implClass = (Class<T>) defineNewClass(mockedClass.getClassLoader(), generatedBytecode, mockClassName);

      return implClass;
   }

   @NotNull
   protected abstract ClassVisitor createMethodBodyGenerator(
      @NotNull ClassReader typeReader, @NotNull String className);

   public final byte[] getGeneratedBytecode() { return generatedBytecode; }

   @NotNull
   public static Class<?> defineNewClass(
      @Nullable ClassLoader parentLoader, @NotNull final byte[] bytecode, @NotNull String className)
   {
      if (parentLoader == null) {
         //noinspection AssignmentToMethodParameter
         parentLoader = ImplementationClass.class.getClassLoader();
      }

      return new ClassLoader(parentLoader) {
         @Override
         protected Class<?> findClass(String name)
         {
            return defineClass(name, bytecode, 0, bytecode.length);
         }
      }.findClass(className);
   }
}
