/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;

import org.junit.*;
import org.junit.internal.TextListener;
import org.junit.runner.*;
import org.junit.runner.notification.*;

import static org.junit.Assert.assertFalse;

import mockit.internal.startup.*;

@Ignore("Run only with -javaagent and as the only test class in the suite.")
public final class CustomClassLoadingTest
{
   static final String classPath = System.getProperty("java.class.path");
   static final String[] classPathEntries = classPath.split(File.pathSeparator);

   static final class IsolatedClassLoader extends URLClassLoader
   {
      private final ClassLoader systemCL = ClassLoader.getSystemClassLoader();

      IsolatedClassLoader() throws MalformedURLException
      {
         super(
            new URL[] {
               new URL("file:target/classes/"),
               getURLForClassPathEntry("junit-4.12.jar"),
               getURLForClassPathEntry("hamcrest-core-1.3.jar"),
               new URL("file:target/test-classes/")
            },
            null);
      }

      private static URL getURLForClassPathEntry(String jarFileName) throws MalformedURLException
      {
         for (String classPathEntry : classPathEntries) {
            if (classPathEntry.endsWith(jarFileName)) {
               return new URL("file:" + classPathEntry);
            }
         }

         return null;
      }

      @Override
      public Class<?> loadClass(String name) throws ClassNotFoundException
      {
         Class<?> c = findLoadedClass(name);

         if (c == null) {
            try {
               c = findClass(name);
            }
            catch (ClassNotFoundException ignore) {
               c = systemCL.loadClass(name); // used for JRE classes only
            }
         }

         return c;
      }
   }

   ClassLoader cl;
   ByteArrayOutputStream testOutput;

   @Before
   public void prepareExecutionEnvironmentWithCustomClassLoader() throws Exception
   {
      cl = new IsolatedClassLoader();
      testOutput = new ByteArrayOutputStream(8192);

      // Emulate JVM where JMockit agent was not loaded yet.
      String classPathWithoutJMockitJar = classPath.substring(0, classPath.lastIndexOf(File.pathSeparatorChar));
      System.setProperty("java.class.path", classPathWithoutJMockitJar);
      InstrumentationHolder.agentmain(null, null);
   }

   @Test @SuppressWarnings("OverlyCoupledMethod")
   public void createAndExecuteTestRunOnIsolatedClassLoader() throws Exception
   {
      Class<?>[] testClasses = loadIsolatedTestClasses(
         //CapturingImplementationsTest.class,
         CapturingInstancesTest.class, CascadingFieldTest.class, CascadingParametersTest.class,
         ClassInitializationTest.class, //ClassLoadingAndJREMocksTest.class,
         DelegateTest.class, ExpectationsTest.class, FinalMockFieldsTest.class, FullVerificationsTest.class,
         InjectableMockedTest.class, //DelegateInvocationProceedTest.class, //JREMockingTest.class,
         MockUpTest.class, StandardDITest.class
      );

      Object jUnit = createJUnitCoreInIsolatedCL();
      Method runMethod = jUnit.getClass().getDeclaredMethod("run", Class[].class);
      runMethod.invoke(jUnit, (Object) testClasses);

      String output = testOutput.toString();
      System.out.print(output);
      assertFalse("There were test failures", output.contains("FAILURES!"));
   }

   Object createJUnitCoreInIsolatedCL() throws Exception
   {
      Class<?> jUnitCoreClass = loadIsolatedClass(JUnitCore.class);
      Object jUnit = jUnitCoreClass.newInstance();

      Class<?> textListenerClass = loadIsolatedClass(TextListener.class);
      PrintStream output = new PrintStream(testOutput);
      Object textListener = textListenerClass.getDeclaredConstructor(PrintStream.class).newInstance(output);

      Class<?> runListenerClass = loadIsolatedClass(RunListener.class);
      Method addListener = jUnitCoreClass.getDeclaredMethod("addListener", runListenerClass);
      addListener.invoke(jUnit, textListener);

      return jUnit;
   }

   Class<?>[] loadIsolatedTestClasses(Class<?>... testClasses) throws ClassNotFoundException
   {
      Class<?>[] isolatedTestClasses = new Class<?>[testClasses.length];

      for (int i = 0; i < testClasses.length; i++) {
         isolatedTestClasses[i] = loadIsolatedClass(testClasses[i]);
      }

      return isolatedTestClasses;
   }

   Class<?> loadIsolatedClass(Class<?> aClass) throws ClassNotFoundException { return cl.loadClass(aClass.getName()); }
}
