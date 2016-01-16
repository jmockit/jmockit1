/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests;

import java.io.*;

import org.junit.runners.*;
import org.junit.runners.model.*;

public final class CustomRunner extends BlockJUnit4ClassRunner
{
   private static final ClassLoader CUSTOM_CL = new CustomLoader();

   public CustomRunner(Class<?> testClass) throws InitializationError
   {
      super(reloadTestClass(testClass));
   }

   private static Class<?> reloadTestClass(Class<?> testClass) throws InitializationError
   {
      try {
         return CUSTOM_CL.loadClass(testClass.getName());
      }
      catch (ClassNotFoundException e) {
         throw new InitializationError(e);
      }
   }
}

final class CustomLoader extends ClassLoader
{
   CustomLoader() { super(null); }

   @Override
   protected Class<?> findClass(String name) throws ClassNotFoundException
   {
      ClassLoader systemCL = ClassLoader.getSystemClassLoader();

      if (
         CustomRunner.class.getName().equals(name) || CustomLoader.class.getName().equals(name) ||
         name.startsWith("mockit.") || name.startsWith("org.junit.") || name.startsWith("javax.")
      ) {
         return systemCL.loadClass(name);
      }

      String classFileName = name.replace('.', '/') + ".class";
      InputStream classFile = systemCL.getResourceAsStream(classFileName);
      Class<?> testClass;

      try {
         int classFileSize = classFile.available();
         byte[] classBytes = new byte[classFileSize];
         int bytesRead = classFile.read(classBytes);
         assert bytesRead == classFileSize;

         testClass = defineClass(name, classBytes, 0, classFileSize, null);
         System.out.println("Reloaded class: " + name);
      }
      catch (IOException e) {
         throw new RuntimeException(e);
      }

      return testClass;
   }
}
