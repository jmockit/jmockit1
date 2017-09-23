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

   private static void notifyOfTearDown(@Nonnull MockUp<?> mockUp)
   {
      try { ON_TEAR_DOWN_METHOD.invoke(mockUp); }
      catch (IllegalAccessException ignore) {}
      catch (InvocationTargetException e) { e.getCause().printStackTrace(); }
   }

   public static final class MockUpInstances
   {
      @Nonnull public final MockUp<?> initialMockUp;
      boolean hasMockupsForSingleInstances;

      MockUpInstances(@Nonnull MockUp<?> initialMockUp)
      {
         this.initialMockUp = initialMockUp;
         hasMockupsForSingleInstances = false;
      }

      public boolean hasMockUpsForSingleInstances() { return hasMockupsForSingleInstances; }

      void notifyMockUpOfTearDown() { notifyOfTearDown(initialMockUp); }
   }

   @Nonnull private final Map<String, MockUp<?>> startupFakes;
   @Nonnull private final Map<Class<?>, MockUpInstances> fakeClassesToFakeInstances;
   @Nonnull private final Map<Object, MockUp<?>> fakedToFakeInstances;
   @Nonnull public final MockStates fakeStates;

   public FakeClasses()
   {
      startupFakes = new IdentityHashMap<String, MockUp<?>>(8);
      fakeClassesToFakeInstances = new IdentityHashMap<Class<?>, MockUpInstances>();
      fakedToFakeInstances = new IdentityHashMap<Object, MockUp<?>>();
      fakeStates = new MockStates();
   }

   public void addFake(@Nonnull String fakeClassDesc, @Nonnull MockUp<?> fake)
   {
      startupFakes.put(fakeClassDesc, fake);
   }

   public void addFake(@Nonnull MockUp<?> fake)
   {
      Class<?> fakeClass = fake.getClass();
      MockUpInstances newData = new MockUpInstances(fake);
      fakeClassesToFakeInstances.put(fakeClass, newData);
   }

   public void addFake(@Nonnull MockUp<?> fake, @Nonnull Object fakedInstance)
   {
      MockUp<?> previousFake = fakedToFakeInstances.put(fakedInstance, fake);
      assert previousFake == null;

      MockUpInstances fakeInstances = fakeClassesToFakeInstances.get(fake.getClass());
      fakeInstances.hasMockupsForSingleInstances = true;
   }

   @Nullable
   public MockUp<?> getFake(@Nonnull String mockUpClassDesc, @Nullable Object mockedInstance)
   {
      if (mockedInstance != null) {
         MockUp<?> mockUpForSingleInstance = fakedToFakeInstances.get(mockedInstance);

         if (mockUpForSingleInstance != null) {
            return mockUpForSingleInstance;
         }
      }

      MockUp<?> startupMock = startupFakes.get(mockUpClassDesc);

      if (startupMock != null) {
         return startupMock;
      }

      Class<?> mockUpClass = ClassLoad.loadByInternalName(mockUpClassDesc);
      MockUpInstances mockUpInstances = fakeClassesToFakeInstances.get(mockUpClass);
      Object invokedInstance = mockedInstance;

      if (mockedInstance == null) {
         invokedInstance = Void.class;
      }
      else if (mockUpInstances.hasMockUpsForSingleInstances()) {
         return null;
      }

      try { INVOKED_INSTANCE_FIELD.set(mockUpInstances.initialMockUp, invokedInstance); }
      catch (IllegalAccessException ignore) {}

      return mockUpInstances.initialMockUp;
   }

   @Nullable
   public MockUpInstances findPreviouslyAppliedFakes(@Nonnull MockUp<?> newFake)
   {
      Class<?> mockUpClass = newFake.getClass();
      MockUpInstances mockUpInstances = fakeClassesToFakeInstances.get(mockUpClass);

      if (mockUpInstances != null && mockUpInstances.hasMockupsForSingleInstances) {
         fakeStates.copyMockStates(mockUpInstances.initialMockUp, newFake);
      }

      return mockUpInstances;
   }

   private void discardFakeInstances(@Nonnull Map<Object, MockUp<?>> previousFakeInstances)
   {
      if (!previousFakeInstances.isEmpty()) {
         fakedToFakeInstances.entrySet().retainAll(previousFakeInstances.entrySet());
      }
      else if (!fakedToFakeInstances.isEmpty()) {
         fakedToFakeInstances.clear();
      }
   }

   private void discardFakeInstancesExceptPreviousOnes(@Nonnull Map<Class<?>, Boolean> previousFakeClasses)
   {
      updatePreviousFakes(previousFakeClasses);

      for (Entry<Class<?>, MockUpInstances> fakeClassAndInstances : fakeClassesToFakeInstances.entrySet()) {
         Class<?> fakeClass = fakeClassAndInstances.getKey();

         if (!previousFakeClasses.containsKey(fakeClass)) {
            MockUpInstances mockUpInstances = fakeClassAndInstances.getValue();
            mockUpInstances.notifyMockUpOfTearDown();
         }
      }

      fakeClassesToFakeInstances.keySet().retainAll(previousFakeClasses.keySet());
   }

   private void updatePreviousFakes(@Nonnull Map<Class<?>, Boolean> previousFakeClasses)
   {
      for (Entry<Class<?>, Boolean> fakeClassAndData : previousFakeClasses.entrySet()) {
         Class<?> fakeClass = fakeClassAndData.getKey();
         MockUpInstances fakeData = fakeClassesToFakeInstances.get(fakeClass);
         fakeData.hasMockupsForSingleInstances = fakeClassAndData.getValue();
      }
   }

   private void discardAllFakeInstances()
   {
      if (!fakeClassesToFakeInstances.isEmpty()) {
         for (MockUpInstances mockUpInstances : fakeClassesToFakeInstances.values()) {
            mockUpInstances.notifyMockUpOfTearDown();
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
      @Nonnull private final Map<Object, MockUp<?>> previousFakeInstances;
      @Nonnull private final Map<Class<?>, Boolean> previousFakeClasses;

      public SavePoint()
      {
         previousFakeInstances = new IdentityHashMap<Object, MockUp<?>>(fakedToFakeInstances);
         previousFakeClasses = new IdentityHashMap<Class<?>, Boolean>();

         for (Entry<Class<?>, MockUpInstances> fakeClassAndData : fakeClassesToFakeInstances.entrySet()) {
            Class<?> fakeClass = fakeClassAndData.getKey();
            MockUpInstances fakeData = fakeClassAndData.getValue();
            previousFakeClasses.put(fakeClass, fakeData.hasMockupsForSingleInstances);
         }
      }

      public void rollback()
      {
         discardFakeInstances(previousFakeInstances);

         if (!previousFakeClasses.isEmpty()) {
            discardFakeInstancesExceptPreviousOnes(previousFakeClasses);
         }
         else {
            discardAllFakeInstances();
         }
      }
   }
}
