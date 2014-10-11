/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.state;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.*;

import static java.lang.reflect.Modifier.*;

import org.jetbrains.annotations.*;

import mockit.internal.*;
import mockit.internal.capturing.*;
import mockit.internal.expectations.mocking.*;
import mockit.internal.startup.*;
import mockit.internal.util.*;

import static mockit.internal.util.GeneratedClasses.*;
import static mockit.internal.util.Utilities.*;

/**
 * Holds data about redefined real classes and their corresponding mock classes (if any), and provides methods to
 * add/remove such state both from this instance and from other state holders with associated data.
 */
public final class MockFixture
{
   /**
    * Similar to {@code redefinedClasses}, but for classes modified by a {@code ClassFileTransformer} such as the
    * {@code CaptureTransformer}, and containing the pre-transform bytecode instead of the modified one.
    */
   @NotNull private final Map<ClassIdentification, byte[]> transformedClasses;

   /**
    * Real classes currently redefined in the running JVM and their current (modified) bytecodes.
    * <p/>
    * The keys in the map allow each redefined real class to be later restored to a previous definition.
    * <p/>
    * The modified bytecode arrays in the map allow a new redefinition to be made on top of the current redefinition
    * (in the case of the Mockups API), or to restore the class to a previous definition (provided the map is copied
    * between redefinitions of the same class).
    */
   @NotNull private final Map<Class<?>, byte[]> redefinedClasses;

   /**
    * Subset of all currently redefined classes which contain one or more native methods.
    * <p/>
    * This is needed because in order to restore such methods it is necessary (for some classes) to re-register them
    * with the JVM.
    *
    * @see #reregisterNativeMethodsForRestoredClass(Class)
    */
   @NotNull private final Set<String> redefinedClassesWithNativeMethods;

   /**
    * Maps redefined real classes to the internal name of the corresponding mock classes, when it's the case.
    * <p/>
    * This allows any global state associated to a mock class to be discarded when the corresponding real class is
    * later restored to its original definition.
    */
   @NotNull private final Map<Class<?>, String> realClassesToMockClasses;

   @NotNull private final List<Class<?>> mockedClasses;
   @NotNull private final Map<Type, InstanceFactory> mockedTypesAndInstances;

   @NotNull private final List<CaptureTransformer<?>> captureTransformers;

   public MockFixture()
   {
      transformedClasses = new HashMap<ClassIdentification, byte[]>(2);
      redefinedClasses = new IdentityHashMap<Class<?>, byte[]>(8);
      redefinedClassesWithNativeMethods = new HashSet<String>();
      realClassesToMockClasses = new IdentityHashMap<Class<?>, String>(8);
      mockedClasses = new ArrayList<Class<?>>();
      mockedTypesAndInstances = new IdentityHashMap<Type, InstanceFactory>();
      captureTransformers = new ArrayList<CaptureTransformer<?>>();
   }

   // Methods to add/remove transformed/redefined classes /////////////////////////////////////////////////////////////

   public void addTransformedClass(@NotNull ClassIdentification classId, @NotNull byte[] pretransformClassfile)
   {
      transformedClasses.put(classId, pretransformClassfile);
   }

   public void addRedefinedClass(
      @Nullable String mockClassInternalName, @NotNull Class<?> redefinedClass, @NotNull byte[] modifiedClassfile)
   {
      if (mockClassInternalName != null) {
         String previousNames = realClassesToMockClasses.put(redefinedClass, mockClassInternalName);

         if (previousNames != null) {
            realClassesToMockClasses.put(redefinedClass, previousNames + ' ' + mockClassInternalName);
         }
      }

      addRedefinedClass(redefinedClass, modifiedClassfile);
   }

   public void addRedefinedClass(@NotNull Class<?> redefinedClass, @NotNull byte[] modifiedClassfile)
   {
      redefinedClasses.put(redefinedClass, modifiedClassfile);
   }

   public void registerMockedClass(@NotNull Class<?> mockedType)
   {
      if (!containsReference(mockedClasses, mockedType)) {
         if (Proxy.isProxyClass(mockedType)) {
            mockedType = mockedType.getInterfaces()[0];
         }

         mockedClasses.add(mockedType);
      }
   }

   public boolean isStillMocked(@Nullable Object instance, @NotNull String classDesc)
   {
      Class<?> targetClass;

      if (instance == null) {
         targetClass = ClassLoad.loadByInternalName(classDesc);
         return isClassAssignableTo(mockedClasses, targetClass);
      }

      targetClass = instance.getClass();
      return mockedTypesAndInstances.containsKey(targetClass) || isInstanceOfMockedClass(instance);
   }

