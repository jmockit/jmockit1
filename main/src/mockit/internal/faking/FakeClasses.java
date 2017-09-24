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
   private static final Field INVOKED_INSTANCE_FIELD;
   private static final Method ON_TEAR_DOWN_METHOD;
   static
   {
      try {
         INVOKED_INSTANCE_FIELD = MockUp.class.getDeclaredField("invokedInstance");
         INVOKED_INSTANCE_FIELD.setAccessible(true);

         ON_TEAR_DOWN_METHOD = MockUp.class.getDeclaredMethod("onTearDown");
         ON_TEAR_DOWN_METHOD.setAccessible(true);
      }
      catch (NoSuchFieldException e)  { throw new RuntimeException(e); }
      catch (NoSuchMethodException e) { throw new RuntimeException(e); }
   }

   private static void notifyOfTearDown(@Nonnull MockUp<?> fake)
   {
      try { ON_TEAR_DOWN_METHOD.invoke(fake); }
      catch (IllegalAccessException ignore) {}
      catch (InvocationTargetException e) { e.getCause().printStackTrace(); }
   }

   public static final class FakeInstances
   {
      @Nonnull public final MockUp<?> initialFake;
      FakeInstances(@Nonnull MockUp<?> initialFake) { this.initialFake = initialFake; }
      void notifyMockUpOfTearDown() { notifyOfTearDown(initialFake); }
   }

   @Nonnull private final Map<String, MockUp<?>> startupFakes;
   @Nonnull private final Map<Class<?>, FakeInstances> fakeClassesToFakeInstances;
   @Nonnull public final FakeStates fakeStates;

   public FakeClasses()
   {
      startupFakes = new IdentityHashMap<String, MockUp<?>>(8);
      fakeClassesToFakeInstances = new IdentityHashMap<Class<?>, FakeInstances>();
      fakeStates = new FakeStates();
   }

   public void addFake(@Nonnull String fakeClassDesc, @Nonnull MockUp<?> fake)
   {
      startupFakes.put(fakeClassDesc, fake);
   }

   public void addFake(@Nonnull MockUp<?> fake)
   {
      Class<?> fakeClass = fake.getClass();
      FakeInstances newData = new FakeInstances(fake);
      fakeClassesToFakeInstances.put(fakeClass, newData);
   }

   @Nonnull
   public MockUp<?> getFake(@Nonnull String mockUpClassDesc, @Nullable Object mockedInstance)
   {
      MockUp<?> startupMock = startupFakes.get(mockUpClassDesc);

      if (startupMock != null) {
         return startupMock;
      }

      Class<?> mockUpClass = ClassLoad.loadByInternalName(mockUpClassDesc);
      FakeInstances fakeInstances = fakeClassesToFakeInstances.get(mockUpClass);
      Object invokedInstance = mockedInstance;

      if (mockedInstance == null) {
         invokedInstance = Void.class;
      }

      try { INVOKED_INSTANCE_FIELD.set(fakeInstances.initialFake, invokedInstance); }
      catch (IllegalAccessException ignore) {}

      return fakeInstances.initialFake;
   }

   @Nullable
   public FakeInstances findPreviouslyAppliedFakes(@Nonnull MockUp<?> newFake)
   {
      Class<?> fakeClass = newFake.getClass();
      FakeInstances fakeInstances = fakeClassesToFakeInstances.get(fakeClass);

      if (fakeInstances != null) {
         fakeStates.copyFakeStates(fakeInstances.initialFake, newFake);
      }

      return fakeInstances;
   }

   private void discardFakeInstancesExceptPreviousOnes(@Nonnull Map<Class<?>, Boolean> previousFakeClasses)
   {
      for (Entry<Class<?>, FakeInstances> fakeClassAndInstances : fakeClassesToFakeInstances.entrySet()) {
         Class<?> fakeClass = fakeClassAndInstances.getKey();

         if (!previousFakeClasses.containsKey(fakeClass)) {
            FakeInstances fakeInstances = fakeClassAndInstances.getValue();
            fakeInstances.notifyMockUpOfTearDown();
         }
      }

      fakeClassesToFakeInstances.keySet().retainAll(previousFakeClasses.keySet());
   }

   private void discardAllFakeInstances()
   {
      if (!fakeClassesToFakeInstances.isEmpty()) {
         for (FakeInstances fakeInstances : fakeClassesToFakeInstances.values()) {
            fakeInstances.notifyMockUpOfTearDown();
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

         for (Entry<Class<?>, FakeInstances> fakeClassAndData : fakeClassesToFakeInstances.entrySet()) {
            Class<?> fakeClass = fakeClassAndData.getKey();
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
