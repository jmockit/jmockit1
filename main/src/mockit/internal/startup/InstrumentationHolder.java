/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.lang.instrument.*;
import java.util.*;
import java.util.jar.*;

import mockit.internal.util.*;

public final class InstrumentationHolder implements Instrumentation
{
   private static Instrumentation inst;
   private static Instrumentation wrappedInst;

   @SuppressWarnings("unused")
   public static void agentmain(String agentArgs, Instrumentation instrumentation)
   {
      set(instrumentation);
   }

   public static Instrumentation get()
   {
      if (inst == null) {
         recoverInstrumentationFromHolderClassInSystemClassLoaderIfAvailable();
      }

      return wrappedInst;
   }

   private static void recoverInstrumentationFromHolderClassInSystemClassLoaderIfAvailable()
   {
      Class<?> regularHolderClass = getHolderClassFromSystemClassLoaderIfThisIsCustomClassLoader();

      if (regularHolderClass != null) {
         inst = FieldReflection.getField(regularHolderClass, "inst", null);
         wrappedInst = FieldReflection.getField(regularHolderClass, "wrappedInst", null);

         if (inst != null && inst == wrappedInst) {
            wrappedInst = new InstrumentationHolder();
            FieldReflection.setField(regularHolderClass, null, "wrappedInst", wrappedInst);
         }
      }
   }

   private static Class<?> getHolderClassFromSystemClassLoaderIfThisIsCustomClassLoader()
   {
      ClassLoader systemCL = ClassLoader.getSystemClassLoader();

      if (InstrumentationHolder.class.getClassLoader() == systemCL) {
         return null;
      }

      return ClassLoad.loadClass(systemCL, InstrumentationHolder.class.getName());
   }

   static void set(Instrumentation instrumentation)
   {
      inst = instrumentation;
      wrappedInst = instrumentation;
   }

   private final List<ClassFileTransformer> transformers = new ArrayList<ClassFileTransformer>();

   @Override
   public void addTransformer(ClassFileTransformer transformer, boolean canRetransform)
   {
      removePreviouslyAddedTransformersOfSameType(transformer);
      inst.addTransformer(transformer, canRetransform);
      transformers.add(transformer);
   }

   @Override
   public void addTransformer(ClassFileTransformer transformer)
   {
      removePreviouslyAddedTransformersOfSameType(transformer);
      inst.addTransformer(transformer);
      transformers.add(transformer);
   }

   private void removePreviouslyAddedTransformersOfSameType(ClassFileTransformer transformer)
   {
      String transformerName = transformer.getClass().getName();

      for (Iterator<ClassFileTransformer> itr = transformers.iterator(); itr.hasNext(); ) {
         ClassFileTransformer previouslyAdded = itr.next();

         if (previouslyAdded.getClass().getName().equals(transformerName)) {
            inst.removeTransformer(previouslyAdded);
            itr.remove();
         }
      }
   }

   @Override
   public boolean removeTransformer(ClassFileTransformer transformer)
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
   public Class[] getAllLoadedClasses() { return inst.getAllLoadedClasses(); }

   @Override
   public Class[] getInitiatedClasses(ClassLoader loader) { return inst.getInitiatedClasses(loader); }

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
