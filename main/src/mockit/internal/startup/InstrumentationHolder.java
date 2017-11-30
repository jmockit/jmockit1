/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.lang.instrument.*;
import java.util.*;
import java.util.jar.*;
import javax.annotation.*;

public final class InstrumentationHolder implements Instrumentation
{
   public static Instrumentation inst;
   private static InstrumentationHolder wrappedInst;
   public static String hostJREClassName;
   @Nonnull private final List<ClassFileTransformer> transformers;

   @SuppressWarnings("unused")
   public static void agentmain(String agentArgs, @Nonnull Instrumentation instrumentation)
   {
      set(instrumentation, null);
   }

   static void setHostJREClassName(String className) { hostJREClassName = className; }

   static InstrumentationHolder get() { return wrappedInst; }

   boolean wasRecreated() { return transformers.isEmpty(); }

   static Instrumentation set(@Nonnull Instrumentation instrumentation, @Nullable String hostJREClassName)
   {
      inst = instrumentation;
      wrappedInst = new InstrumentationHolder();
      InstrumentationHolder.hostJREClassName = hostJREClassName;
      return wrappedInst;
   }

   private InstrumentationHolder() { transformers = new ArrayList<ClassFileTransformer>(); }

   @Override
   public void addTransformer(@Nonnull ClassFileTransformer transformer, boolean canRetransform)
   {
      removePreviouslyAddedTransformersOfSameType(transformer);
      inst.addTransformer(transformer, canRetransform);
      transformers.add(transformer);
   }

   @Override
   public void addTransformer(@Nonnull ClassFileTransformer transformer)
   {
      removePreviouslyAddedTransformersOfSameType(transformer);
      inst.addTransformer(transformer);
      transformers.add(transformer);
   }

   private void removePreviouslyAddedTransformersOfSameType(@Nonnull ClassFileTransformer transformer)
   {
      Class<?> transformerClass = transformer.getClass();
      ClassLoader transformerCL = transformerClass.getClassLoader();
      String transformerName = transformerClass.getName();

      for (Iterator<ClassFileTransformer> itr = transformers.iterator(); itr.hasNext(); ) {
         ClassFileTransformer previouslyAdded = itr.next();
         Class<?> previousTransformerClass = previouslyAdded.getClass();

         if (
            previousTransformerClass.getClassLoader() != transformerCL &&
            previousTransformerClass.getName().equals(transformerName)
         ) {
            inst.removeTransformer(previouslyAdded);
            itr.remove();
         }
      }
   }

   @Override
   public boolean removeTransformer(@Nonnull ClassFileTransformer transformer)
   {
      transformers.remove(transformer);
      return inst.removeTransformer(transformer);
   }

   @Override
   public void retransformClasses(Class<?>... classes) throws UnmodifiableClassException
   {
      inst.retransformClasses(classes);
   }

   @Override
   public void redefineClasses(ClassDefinition... definitions)
      throws ClassNotFoundException, UnmodifiableClassException
   {
      inst.redefineClasses(definitions);
   }

   @Override
   public boolean isRetransformClassesSupported() { return true; }

   @Override
   public boolean isRedefineClassesSupported() { return true; }

   @Override
   public boolean isModifiableClass(Class<?> theClass) { return inst.isModifiableClass(theClass); }

   @Override
   public Class<?>[] getAllLoadedClasses() { return inst.getAllLoadedClasses(); }

   @Override
   public Class<?>[] getInitiatedClasses(ClassLoader loader) { return inst.getInitiatedClasses(loader); }

   @Override
   public long getObjectSize(Object objectToSize) { return inst.getObjectSize(objectToSize); }

   @Override
   public void appendToBootstrapClassLoaderSearch(JarFile jarfile) {}

   @Override
   public void appendToSystemClassLoaderSearch(JarFile jarfile) {}

   @Override
   public boolean isNativeMethodPrefixSupported() { return false; }

   @Override
   public void setNativeMethodPrefix(ClassFileTransformer transformer, String prefix) {}
}
