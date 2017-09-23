/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.io.*;
import java.lang.instrument.*;
import javax.annotation.*;

import mockit.coverage.*;
import mockit.internal.expectations.transformation.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

/**
 * This is the "agent class" that initializes the JMockit "Java agent". It is not intended for use in client code.
 * <p/>
 * There are several possible initialization scenarios:
 * <ol>
 *    <li>Execution with {@code -javaagent:jmockit-1-x.jar} and without reloading in a custom class loader.</li>
 *    <li>Execution from system CL without {@code -javaagent} and without reloading in custom CL.</li>
 *    <li>Execution with {@code -javaagent} and with reloading in custom CL.</li>
 *    <li>Execution from system CL without {@code -javaagent} and with reloading in custom CL.</li>
 *    <li>Execution from custom CL without {@code -javaagent} and without reloading in another custom CL.</li>
 *    <li>Execution from custom CL without {@code -javaagent} and with reloading in another custom CL.</li>
 * </ol>
 *
 * @see #premain(String, Instrumentation)
 * @see #agentmain(String, Instrumentation)
 */
public final class Startup
{
   public static boolean initializing;

   private Startup() {}

   /**
    * This method must only be called by the JVM, to provide the instrumentation object.
    * In order for this to occur, the JVM must be started with "-javaagent:jmockit-1.x.jar" as a command line parameter
    * (assuming the jar file is in the current directory).
    * <p/>
    * It is also possible to load user-specified fakes at this time, by having set the "fakes" system property.
    *
    * @param agentArgs not used
    * @param inst      the instrumentation service provided by the JVM
    */
   public static void premain(@Nullable String agentArgs, @Nonnull Instrumentation inst)
   {
      if (!activateCodeCoverageIfRequested(agentArgs, inst)) {
         String hostJREClassName = MockingBridgeFields.createSyntheticFieldsInJREClassToHoldMockingBridges(inst);
         Instrumentation wrappedInst = InstrumentationHolder.set(inst, hostJREClassName);
         initialize(wrappedInst);
      }
   }

   private static void initialize(@Nonnull Instrumentation inst)
   {
      inst.addTransformer(CachedClassfiles.INSTANCE, true);
      applyStartupFakes(inst);
      inst.addTransformer(new ExpectationsTransformer(inst));
   }

   private static void applyStartupFakes(@Nonnull Instrumentation inst)
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
   public static void agentmain(@Nullable String agentArgs, @Nonnull Instrumentation inst)
   {
      if (!inst.isRedefineClassesSupported()) {
         throw new UnsupportedOperationException(
            "This JRE must be started in debug mode, or with -javaagent:<proper path>/jmockit.jar");
      }

      String hostJREClassName = InstrumentationHolder.hostJREClassName;

      if (hostJREClassName == null) {
         hostJREClassName = MockingBridgeFields.createSyntheticFieldsInJREClassToHoldMockingBridges(inst);
      }

      InstrumentationHolder.set(inst, hostJREClassName);
      activateCodeCoverageIfRequested(agentArgs, inst);
   }

   private static boolean activateCodeCoverageIfRequested(@Nullable String agentArgs, @Nonnull Instrumentation inst)
   {
      if ("coverage".equals(agentArgs)) {
         try {
            CodeCoverage coverage = CodeCoverage.create(true);
            inst.addTransformer(coverage);

            return true;
         }
         catch (Throwable t) {
            try {
               PrintWriter out = new PrintWriter("coverage-failure.txt");
               t.printStackTrace(out);
               out.close();
            }
            catch (FileNotFoundException ignore) {}
         }
      }

      return false;
   }

   @Nonnull
   public static Instrumentation instrumentation()
   {
      verifyInitialization();
      return InstrumentationHolder.get();
   }

   public static void verifyInitialization()
   {
      if (InstrumentationHolder.get() == null) {
         new AgentLoader().loadAgent(null);
      }
   }

   public static boolean initializeIfPossible()
   {
      InstrumentationHolder wrappedInst = InstrumentationHolder.get();

      if (wrappedInst == null) {
         try {
            new AgentLoader().loadAgent(null);
            Instrumentation inst = InstrumentationHolder.get();

            if (InstrumentationHolder.hostJREClassName == null) {
               String hostJREClassName = MockingBridgeFields.createSyntheticFieldsInJREClassToHoldMockingBridges(inst);
               InstrumentationHolder.setHostJREClassName(hostJREClassName);
            }

            initialize(inst);
            return true;
         }
         catch (IllegalStateException e) {
            StackTrace.filterStackTrace(e);
            e.printStackTrace();
         }
         catch (RuntimeException e) { e.printStackTrace(); }

         return false;
      }

      if (wrappedInst.wasRecreated()) {
         initialize(wrappedInst);
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
      Instrumentation instrumentation = InstrumentationHolder.get();

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
