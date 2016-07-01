/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import javax.annotation.*;

import mockit.external.asm.*;
import mockit.internal.state.*;
import static mockit.external.asm.ClassReader.*;

public final class ClassFile
{
   private static final Map<String, ClassReader> CLASS_FILES = new ConcurrentHashMap<String, ClassReader>();

   private ClassFile() {}

   public static final class NotFoundException extends RuntimeException
   {
      private NotFoundException(@Nonnull String classNameOrDesc)
      {
         super("Unable to find class file for " + classNameOrDesc.replace('/', '.'));
      }
   }

   private static void verifyClassFileFound(@Nullable InputStream classFile, @Nonnull String classNameOrDesc)
   {
      if (classFile == null) {
         throw new NotFoundException(classNameOrDesc);
      }
   }

   @Nonnull
   public static ClassReader createClassFileReader(@Nonnull Class<?> aClass)
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

   @Nonnull
   public static ClassReader createReaderOrGetFromCache(@Nonnull Class<?> aClass)
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

   @Nonnull @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
   private static InputStream readClassFromClasspath(@Nonnull String classDesc)
   {
      String classFileName = classDesc + ".class";
      ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
      InputStream inputStream = null;

      if (contextClassLoader != null) {
         inputStream = contextClassLoader.getResourceAsStream(classFileName);
      }

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
      //noinspection ConstantConditions
      return inputStream;
   }

   @Nonnull
   public static ClassReader createReaderFromLastRedefinitionIfAny(@Nonnull Class<?> aClass)
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

   @Nonnull
   public static ClassReader createClassFileReader(@Nullable ClassLoader loader, @Nonnull String internalClassName)
   {
      byte[] cachedClassfile = CachedClassfiles.getClassfile(loader, internalClassName);

      if (cachedClassfile != null) {
         return new ClassReader(cachedClassfile);
      }

      return readFromFile(internalClassName);
   }

   @Nonnull
   public static ClassReader readFromFile(@Nonnull Class<?> aClass)
   {
      String classDesc = aClass.getName().replace('.', '/');
      return readFromFile(classDesc);
   }

   @Nonnull
   public static ClassReader readFromFile(@Nonnull String classDesc)
   {
      if (classDesc.startsWith("java/") || classDesc.startsWith("javax/")) {
         byte[] classfile = CachedClassfiles.getClassfile(classDesc);

         if (classfile != null) {
            return new ClassReader(classfile);
         }
      }

      InputStream classFile = readClassFromClasspath(classDesc);

      try {
         return new ClassReader(classFile);
      }
      catch (IOException e) {
         throw new RuntimeException("Failed to read class file for " + classDesc.replace('/', '.'), e);
      }
   }

   public static void visitClass(@Nonnull String classDesc, @Nonnull ClassVisitor visitor)
   {
      byte[] classfile = CachedClassfiles.getClassfile(classDesc);
      ClassReader cr = classfile != null ? new ClassReader(classfile) : readFromFile(classDesc);
      cr.accept(visitor, SKIP_DEBUG + SKIP_FRAMES);
   }
}
