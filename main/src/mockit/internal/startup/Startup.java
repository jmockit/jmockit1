/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.io.*;
import java.lang.instrument.*;
import javax.annotation.*;

import mockit.coverage.*;
import mockit.coverage.standalone.*;
import mockit.internal.expectations.transformation.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

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
    * It is also possible to load user-specified mock-ups at this time, by having set the "mockups" system property.
    *
    * @param agentArgs not used
    * @param inst      the instrumentation service provided by the JVM
    */
   public static void premain(@Nullable String agentArgs, @Nonnull Instrumentation inst) throws IOException
   {
      if (!activateCodeCoverageIfRequested(agentArgs, inst)) {
         initialize(true, inst);
      }
   }

   private static void initialize(boolean applyStartupMocks, @Nonnull Instrumentation inst) throws IOException
   {
      if (instrumentation == null) {
         instrumentation = inst;

         MockingBridgeFields.createSyntheticFieldsInJREClassToHoldMockingBridges(inst);
         inst.addTransformer(CachedClassfiles.INSTANCE, true);

         if (applyStartupMocks) {
            applyStartupMocks(inst);
         }

         inst.addTransformer(new ExpectationsTransformer(inst));
      }
   }

   private static void applyStartupMocks(@Nonnull Instrumentation inst) throws IOException
   {
      initializing = true;

      try {
         new JMockitInitialization().initialize(inst);
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
   @SuppressWarnings({"unused", "WeakerAccess"})
   public static void agentmain(@Nullable String agentArgs, @Nonnull Instrumentation inst) throws IOException
   {
      if (!inst.isRedefineClassesSupported()) {
         throw new UnsupportedOperationException(
            "This JRE must be started in debug mode, or with -javaagent:<proper path>/jmockit.jar");
      }

      initialize(false, inst);

      ClassLoader customCL = (ClassLoader) System.getProperties().remove("jmockit-customCL");

      if (customCL != null) {
         reinitializeJMockitUnderCustomClassLoader(customCL);
      }

      activateCodeCoverageIfRequested(agentArgs, inst);
   }

   private static boolean activateCodeCoverageIfRequested(@Nullable String agentArgs, @Nonnull Instrumentation inst)
      throws FileNotFoundException
   {
      if ("coverage".equals(agentArgs)) {
         try {
            CoverageControl.create();

            CodeCoverage coverage = CodeCoverage.create(true);
            inst.addTransformer(coverage);

            return true;
         }
         catch (Throwable t) {
            PrintWriter out = new PrintWriter("coverage-failure.txt");
            t.printStackTrace(out);
            out.close();
         }
      }

      return false;
   }

   private static void reinitializeJMockitUnderCustomClassLoader(@Nonnull ClassLoader customLoader)
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

   @SuppressWarnings("ConstantConditions")
   private static void reapplyStartupMocks()
   {
      MockingBridgeFields.setMockingBridgeFields();
      try { applyStartupMocks(instrumentation); } catch (IOException e) { throw new RuntimeException(e); }
   }

   @Nonnull
   public static Instrumentation instrumentation()
   {
      verifyInitialization();
      assert instrumentation != null;
      return instrumentation;
   }

   public static boolean wasInitializedOnDemand() { return initializedOnDemand; }

   public static void verifyInitialization()
   {
      if (getInstrumentation() == null) {
         new AgentLoader().loadAgent(null);
         initializedOnDemand = true;
      }
   }

   @Nullable
   private static Instrumentation getInstrumentation()
   {
      if (instrumentation == null) {
         ClassLoader systemCL = ClassLoader.getSystemClassLoader();
         Class<?> initialStartupClass = ClassLoad.loadClass(systemCL, Startup.class.getName());

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
            new AgentLoader().loadAgent(null);

            if (!usingCustomCL) {
               assert instrumentation != null;
               applyStartupMocks(instrumentation);
            }

            return true;
         }
         catch (IllegalStateException e) {
            StackTrace.filterStackTrace(e);
            e.printStackTrace();
         }
         catch (RuntimeException e) { e.printStackTrace(); }
         catch (IOException e) { e.printStackTrace(); }

         return false;
      }

      return true;
   }

   public static void retransformClass(@Nonnull Class<?> aClass)
   {
      try { instrumentation().retransformClasses(aClass); } catch (UnmodifiableClassException ignore) {}
   }

   public static void redefineMethods(@Nonnull Class<?> classToRedefine, @Nonnull byte[] modifiedClassfile)
   {
      redefineMethods(new ClassDefinition(classToRedefine, modifiedClassfile));
   }

   public static void redefineMethods(@Nonnull ClassDefinition... classDefs)
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

   private static void detectMissingDependenciesIfAny(@Nonnull Class<?> mockedClass)
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

   @Nullable
   public static Class<?> getClassIfLoaded(@Nonnull String classDescOrName)
   {
      if (instrumentation != null) {
         String className = classDescOrName.replace('/', '.');

         for (Class<?> aClass : instrumentation.getAllLoadedClasses()) {
            if (aClass.getName().equals(className)) {
               return aClass;
            }
         }
      }

      return null;
   }
}
