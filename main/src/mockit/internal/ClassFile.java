/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import javax.annotation.*;

import mockit.asm.*;
import mockit.internal.state.*;

public final class ClassFile
{
   private static final Map<String, ClassReader> CLASS_FILES = new ConcurrentHashMap<>();

   private ClassFile() {}

   public static final class NotFoundException extends RuntimeException {
      private NotFoundException(@Nonnull String classNameOrDesc) {
         super("Unable to find class file for " + classNameOrDesc.replace('/', '.'));
      }
   }

   private static void verifyClassFileFound(@Nullable InputStream classFile, @Nonnull String classNameOrDesc) {
      if (classFile == null) {
         throw new NotFoundException(classNameOrDesc);
      }
   }

   @Nullable
   public static ClassReader createClassReader(@Nonnull ClassLoader cl, @Nonnull String internalClassName) {
      String classFileName = internalClassName + ".class";
      InputStream classFile = cl.getResourceAsStream(classFileName);

      if (classFile != null) { // ignore the class if the ".class" file wasn't located
         try {
            byte[] bytecode = readClass(classFile);
            return new ClassReader(bytecode);
         }
         catch (IOException ignore) {}
      }

      return null;
   }

   @Nonnull
   private static byte[] readClass(@Nonnull InputStream is) throws IOException {
      try {
         byte[] bytecode = new byte[is.available()];
         int len = 0;

         while (true) {
            int n = is.read(bytecode, len, bytecode.length - len);

            if (n == -1) {
               if (len < bytecode.length) {
                  byte[] truncatedCopy = new byte[len];
                  System.arraycopy(bytecode, 0, truncatedCopy, 0, len);
                  bytecode = truncatedCopy;
               }

               return bytecode;
            }

            len += n;

            if (len == bytecode.length) {
               int last = is.read();

               if (last < 0) {
                  return bytecode;
               }

               byte[] lengthenedCopy = new byte[bytecode.length + 1000];
               System.arraycopy(bytecode, 0, lengthenedCopy, 0, len);
               //noinspection NumericCastThatLosesPrecision
               lengthenedCopy[len++] = (byte) last;
               bytecode = lengthenedCopy;
            }
         }
      }
      finally {
         is.close();
      }
   }

   @Nonnull
   public static ClassReader createReaderOrGetFromCache(@Nonnull Class<?> aClass) {
      byte[] cachedClassfile = CachedClassfiles.getClassfile(aClass);

      if (cachedClassfile != null) {
         return new ClassReader(cachedClassfile);
      }

      String classDesc = aClass.getName().replace('.', '/');
      ClassReader reader = CLASS_FILES.get(classDesc);

      if (reader == null) {
         reader = readFromFileSavingInCache(classDesc);
      }

      return reader;
   }

   @Nonnull
   private static ClassReader readFromFileSavingInCache(@Nonnull String classDesc) {
      byte[] classfileBytes = readBytesFromClassFile(classDesc);
      ClassReader cr = new ClassReader(classfileBytes);
      CLASS_FILES.put(classDesc, cr);
      return cr;
   }

   @Nonnull
   public static ClassReader createReaderFromLastRedefinitionIfAny(@Nonnull Class<?> aClass) {
      byte[] classfile = TestRun.mockFixture().getRedefinedClassfile(aClass);

      if (classfile == null) {
         classfile = CachedClassfiles.getClassfile(aClass);
      }

      if (classfile != null) {
         return new ClassReader(classfile);
      }

      String classDesc = aClass.getName().replace('.', '/');
      ClassReader reader = readFromFileSavingInCache(classDesc);
      return reader;
   }

   @Nonnull
   public static byte[] getClassFile(@Nonnull String internalClassName) {
      byte[] classfileBytes = CachedClassfiles.getClassfile(internalClassName);

      if (classfileBytes == null) {
         classfileBytes = readBytesFromClassFile(internalClassName);
      }

      return classfileBytes;
   }

   @Nonnull
   public static byte[] getClassFile(@Nullable ClassLoader loader, @Nonnull String internalClassName) {
      byte[] classfileBytes = CachedClassfiles.getClassfile(loader, internalClassName);

      if (classfileBytes == null) {
         classfileBytes = readBytesFromClassFile(internalClassName);
      }

      return classfileBytes;
   }

   @Nonnull
   public static byte[] getClassFile(@Nonnull Class<?> aClass) {
      byte[] classfileBytes = CachedClassfiles.getClassfile(aClass);

      if (classfileBytes == null) {
         classfileBytes = readBytesFromClassFile(aClass);
      }

      return classfileBytes;
   }

   @Nonnull
   public static byte[] readBytesFromClassFile(@Nonnull String classDesc) {
      if (classDesc.startsWith("java/") || classDesc.startsWith("javax/")) {
         byte[] classfile = CachedClassfiles.getClassfile(classDesc);

         if (classfile != null) {
            return classfile;
         }
      }

      InputStream classFile = readClassFromClasspath(classDesc);

      try {
         byte[] bytecode = readClass(classFile);
         return bytecode;
      }
      catch (IOException e) {
         throw new RuntimeException("Failed to read class file for " + classDesc.replace('/', '.'), e);
      }
   }

   @Nonnull
   public static byte[] readBytesFromClassFile(@Nonnull Class<?> aClass) {
      String classDesc = aClass.getName().replace('.', '/');
      return readBytesFromClassFile(classDesc);
   }

   @Nonnull @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
   private static InputStream readClassFromClasspath(@Nonnull String classDesc) {
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
      return inputStream;
   }
}
