/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.classGeneration;

import java.lang.reflect.Type;
import javax.annotation.*;

import mockit.external.asm.*;
import mockit.internal.*;
import mockit.internal.util.*;
import static mockit.external.asm.ClassReader.*;

/**
 * Allows the creation of new implementation classes for interfaces and abstract classes.
 */
public abstract class ImplementationClass<T>
{
   @Nonnull protected final Class<?> sourceClass;
   @Nonnull protected String generatedClassName;
   @Nullable private byte[] generatedBytecode;

   protected ImplementationClass(@Nonnull Type mockedType) { this(Utilities.getClassType(mockedType)); }

   protected ImplementationClass(@Nonnull Class<?> mockedClass)
   {
      this(mockedClass, GeneratedClasses.getNameForGeneratedClass(mockedClass, null));
   }

   protected ImplementationClass(@Nonnull Class<?> sourceClass, @Nonnull String desiredClassName)
   {
      this.sourceClass = sourceClass;
      generatedClassName = desiredClassName;
   }

   @Nonnull
   public final Class<T> generateClass()
   {
      ClassReader classReader = ClassFile.createReaderOrGetFromCache(sourceClass);

      ClassVisitor modifier = createMethodBodyGenerator(classReader);
      classReader.accept(modifier, SKIP_FRAMES);

      return defineNewClass(modifier);
   }

   @Nonnull
   protected abstract ClassVisitor createMethodBodyGenerator(@Nonnull ClassReader typeReader);

   @Nonnull
   private Class<T> defineNewClass(@Nonnull ClassVisitor modifier)
   {
      final ClassLoader parentLoader = ClassLoad.getClassLoaderWithAccess(sourceClass);
      final byte[] modifiedClassfile = modifier.toByteArray();

      try {
         @SuppressWarnings("unchecked")
         Class<T> generatedClass = (Class<T>) new ClassLoader(parentLoader) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException
            {
               if (!name.equals(generatedClassName)) {
                  return parentLoader.loadClass(name);
               }

               return defineClass(name, modifiedClassfile, 0, modifiedClassfile.length);
            }
         }.findClass(generatedClassName);

         generatedBytecode = modifiedClassfile;
         return generatedClass;
      }
      catch (ClassNotFoundException e) {
         throw new RuntimeException("Unable to define class: " + generatedClassName, e);
      }
   }

   @Nullable
   public final byte[] getGeneratedBytecode() { return generatedBytecode; }
}