   public boolean isMockedClass(@NotNull Class<?> targetClass)
   {
      int n = mockedClasses.size();

      for (int i = 0; i < n; i++) {
         if (mockedClasses.get(i) == targetClass) {
            return true;
         }
      }

      return false;
   }

   @Nullable
   public Class<?> findClassAlreadyMocked(@NotNull Class<?> targetClass)
   {
      return findClassAssignableFrom(mockedClasses, targetClass);
   }

   public boolean isInstanceOfMockedClass(@NotNull Object mockedInstance)
   {
      Class<?> mockedClass = mockedInstance.getClass();
      return findClassAlreadyMocked(mockedClass) != null;
   }

   public void registerInstanceFactoryForMockedType(
      @NotNull Class<?> mockedType, @NotNull InstanceFactory mockedInstanceFactory)
   {
      registerMockedClass(mockedType);
      mockedTypesAndInstances.put(mockedType, mockedInstanceFactory);
   }

   @Nullable
   public InstanceFactory findInstanceFactory(@NotNull Type mockedType)
   {
      Class<?> mockedClass = getClassType(mockedType);
      InstanceFactory instanceFactory = mockedTypesAndInstances.get(mockedType);

      if (instanceFactory != null) {
         return instanceFactory;
      }

      boolean abstractType = mockedClass.isInterface() || isAbstract(mockedClass.getModifiers());

      for (Entry<Type, InstanceFactory> entry : mockedTypesAndInstances.entrySet()) {
         Type registeredMockedType = entry.getKey();
         Class<?> registeredMockedClass = getClassType(registeredMockedType);

         if (abstractType) {
            registeredMockedClass = getMockedClassOrInterfaceType(registeredMockedClass);
         }

         if (mockedClass.isAssignableFrom(registeredMockedClass)) {
            instanceFactory = entry.getValue();
            break;
         }
      }

      return instanceFactory;
   }

   public void restoreAndRemoveRedefinedClasses(@Nullable Set<Class<?>> desiredClasses)
   {
      Set<Class<?>> classesToRestore = desiredClasses == null ? redefinedClasses.keySet() : desiredClasses;
      RedefinitionEngine redefinitionEngine = new RedefinitionEngine();

      for (Class<?> redefinedClass : classesToRestore) {
         redefinitionEngine.restoreOriginalDefinition(redefinedClass);
         restoreDefinition(redefinedClass);
         discardStateForCorrespondingMockClassIfAny(redefinedClass);
      }

      if (desiredClasses == null) {
         redefinedClasses.clear();
      }
      else {
         redefinedClasses.keySet().removeAll(desiredClasses);
      }
   }

   private void restoreDefinition(@NotNull Class<?> redefinedClass)
   {
      if (redefinedClassesWithNativeMethods.contains(redefinedClass.getName())) {
         reregisterNativeMethodsForRestoredClass(redefinedClass);
      }

      removeMockedClass(redefinedClass);
   }

   private void removeMockedClass(@NotNull Class<?> mockedClass)
   {
      mockedTypesAndInstances.remove(mockedClass);
      mockedClasses.remove(mockedClass);
   }

   private void discardStateForCorrespondingMockClassIfAny(@NotNull Class<?> redefinedClass)
   {
      String mockClassesInternalNames = realClassesToMockClasses.remove(redefinedClass);
      TestRun.getMockStates().removeClassState(redefinedClass, mockClassesInternalNames);
   }

   void restoreTransformedClasses(@NotNull Set<ClassIdentification> previousTransformedClasses)
   {
      if (!transformedClasses.isEmpty()) {
         Set<ClassIdentification> classesToRestore;

         if (previousTransformedClasses.isEmpty()) {
            classesToRestore = transformedClasses.keySet();
         }
         else {
            classesToRestore = getTransformedClasses();
            classesToRestore.removeAll(previousTransformedClasses);
         }

         if (!classesToRestore.isEmpty()) {
            restoreAndRemoveTransformedClasses(classesToRestore);
         }
      }
   }

   private void restoreAndRemoveTransformedClasses(@NotNull Set<ClassIdentification> classesToRestore)
   {
      RedefinitionEngine redefinitionEngine = new RedefinitionEngine();

      for (ClassIdentification transformedClassId : classesToRestore) {
         byte[] definitionToRestore = transformedClasses.get(transformedClassId);
         redefinitionEngine.restoreToDefinition(transformedClassId.getLoadedClass(), definitionToRestore);
      }

      transformedClasses.keySet().removeAll(classesToRestore);
   }

