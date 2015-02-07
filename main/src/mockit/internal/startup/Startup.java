/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.io.*;
import java.lang.instrument.*;

import mockit.internal.expectations.transformation.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

import org.jetbrains.annotations.*;

/**
 * This is the "agent class" that initializes the JMockit "Java agent". It is not intended for use in client code.
 *
 * @see #premain(String, Instrumentation)
 * @see #agentmain(String, Instrumentation)
 */
public final class Startup
{
   public static boolean initializing;
   @Nullable private static Instrumentation instrumentation;
   private static boolean initializedOnDemand;

   private Startup() {}

   /**
    * This method must only be called by the JVM, to provide the instrumentation object.
    * In order for this to occur, the JVM must be started with "-javaagent:jmockit.jar" as a command line parameter
    * (assuming the jar file is in the current directory).
    * <p/>
    * It is also possible to load other <em>instrumentation tools</em> at this time, by having set the "jmockit-tools"
    * and/or "jmockit-mocks" system properties in the JVM command line.
    * There are two types of instrumentation tools:
    * <ol>
    * <li>A {@link ClassFileTransformer class file transformer}, which will be instantiated and added to the JVM
    * instrumentation service. Such a class must have a no-args constructor.</li>
    * <li>An <em>external mock</em>, which should be a {@code MockUp} subclass with a no-args constructor.
    * </ol>
    *
    * @param agentArgs not used
    * @param inst      the instrumentation service provided by the JVM
    */
   public static void premain(String agentArgs, @NotNull Instrumentation inst) throws IOException
   {
      initialize(true, inst);
   }

   private static void initialize(boolean applyStartupMocks, @NotNull Instrumentation inst) throws IOException
   {
      if (instrumentation == null) {
         instrumentation = inst;

         MockingBridgeFields.createSyntheticFieldsInJREClassToHoldMockingBridges(inst);
         inst.addTransformer(CachedClassfiles.INSTANCE, true);

         if (applyStartupMocks) {
            applyStartupMocks();
         }

         inst.addTransformer(new ExpectationsTransformer(inst));
      }
   }

   private static void applyStartupMocks() throws IOException
   {
      initializing = true;

      try {
         new JMockitInitialization().initialize();
      }
      finally {
         initializing = false;
      }
   }

   /**
    * This method must only be called by the JVM, to provide the instrumentation object.
    * This occurs only when the JMockit Java agent gets loaded on demand, through the Attach API.
    * <p/>
    * For additional details, see the {@link #premain(String, Instrumentation)} method.
    *
    * @param agentArgs not used
    * @param inst      the instrumentation service provided by the JVM
    */
   public static void agentmain(@SuppressWarnings("unused") String agentArgs, @NotNull Instrumentation inst)
      throws IOException
   {
      if (!inst.isRedefineClassesSupported()) {
         throw new UnsupportedOperationException(
            "This JRE must be started with -javaagent:<proper path>jmockit.jar, or in debug mode");
      }

      initialize(false, inst);

      ClassLoader customCL = (ClassLoader) System.getProperties().remove("jmockit-customCL");

      if (customCL != null) {
         reinitializeJMockitUnderCustomClassLoader(customCL);
      }
   }

   private static void reinitializeJMockitUnderCustomClassLoader(@NotNull ClassLoader customLoader)
   {
      Class<?> startupClass;

      try {
         startupClass = customLoader.loadClass(Startup.class.getName());
      }
      catch (ClassNotFoundException ignore) {
         return;
      }

      System.out.println("JMockit: Reinitializing under custom class loader " + customLoader);
      FieldReflection.setField(startupClass, null, "instrumentation", instrumentation);
      MethodReflection.invoke(startupClass, (Object) null, "reapplyStartupMocks");
   }

   private static void reapplyStartupMocks()
   {
      MockingBridgeFields.setMockingBridgeFields();
      try { applyStartupMocks(); } catch (IOException e) { throw new RuntimeException(e); }
   }

   @NotNull public static Instrumentation instrumentation()
   {
      verifyInitialization();
      assert instrumentation != null;
      return instrumentation;
   }

