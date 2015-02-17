/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import javax.naming.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class CustomClassLoadingTest
{
   static final class IsolatedClassLoader extends URLClassLoader
   {
      private final ClassLoader systemCL = ClassLoader.getSystemClassLoader();

      IsolatedClassLoader() throws MalformedURLException
      {
         super(
            new URL[] {
               new URL("file:///github/jmockit1.org/jmockit.jar"),
               new URL("file:lib/junit-4.12.jar"), new URL("file:lib/hamcrest-core-1.2.jar"),
               new URL("file:main/test-classes/")
            },
            null);
      }

      @Override
      public Class<?> loadClass(String name) throws ClassNotFoundException
      {
         Class<?> c = findLoadedClass(name);

         if (c == null) {
            try { c = findClass(name); } catch (ClassNotFoundException ignore) { c = systemCL.loadClass(name); }
         }

         return c;
      }
   }

   public static void main(String[] args) throws Exception
   {
      ClassLoader cl = new IsolatedClassLoader();
      Class<?> jUnitCoreClass = cl.loadClass("org.junit.runner.JUnitCore");
      Class<?> runListenerClass = cl.loadClass("org.junit.runner.notification.RunListener");
      Class<?> textListenerClass = cl.loadClass("org.junit.internal.TextListener");
      Class<?> testClass = cl.loadClass(CustomClassLoadingTest.class.getName());

      //noinspection ClassNewInstance
      Object jUnit = jUnitCoreClass.newInstance();

      Object textListener = textListenerClass.getConstructor(PrintStream.class).newInstance(System.out);
      Method addListener = jUnitCoreClass.getMethod("addListener", runListenerClass);
      addListener.invoke(jUnit, textListener);

      if (args.length > 0){
         System.out.println("Pre-initializing JMockit...");
         //noinspection UnnecessaryFullyQualifiedName
         mockit.internal.startup.Startup.initializeIfPossible();
      }

      Class<?>[] classes = {testClass};
      Method run = jUnitCoreClass.getMethod("run", classes.getClass());
      run.invoke(jUnit, new Object[] {classes});
   }

   final Thread currentThread = Thread.currentThread();
   final ClassLoader originalCL = currentThread.getContextClassLoader();

   @After
   public void restoreOriginalContextCL()
   {
      currentThread.setContextClassLoader(originalCL);
   }

   @Test
   public void changeContextCLDuringReplay(@Mocked final InitialContext ic) throws Exception
   {
      // Uses TestRun instance associated with current context CL:
      new Expectations() {{ ic.lookup(anyString); result = "mocked"; }};

      // OpenEJB does this whenever a method is called on an EJB:
      ClassLoader childOfSystemCL = new URLClassLoader(new URL[0]);
      currentThread.setContextClassLoader(childOfSystemCL);

      // Replay with a different context CL; must use same TestRun instance:
      assertEquals("mocked", ic.lookup("test"));
   }

   static class Collaborator { Collaborator() { throw new RuntimeException(); } }
   static class MockCollaborator extends MockUp<Collaborator> { @Mock void $init() {} }
   static class CustomCL extends ClassLoader { CustomCL(ClassLoader parent) { super(parent); } }

   @Test
   public void setContextCLToNull()
   {
      new MockCollaborator();

      currentThread.setContextClassLoader(null);

      new Collaborator();
   }

   @Test
   public void changeContextCLToCustomCLWhoseParentIsOriginalContextCL()
   {
      new MockCollaborator();

      currentThread.setContextClassLoader(new CustomCL(originalCL));

      new Collaborator();
   }

   @Test
   public void changeContextCLToCustomCLWhoseParentIsAnotherCustomCL()
   {
      new MockCollaborator();

      currentThread.setContextClassLoader(new CustomCL(new CustomCL(originalCL)));

      new Collaborator();
   }
}
