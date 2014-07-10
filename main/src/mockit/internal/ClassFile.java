/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.jetbrains.annotations.*;

import mockit.external.asm4.*;
import mockit.internal.state.*;

public final class ClassFile
{
   private static final Map<String, ClassReader> CLASS_FILES = new ConcurrentHashMap<String, ClassReader>();

   private ClassFile() {}

   public static final class NotFoundException extends RuntimeException
   {
      private NotFoundException(@NotNull String classNameOrDesc)
      {
         super("Unable to find class file for " + classNameOrDesc.replace('/', '.'));
      }
   }

   @Contract("null, _ -> fail")
   private static void verifyClassFileFound(@Nullable InputStream classFile, @NotNull String classNameOrDesc)
   {
      if (classFile == null) {
         throw new NotFoundException(classNameOrDesc);
      }
   }

   @NotNull public static ClassReader createClassFileReader(@NotNull Class<?> aClass)
   {
      byte[] cachedClassfile = CachedClassfiles.getClassfile(aClass);

      if (cachedClassfile != null) {
         return new ClassReader(cachedClassfile);
      }

      String className = aClass.getName();
      InputStream classFile = aClass.getResourceAsStream('/' + className.replace('.', '/') + ".class");
      verifyClassFileFound(classFile, className);

      try {
         return new ClassReader(classFile);
      }
      catch (IOException e) {
         throw new RuntimeException("Failed to read class file for " + className, e);
      }
   }

   @NotNull public static ClassReader createReaderOrGetFromCache(@NotNull Class<?> aClass)
   {
      byte[] cachedClassfile = CachedClassfiles.getClassfile(aClass);

      if (cachedClassfile != null) {
         return new ClassReader(cachedClassfile);
      }

      String classDesc = aClass.getName().replace('.', '/');
      ClassReader reader = CLASS_FILES.get(classDesc);

      if (reader == null) {
         reader = readFromFile(classDesc);
         CLASS_FILES.put(classDesc, reader);
      }

      return reader;
   }

   @NotNull private static InputStream readClassFromClasspath(@NotNull String classDesc)
   {
      String classFileName = classDesc + ".class";
      ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
      InputStream inputStream = contextClassLoader.getResourceAsStream(classFileName);

      if (inputStream == null) {
         ClassLoader thisClassLoader = ClassFile.class.getClassLoader();

         if (thisClassLoader != contextClassLoader) {
            inputStream = thisClassLoader.getResourceAsStream(classFileName);

            if (inputStream == null) {
               Class<?> testClass = TestRun.getCurrentTestClass();

               if (testClass != null) {
                  inputStream = testClass.getClassLoader().getResourceAsStream(classFileName);
               }
            }
         }
      }

      verifyClassFileFound(inputStream, classDesc);
      return inputStream;
   }

   @NotNull public static ClassReader createReaderFromLastRedefinitionIfAny(@NotNull Class<?> aClass)
   {
      byte[] classfile = TestRun.mockFixture().getRedefinedClassfile(aClass);

      if (classfile == null) {
         classfile = CachedClassfiles.getClassfile(aClass);
      }

      if (classfile != null) {
         return new ClassReader(classfile);
      }

      String classDesc = aClass.getName().replace('.', '/');
      ClassReader reader = readFromFile(classDesc);

      CLASS_FILES.put(classDesc, reader);
      return reader;
   }

   @NotNull
   public static ClassReader createClassFileReader(@Nullable ClassLoader loader, @NotNull String internalClassName)
   {
      byte[] cachedClassfile = CachedClassfiles.getClassfile(loader, internalClassName);

      if (cachedClassfile != null) {
         return new ClassReader(cachedClassfile);
      }

      return readFromFile(internalClassName);
   }

   @NotNull public static ClassReader readFromFile(@NotNull Class<?> aClass)
   {
      String classDesc = aClass.getName().replace('.', '/');
      return readFromFile(classDesc);
   }

   @NotNull public static ClassReader readFromFile(@NotNull String classDesc)
   {
      InputStream classFile = readClassFromClasspath(classDesc);

      try {
         return new ClassReader(classFile);
      }
      catch (IOException e) {
         throw new RuntimeException("Failed to read class file for " + classDesc.replace('/', '.'), e);
      }
   }

   public static void visitClass(@NotNull String classDesc, @NotNull ClassVisitor visitor)
   {
      InputStream classFile = readClassFromClasspath(classDesc);

      try {
         ClassReader cr = new ClassReader(classFile);
         cr.accept(visitor, ClassReader.SKIP_DEBUG);
      }
      catch (IOException e) {
         throw new RuntimeException(e);
      }
   }
}
