/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.io.*;
import java.lang.instrument.*;
import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;

import mockit.coverage.*;
import mockit.internal.*;
import mockit.internal.expectations.transformation.*;
import mockit.internal.state.*;
import mockit.internal.util.*;
import static mockit.internal.startup.ClassLoadingBridgeFields.createSyntheticFieldsInJREClassToHoldClassLoadingBridges;

/**
 * This is the "agent class" that initializes the JMockit "Java agent". It is not intended for use in client code.
 * <p/>
 * There are two possible initialization scenarios:
 * <ol>
 *    <li>Execution with <tt>-javaagent:jmockit-1-x.jar</tt>.</li>
 *    <li>Execution without <tt>-javaagent</tt>, by self-attaching with the Attach API.</li>
 * </ol>
 *
 * @see #premain(String, Instrumentation)
 * @see #agentmain(String, Instrumentation)
 */
public final class Startup
{
   public static boolean initializing;
   @Nullable private static volatile Instrumentation instrumentation;
   private static final String instrumentationFieldName = "instrumentation";

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
   public static void premain(@Nullable String agentArgs, @Nonnull Instrumentation inst) {
      if (!activateCodeCoverageIfRequested(agentArgs, inst)) {
         createSyntheticFieldsInJREClassToHoldClassLoadingBridges(inst);
         instrumentation = inst;
         initialize(inst);
      }
   }

   private static void initialize(@Nonnull Instrumentation inst) {
      inst.addTransformer(CachedClassfiles.INSTANCE, true);
      applyStartupFakes(inst);
      inst.addTransformer(new ExpectationsTransformer());
   }

   private static void applyStartupFakes(@Nonnull Instrumentation instr) {
      initializing = true;

      try {
         JMockitInitialization.initialize(instr);
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
   public static void agentmain(@Nullable String agentArgs, @Nonnull Instrumentation inst) {
      if (!inst.isRedefineClassesSupported()) {
         throw new UnsupportedOperationException("This JRE must be started in debug mode, or with -javaagent:<proper path>/jmockit.jar");
      }

      activateCodeCoverageIfRequested(agentArgs, inst);

      final Set<Thread> currentThreads = Thread.getAllStackTraces().keySet();

      for (final Thread thread : currentThreads) {
         try {
            final ClassLoader clsLoader = thread.getContextClassLoader();
            final Class<?> cls = (clsLoader == null) ? null : clsLoader.loadClass(Startup.class.getName());
            final Field instField = cls.getDeclaredField(instrumentationFieldName);
            if (instField != null) {
               instField.setAccessible(true);
               instField.set(null, inst);
               instField.setAccessible(false);
            }
         } catch (Throwable ignore) {}
      }
   }

   private static boolean activateCodeCoverageIfRequested(@Nullable String agentArgs, @Nonnull Instrumentation inst) {
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

   @Nonnull @SuppressWarnings("ConstantConditions")
   public static Instrumentation instrumentation() { return instrumentation; }

   public static void verifyInitialization() {
      if (instrumentation == null) {
         throw new IllegalStateException(
            "JMockit didn't get initialized; please check jmockit.jar precedes junit.jar in the classpath");
      }
   }

   public static boolean initializeIfPossible() {
      if (instrumentation == null) {
         try {
            new AgentLoader().loadAgent(null);
            createSyntheticFieldsInJREClassToHoldClassLoadingBridges(instrumentation);
            initialize(instrumentation);
            return true;
         }
         catch (IllegalStateException e) {
            StackTrace.filterStackTrace(e);
            e.printStackTrace();
         }
         catch (RuntimeException e) { e.printStackTrace(); }

         return false;
      }

      return true;
   }

   @SuppressWarnings("ConstantConditions")
   public static void retransformClass(@Nonnull Class<?> aClass) {
      try { instrumentation.retransformClasses(aClass); } catch (UnmodifiableClassException ignore) {}
   }

   public static void redefineMethods(@Nonnull ClassIdentification classToRedefine, @Nonnull byte[] modifiedClassfile) {
      Class<?> loadedClass = classToRedefine.getLoadedClass();
      redefineMethods(loadedClass, modifiedClassfile);
   }

   public static void redefineMethods(@Nonnull Class<?> classToRedefine, @Nonnull byte[] modifiedClassfile) {
      redefineMethods(new ClassDefinition(classToRedefine, modifiedClassfile));
   }

   public static void redefineMethods(@Nonnull ClassDefinition... classDefs) {
      try {
         //noinspection ConstantConditions
         instrumentation.redefineClasses(classDefs);
      }
      catch (ClassNotFoundException | UnmodifiableClassException e) {
         throw new RuntimeException(e); // should never happen
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

   private static void detectMissingDependenciesIfAny(@Nonnull Class<?> mockedClass) {
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
   public static Class<?> getClassIfLoaded(@Nonnull String classDescOrName) {
      String className = classDescOrName.replace('/', '.');
      @SuppressWarnings("ConstantConditions") Class<?>[] loadedClasses = instrumentation.getAllLoadedClasses();

      for (Class<?> aClass : loadedClasses) {
         if (aClass.getName().equals(className)) {
            return aClass;
         }
      }

      return null;
   }
}