   /**
    * Only called from the coverage tool, when it is executed with {@code -javaagent:jmockit-coverage.jar} even though
    * JMockit is in the classpath.
    */
   public static void initialize(@NotNull Instrumentation inst) throws IOException
   {
      boolean fullJMockit = false;

      try {
         Class.forName("mockit.internal.expectations.transformation.ExpectationsTransformer");
         fullJMockit = true;
      }
      catch (ClassNotFoundException ignored) {}

      instrumentation = inst;

      if (fullJMockit) {
         MockingBridgeFields.createSyntheticFieldsInJREClassToHoldMockingBridges(inst);
         initializing = true;

         try {
            new JMockitInitialization().initialize();
         }
         finally {
            initializing = false;
         }

         inst.addTransformer(CachedClassfiles.INSTANCE, true);
         inst.addTransformer(new ExpectationsTransformer(inst));
      }
   }

   public static boolean wasInitializedOnDemand() { return initializedOnDemand; }

   public static void verifyInitialization()
   {
      if (getInstrumentation() == null) {
         initializedOnDemand = AgentInitialization.loadAgentFromLocalJarFile();
      }
   }

   @Nullable private static Instrumentation getInstrumentation()
   {
      if (instrumentation == null) {
         Class<?> initialStartupClass =
            ClassLoad.loadClass(ClassLoader.getSystemClassLoader(), Startup.class.getName());

         if (initialStartupClass != null) {
            instrumentation = FieldReflection.getField(initialStartupClass, "instrumentation", null);

            if (instrumentation != null) {
               reapplyStartupMocks();
            }
         }
      }

      return instrumentation;
   }

   public static boolean initializeIfPossible()
   {
      if (getInstrumentation() == null) {
         ClassLoader currentCL = Startup.class.getClassLoader();
         boolean usingCustomCL = currentCL != ClassLoader.getSystemClassLoader();

         if (usingCustomCL) {
            //noinspection UseOfPropertiesAsHashtable
            System.getProperties().put("jmockit-customCL", currentCL);
         }

         try {
            boolean initialized = AgentInitialization.loadAgentFromLocalJarFile();

            if (initialized && !usingCustomCL) {
               applyStartupMocks();
            }

            return initialized;
         }
         catch (RuntimeException e) {
            e.printStackTrace(); // makes sure the exception gets printed at least once
            throw e;
         }
         catch (IOException e) {
            e.printStackTrace(); // makes sure the exception gets printed at least once
            throw new RuntimeException(e);
         }
      }

      return true;
   }

   public static void retransformClass(@NotNull Class<?> aClass)
   {
      try { instrumentation().retransformClasses(aClass); } catch (UnmodifiableClassException ignore) {}
   }

   public static void redefineMethods(@NotNull Class<?> classToRedefine, @NotNull byte[] modifiedClassfile)
   {
      redefineMethods(new ClassDefinition(classToRedefine, modifiedClassfile));
   }

   public static void redefineMethods(@NotNull ClassDefinition... classDefs)
   {
      try {
         instrumentation().redefineClasses(classDefs);
      }
      catch (ClassNotFoundException e) {
         // should never happen
         throw new RuntimeException(e);
      }
      catch (UnmodifiableClassException e) {
         throw new RuntimeException(e);
      }
      catch (InternalError ignore) {
         // If a class to be redefined hasn't been loaded yet, the JVM may get a NoClassDefFoundError during
         // redefinition. Unfortunately, it then throws a plain InternalError instead.
         for (ClassDefinition classDef : classDefs) {
            detectMissingDependenciesIfAny(classDef.getDefinitionClass());
         }

         // If the above didn't throw upon detecting a NoClassDefFoundError, then ignore the original error and
         // continue, in order to prevent secondary failures.
      }
   }

   private static void detectMissingDependenciesIfAny(@NotNull Class<?> mockedClass)
   {
      try {
         Class.forName(mockedClass.getName(), false, mockedClass.getClassLoader());
      }
      catch (NoClassDefFoundError e) {
         throw new RuntimeException("Unable to mock " + mockedClass + " due to a missing dependency", e);
      }
      catch (ClassNotFoundException ignore) {
         // Shouldn't happen since the mocked class would already have been found in the classpath.
      }
   }
}
