/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.classGeneration;

import java.lang.reflect.Type;

import mockit.external.asm.*;
import mockit.internal.*;
import mockit.internal.util.*;
import static mockit.external.asm.ClassReader.*;

import org.jetbrains.annotations.*;

/**
 * Allows the creation of new implementation classes for interfaces and abstract classes.
 */
public abstract class ImplementationClass<T>
{
   @NotNull protected final Class<?> sourceClass;
   @NotNull protected String generatedClassName;
   @Nullable private byte[] generatedBytecode;

   protected ImplementationClass(@NotNull Type mockedType) { this(Utilities.getClassType(mockedType)); }

   protected ImplementationClass(@NotNull Class<?> mockedClass)
   {
      this(mockedClass, GeneratedClasses.getNameForGeneratedClass(mockedClass, null));
   }

   protected ImplementationClass(@NotNull Class<?> sourceClass, @NotNull String desiredClassName)
   {
      this.sourceClass = sourceClass;
      generatedClassName = desiredClassName;
   }

   @NotNull
   public final Class<T> generateClass()
   {
      ClassReader classReader = ClassFile.createReaderOrGetFromCache(sourceClass);

      ClassVisitor modifier = createMethodBodyGenerator(classReader);
      classReader.accept(modifier, SKIP_FRAMES);

      return defineNewClass(modifier);
   }

   @NotNull
   protected abstract ClassVisitor createMethodBodyGenerator(@NotNull ClassReader typeReader);

   @NotNull
   private Class<T> defineNewClass(@NotNull ClassVisitor modifier)
   {
      ClassLoader parentLoader = sourceClass.getClassLoader();

      if (parentLoader == null) {
         parentLoader = ImplementationClass.class.getClassLoader();
      }

      final byte[] modifiedClassfile = modifier.toByteArray();

      @SuppressWarnings("unchecked")
      Class<T> generatedClass = (Class<T>) new ClassLoader(parentLoader) {
         @Override
         protected Class<?> findClass(String name)
         {
            return defineClass(name, modifiedClassfile, 0, modifiedClassfile.length);
         }
      }.findClass(generatedClassName);

      generatedBytecode = modifiedClassfile;
      return generatedClass;
   }

   @Nullable
   public final byte[] getGeneratedBytecode() { return generatedBytecode; }
}
