/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import java.util.*;
import java.util.concurrent.*;

import org.jetbrains.annotations.*;

import mockit.internal.state.*;

public final class ClassLoad
{
   private static final ClassLoader THIS_CL = ClassLoad.class.getClassLoader();
   private static final Map<String, Class<?>> LOADED_CLASSES = new ConcurrentHashMap<String, Class<?>>();

   private ClassLoad() {}

   public static void registerLoadedClass(@NotNull Class<?> aClass)
   {
      LOADED_CLASSES.put(aClass.getName(), aClass);
   }

   @NotNull public static <T> Class<T> loadByInternalName(@NotNull String internalClassName)
   {
      return loadClass(internalClassName.replace('/', '.'));
   }

   @NotNull public static <T> Class<T> loadClass(@NotNull String className)
   {
      @Nullable Class<?> loadedClass = LOADED_CLASSES.get(className);

      if (loadedClass == null) {
         try {
            loadedClass = loadClass(null, className);

            if (loadedClass == null) {
               if (className.startsWith("mockit.")) {
                  loadedClass = loadClass(THIS_CL, className);
               }

               if (loadedClass == null) {
                  Class<?> testClass = TestRun.getCurrentTestClass();
                  loadedClass = testClass == null ? null : loadClass(testClass.getClassLoader(), className);

                  if (loadedClass == null) {
                     ClassLoader contextCL = Thread.currentThread().getContextClassLoader();
                     loadedClass = loadClass(contextCL, className);

                     if (loadedClass == null) {
                        throw new IllegalArgumentException("No class with name \"" + className + "\" found");
                     }
                  }
               }
            }
         }
         catch (LinkageError e) {
            e.printStackTrace();
            throw e;
         }
      }

      //noinspection unchecked
      return (Class<T>) loadedClass;
   }

   @NotNull public static <T> Class<T> loadClassAtStartup(@NotNull String className)
   {
      ClassLoader contextCL = Thread.currentThread().getContextClassLoader();
      Class<?> loadedClass;

      try {
         loadedClass = loadClass(contextCL, className);

         if (loadedClass == null) {
            loadedClass = loadClass(THIS_CL, className);

            if (loadedClass == null) {
               throw new IllegalArgumentException("No class with name \"" + className + "\" found");
            }
         }
      }
      catch (LinkageError e) {
         e.printStackTrace();
         throw e;
      }

      //noinspection unchecked
      return (Class<T>) loadedClass;
   }

   @Nullable public static Class<?> loadClass(@Nullable ClassLoader loader, @NotNull String className)
   {
      try { return Class.forName(className, true, loader); } catch (ClassNotFoundException ignore) { return null; }
   }

   @NotNull public static <T> Class<T> loadFromLoader(@Nullable ClassLoader loader, @NotNull String className)
   {
      try {
         //noinspection unchecked
         return (Class<T>) Class.forName(className, true, loader);
      }
      catch (ClassNotFoundException ignore) {
         throw new IllegalArgumentException("No class with name \"" + className + "\" found");
      }
   }
}
