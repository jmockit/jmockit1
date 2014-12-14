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
   public static final String OBJECT = "java/lang/Object";

   private static final ClassLoader THIS_CL = ClassLoad.class.getClassLoader();
   private static final Map<String, Class<?>> LOADED_CLASSES = new ConcurrentHashMap<String, Class<?>>();
   private static final Map<String, String> SUPER_CLASSES = new ConcurrentHashMap<String, String>();

   private ClassLoad() {}

   public static void registerLoadedClass(@NotNull Class<?> aClass)
   {
      LOADED_CLASSES.put(aClass.getName(), aClass);
   }

   @NotNull
   public static <T> Class<T> loadByInternalName(@NotNull String internalClassName)
   {
      return loadClass(internalClassName.replace('/', '.'));
   }

   @NotNull
   public static <T> Class<T> loadClass(@NotNull String className)
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

   @NotNull
   public static <T> Class<T> loadClassAtStartup(@NotNull String className)
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

   @Nullable
   public static Class<?> loadClass(@Nullable ClassLoader loader, @NotNull String className)
   {
      try { return Class.forName(className, true, loader); } catch (ClassNotFoundException ignore) { return null; }
   }

   @NotNull
   public static <T> Class<T> loadFromLoader(@Nullable ClassLoader loader, @NotNull String className)
   {
      try {
         //noinspection unchecked
         return (Class<T>) Class.forName(className, true, loader);
      }
      catch (ClassNotFoundException ignore) {
         throw new IllegalArgumentException("No class with name \"" + className + "\" found");
      }
   }

   @Nullable
   public static <T> Class<? extends T> searchTypeInClasspath(@NotNull String typeName)
   {
       return searchTypeInClasspath(typeName, false);
   }

   @Nullable
   public static <T> Class<? extends T> searchTypeInClasspath(@NotNull String typeName, boolean initializeType)
   {
      //noinspection OverlyBroadCatchBlock
      try {
         //noinspection unchecked
         return (Class<? extends T>) Class.forName(typeName, initializeType, THIS_CL);
      }
      catch (Throwable ignore) { return null; }
   }

   public static void addSuperClass(@NotNull String classInternalName, @NotNull String superClassInternalName)
   {
      SUPER_CLASSES.put(classInternalName.intern(), superClassInternalName.intern());
   }

   @NotNull
   public static String getSuperClass(@NotNull String classInternalName)
   {
      String classDesc = classInternalName.intern();
      String superName = SUPER_CLASSES.get(classDesc);

      if (superName == null) {
         Class<?> theClass = loadByInternalName(classDesc);
         Class<?> superClass = theClass.getSuperclass();

         if (superClass != null) {
            superName = superClass.getName().replace('.', '/').intern();
            SUPER_CLASSES.put(classDesc, superName);
         }
      }

      return superName == null ? OBJECT : superName;
   }

   @Nullable
   public static String whichIsSuperClass(@NotNull String internalClassName1, @NotNull String internalClassName2)
   {
      String class1 = actualSuperClass(internalClassName1, internalClassName2);

      if (class1 != null) {
         return class1;
      }

      String class2 = actualSuperClass(internalClassName2, internalClassName1);
      return class2;
   }

   @Nullable
   private static String actualSuperClass(@NotNull String candidateSuperClass, @NotNull String candidateSubclass)
   {
      String subclass = candidateSubclass;

      while (true) {
         String superClass = getSuperClass(subclass);

         if (superClass.equals(OBJECT)) {
            return null;
         }

         if (superClass.equals(candidateSuperClass)) {
            return candidateSuperClass;
         }

         subclass = superClass;
      }
   }

   public static boolean isGeneratedSubclass(@NotNull String className)
   {
      return className.contains("_$$_javassist_") || className.contains("CGLIB$$");
   }
}
