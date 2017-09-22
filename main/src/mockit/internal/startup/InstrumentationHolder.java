/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.startup;

import java.lang.instrument.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.jar.*;

public final class InstrumentationHolder implements Instrumentation
{
   public static Instrumentation inst;
   private static InstrumentationHolder wrappedInst;
   public static String hostJREClassName;

   @SuppressWarnings("unused")
   public static void agentmain(String agentArgs, Instrumentation instrumentation)
   {
      set(instrumentation, null);
   }

   static void setHostJREClassName(String className)
   {
      hostJREClassName = className;

      Class<?> regularHolderClass = getHolderClassFromSystemClassLoaderIfThisIsCustomClassLoader();

      if (regularHolderClass != null) {
         try {
            Field field = getField(regularHolderClass, "hostJREClassName");
            field.set(null, className);
         }
         catch (IllegalAccessException e) { throw new RuntimeException(e); }
      }
   }

   private static Field getField(Class<?> aClass, String fieldName)
   {
      try { return aClass.getDeclaredField(fieldName); }
      catch (NoSuchFieldException e) { throw new RuntimeException(e); }
   }

   public static InstrumentationHolder get()
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
         inst = readStaticField(regularHolderClass, "inst");

         if (inst != null) {
            hostJREClassName = readStaticField(regularHolderClass, "hostJREClassName");
            invokeClearTransformersFromSystemCL(regularHolderClass);
            wrappedInst = new InstrumentationHolder();
         }
      }
   }

   private static Class<?> getHolderClassFromSystemClassLoaderIfThisIsCustomClassLoader()
   {
      ClassLoader systemCL = ClassLoader.getSystemClassLoader();

      if (InstrumentationHolder.class.getClassLoader() == systemCL) {
         return null;
      }

      try { return Class.forName(InstrumentationHolder.class.getName(), true, systemCL); }
      catch (ClassNotFoundException e) { return null; }
   }

   private static <T> T readStaticField(Class<?> aClass, String fieldName)
   {
      Field field = getField(aClass, fieldName);

      try {
         @SuppressWarnings("unchecked") T value = (T) field.get(null);
         return value;
      }
      catch (IllegalAccessException e) { throw new RuntimeException(e); }
   }

   private static void invokeClearTransformersFromSystemCL(Class<?> regularHolderClass)
   {
      try {
         Method method = regularHolderClass.getDeclaredMethod("clearTransformers");
         method.invoke(null);
      }
      catch (NoSuchMethodException e) { throw new RuntimeException(e); }
      catch (IllegalAccessException e) { throw new RuntimeException(e); }
      catch (InvocationTargetException e) { throw new RuntimeException(e.getCause()); }
   }

   public static void clearTransformers()
   {
      for (ClassFileTransformer transformer : transformers) {
         inst.removeTransformer(transformer);
      }

      transformers.clear();
   }

   boolean wasRecreated() { return transformers.isEmpty(); }

   static Instrumentation set(Instrumentation instrumentation, String hostJREClassName)
   {
      if (wrappedInst != null) {
         clearTransformers();
      }

      inst = instrumentation;
      wrappedInst = instrumentation == null ? null : new InstrumentationHolder();
      InstrumentationHolder.hostJREClassName = hostJREClassName;
      return wrappedInst;
   }

   public static List<ClassFileTransformer> transformers;

   private InstrumentationHolder()
   {
      Class<?> regularHolderClass = getHolderClassFromSystemClassLoaderIfThisIsCustomClassLoader();

      if (regularHolderClass == null) {
         transformers = new ArrayList<ClassFileTransformer>();
      }
      else {
         transformers = readStaticField(regularHolderClass, "transformers");
      }
   }

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