   void restoreRedefinedClasses(@NotNull Map<?, byte[]> previousDefinitions)
   {
      if (redefinedClasses.isEmpty()) {
         return;
      }

      RedefinitionEngine redefinitionEngine = new RedefinitionEngine();
      Iterator<Entry<Class<?>, byte[]>> itr = redefinedClasses.entrySet().iterator();

      while (itr.hasNext()) {
         Entry<Class<?>, byte[]> entry = itr.next();
         Class<?> redefinedClass = entry.getKey();
         byte[] currentDefinition = entry.getValue();
         byte[] previousDefinition = previousDefinitions.get(redefinedClass);

         //noinspection ArrayEquality
         if (currentDefinition != previousDefinition) {
            redefinitionEngine.restoreDefinition(redefinedClass, previousDefinition);

            if (previousDefinition == null) {
               restoreDefinition(redefinedClass);
               discardStateForCorrespondingMockClassIfAny(redefinedClass);
               itr.remove();
            }
            else {
               entry.setValue(previousDefinition);
            }
         }
      }
   }

   void removeMockedClasses(@NotNull List<Class<?>> previousMockedClasses)
   {
      int currentMockedClassCount = mockedClasses.size();

      if (currentMockedClassCount > 0) {
         int previousMockedClassCount = previousMockedClasses.size();

         if (previousMockedClassCount == 0) {
            mockedClasses.clear();
            mockedTypesAndInstances.clear();
         }
         else if (previousMockedClassCount < currentMockedClassCount) {
            mockedClasses.retainAll(previousMockedClasses);
            mockedTypesAndInstances.keySet().retainAll(previousMockedClasses);
         }
      }
   }

   // Methods that deal with redefined native methods /////////////////////////////////////////////////////////////////

   public void addRedefinedClassWithNativeMethods(@NotNull String redefinedClassInternalName)
   {
      redefinedClassesWithNativeMethods.add(redefinedClassInternalName.replace('/', '.'));
   }

   private static void reregisterNativeMethodsForRestoredClass(@NotNull Class<?> realClass)
   {
      Method registerNatives = null;

      try {
         registerNatives = realClass.getDeclaredMethod("registerNatives");
      }
      catch (NoSuchMethodException ignore) {
         try { registerNatives = realClass.getDeclaredMethod("initIDs"); }
         catch (NoSuchMethodException ignored) {} // OK
      }

      if (registerNatives != null) {
         try {
            registerNatives.setAccessible(true);
            registerNatives.invoke(null);
         }
         catch (IllegalAccessException ignore)    {} // won't happen
         catch (InvocationTargetException ignore) {} // shouldn't happen either
      }

      // OK, although another solution will be required for this particular class if it requires
      // natives to be explicitly registered again (not all do, such as java.lang.Float).
   }

   // Getter methods for the maps and collections of transformed/redefined/mocked classes /////////////////////////////

   @NotNull Set<ClassIdentification> getTransformedClasses()
   {
      return transformedClasses.isEmpty() ?
         Collections.<ClassIdentification>emptySet() :
         new HashSet<ClassIdentification>(transformedClasses.keySet());
   }

   @NotNull Map<Class<?>, byte[]> getRedefinedClasses()
   {
      return redefinedClasses.isEmpty() ?
         Collections.<Class<?>, byte[]>emptyMap() :
         new HashMap<Class<?>, byte[]>(redefinedClasses);
   }

   @Nullable public byte[] getRedefinedClassfile(@NotNull Class<?> redefinedClass)
   {
      return redefinedClasses.get(redefinedClass);
   }

   public boolean containsRedefinedClass(@NotNull Class<?> redefinedClass)
   {
      return redefinedClasses.containsKey(redefinedClass);
   }

   @NotNull public List<Class<?>> getMockedClasses()
   {
      return mockedClasses.isEmpty() ? Collections.<Class<?>>emptyList() : new ArrayList<Class<?>>(mockedClasses);
   }

   // Methods dealing with capture transformers ///////////////////////////////////////////////////////////////////////

   public void addCaptureTransformer(@NotNull CaptureTransformer<?> transformer)
   {
      captureTransformers.add(transformer);
   }

   public int getCaptureTransformerCount() { return captureTransformers.size(); }

   public void removeCaptureTransformers(int previousTransformerCount)
   {
      int currentTransformerCount = captureTransformers.size();

      for (int i = currentTransformerCount - 1; i >= previousTransformerCount; i--) {
         CaptureTransformer<?> transformer = captureTransformers.get(i);
         transformer.deactivate();
         Startup.instrumentation().removeTransformer(transformer);
         captureTransformers.remove(i);
      }
   }

   @Nullable
   public CaptureOfNewInstances findCaptureOfImplementations(@NotNull Class<?> baseType)
   {
      for (CaptureTransformer<?> captureTransformer : captureTransformers) {
         CaptureOfNewInstances capture = captureTransformer.getCaptureOfImplementationsIfApplicable(baseType);

         if (capture != null) {
            return capture;
         }
      }

      return null;
   }
}
