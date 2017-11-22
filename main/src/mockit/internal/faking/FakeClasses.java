/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.faking;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.*;
import javax.annotation.*;

import mockit.*;
import mockit.internal.util.*;

public final class FakeClasses
{
   private static final Method ON_TEAR_DOWN_METHOD;
   static
   {
      try {
         ON_TEAR_DOWN_METHOD = MockUp.class.getDeclaredMethod("onTearDown");
         ON_TEAR_DOWN_METHOD.setAccessible(true);
      }
      catch (NoSuchMethodException e) { throw new RuntimeException(e); }
   }

   private static void notifyOfTearDown(@Nonnull MockUp<?> fake)
   {
      try { ON_TEAR_DOWN_METHOD.invoke(fake); }
      catch (IllegalAccessException ignore) {}
      catch (InvocationTargetException e) { e.getCause().printStackTrace(); }
   }

   @Nonnull private final Map<String, MockUp<?>> startupFakes;
   @Nonnull private final Map<Class<?>, MockUp<?>> fakeClassesToFakeInstances;
   @Nonnull public final FakeStates fakeStates;

   public FakeClasses()
   {
      startupFakes = new IdentityHashMap<String, MockUp<?>>(8);
      fakeClassesToFakeInstances = new IdentityHashMap<Class<?>, MockUp<?>>();
      fakeStates = new FakeStates();
   }

   void addFake(@Nonnull String fakeClassDesc, @Nonnull MockUp<?> fake)
   {
      startupFakes.put(fakeClassDesc, fake);
   }

   void addFake(@Nonnull MockUp<?> fake)
   {
      Class<?> fakeClass = fake.getClass();
      fakeClassesToFakeInstances.put(fakeClass, fake);
   }

   @Nonnull
   public MockUp<?> getFake(@Nonnull String fakeClassDesc)
   {
      MockUp<?> startupFake = startupFakes.get(fakeClassDesc);

      if (startupFake != null) {
         return startupFake;
      }

      Class<?> fakeClass = ClassLoad.loadByInternalName(fakeClassDesc);
      MockUp<?> fakeInstance = fakeClassesToFakeInstances.get(fakeClass);
      return fakeInstance;
   }

   @Nullable
   public MockUp<?> findPreviouslyAppliedFake(@Nonnull MockUp<?> newFake)
   {
      Class<?> fakeClass = newFake.getClass();
      MockUp<?> fakeInstance = fakeClassesToFakeInstances.get(fakeClass);
      return fakeInstance;
   }

   private void discardFakeInstancesExceptPreviousOnes(@Nonnull Map<Class<?>, Boolean> previousFakeClasses)
   {
      for (Entry<Class<?>, MockUp<?>> fakeClassAndInstances : fakeClassesToFakeInstances.entrySet()) {
         Class<?> fakeClass = fakeClassAndInstances.getKey();

         if (!previousFakeClasses.containsKey(fakeClass)) {
            MockUp<?> fakeInstance = fakeClassAndInstances.getValue();
            notifyOfTearDown(fakeInstance);
         }
      }

      fakeClassesToFakeInstances.keySet().retainAll(previousFakeClasses.keySet());
   }

   private void discardAllFakeInstances()
   {
      if (!fakeClassesToFakeInstances.isEmpty()) {
         for (MockUp<?> fakeInstance : fakeClassesToFakeInstances.values()) {
            notifyOfTearDown(fakeInstance);
         }

         fakeClassesToFakeInstances.clear();
      }
   }

   public void discardStartupFakes()
   {
      for (MockUp<?> startupFake : startupFakes.values()) {
         notifyOfTearDown(startupFake);
      }
   }

   public final class SavePoint
   {
      @Nonnull private final Map<Class<?>, Boolean> previousFakeClasses;

      public SavePoint()
      {
         previousFakeClasses = new IdentityHashMap<Class<?>, Boolean>();

         for (Entry<Class<?>, MockUp<?>> fakeClassAndInstance : fakeClassesToFakeInstances.entrySet()) {
            Class<?> fakeClass = fakeClassAndInstance.getKey();
            previousFakeClasses.put(fakeClass, false);
         }
      }

      public void rollback()
      {
         if (!previousFakeClasses.isEmpty()) {
            discardFakeInstancesExceptPreviousOnes(previousFakeClasses);
         }
         else {
            discardAllFakeInstances();
         }
      }
   }
}
