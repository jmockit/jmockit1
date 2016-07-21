/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.state;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.*;
import javax.annotation.*;

import mockit.*;
import mockit.internal.mockups.*;
import mockit.internal.util.*;

public final class MockClasses
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
      int numberOfMockupsForSingleInstance;

      MockUpInstances(@Nonnull MockUp<?> initialMockUp)
      {
         this.initialMockUp = initialMockUp;
         numberOfMockupsForSingleInstance = 0;
      }

      public boolean hasMockUpsForSingleInstances() { return numberOfMockupsForSingleInstance > 0; }

      void notifyMockUpOfTearDown() { notifyOfTearDown(initialMockUp); }
   }

   @Nonnull private final Map<String, MockUp<?>> startupMocks;
   @Nonnull private final Map<Class<?>, MockUpInstances> mockupClassesToMockupInstances;
   @Nonnull private final Map<Object, MockUp<?>> mockedToMockupInstances;
   @Nonnull public final MockStates mockStates;

   MockClasses()
   {
      startupMocks = new IdentityHashMap<String, MockUp<?>>(8);
      mockupClassesToMockupInstances = new IdentityHashMap<Class<?>, MockUpInstances>();
      mockedToMockupInstances = new IdentityHashMap<Object, MockUp<?>>();
      mockStates = new MockStates();
   }

   public void addMock(@Nonnull String mockClassDesc, @Nonnull MockUp<?> mockUp)
   {
      startupMocks.put(mockClassDesc, mockUp);
   }

   public void addMock(@Nonnull MockUp<?> mockUp)
   {
      Class<?> mockUpClass = mockUp.getClass();
      MockUpInstances newData = new MockUpInstances(mockUp);
      MockUpInstances previousData = mockupClassesToMockupInstances.put(mockUpClass, newData);
      assert previousData == null;
   }

   public void addMock(@Nonnull MockUp<?> mockUp, @Nonnull Object mockedInstance)
   {
      MockUp<?> previousMockup = mockedToMockupInstances.put(mockedInstance, mockUp);
      assert previousMockup == null;

      MockUpInstances mockUpInstances = mockupClassesToMockupInstances.get(mockUp.getClass());
      mockUpInstances.numberOfMockupsForSingleInstance++;
   }

   @Nullable
   MockUp<?> getMock(@Nonnull String mockUpClassDesc, @Nullable Object mockedInstance)
   {
      if (mockedInstance != null) {
         MockUp<?> mockUpForSingleInstance = mockedToMockupInstances.get(mockedInstance);

         if (mockUpForSingleInstance != null) {
            return mockUpForSingleInstance;
         }
      }

      MockUp<?> startupMock = startupMocks.get(mockUpClassDesc);

      if (startupMock != null) {
         return startupMock;
      }

      Class<?> mockUpClass = ClassLoad.loadByInternalName(mockUpClassDesc);
      MockUpInstances mockUpInstances = mockupClassesToMockupInstances.get(mockUpClass);
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
   public MockUpInstances findPreviouslyAppliedMockUps(@Nonnull MockUp<?> newMockUp)
   {
      Class<?> mockUpClass = newMockUp.getClass();
      MockUpInstances mockUpInstances = mockupClassesToMockupInstances.get(mockUpClass);

      if (mockUpInstances != null && mockUpInstances.hasMockUpsForSingleInstances()) {
         mockStates.copyMockStates(mockUpInstances.initialMockUp, newMockUp);
      }

      return mockUpInstances;
   }

   @Nonnull
   public MockUpInstances removeMock(@Nonnull MockUp<?> mockUp)
   {
      Class<?> mockUpClass = mockUp.getClass();
      MockUpInstances mockUpInstances = mockupClassesToMockupInstances.remove(mockUpClass);
      return mockUpInstances;
   }

   private void discardMockupInstances(@Nonnull Map<Object, MockUp<?>> previousMockInstances)
   {
      if (!previousMockInstances.isEmpty()) {
         mockedToMockupInstances.entrySet().retainAll(previousMockInstances.entrySet());
      }
      else if (!mockedToMockupInstances.isEmpty()) {
         mockedToMockupInstances.clear();
      }
   }

   private void discardMockupInstancesExceptPreviousOnes(
      @Nonnull Map<Class<?>, Integer> previousMockupClassesAndMockupCounts)
   {
      updateNumberOfMockupsForSingleInstanceForPreviousMockups(previousMockupClassesAndMockupCounts);

      for (Entry<Class<?>, MockUpInstances> mockupClassAndInstances : mockupClassesToMockupInstances.entrySet()) {
         Class<?> mockupClass = mockupClassAndInstances.getKey();

         if (!previousMockupClassesAndMockupCounts.containsKey(mockupClass)) {
            MockUpInstances mockUpInstances = mockupClassAndInstances.getValue();
            mockUpInstances.notifyMockUpOfTearDown();
         }
      }

      mockupClassesToMockupInstances.keySet().retainAll(previousMockupClassesAndMockupCounts.keySet());
   }

   private void discardAllMockupInstances()
   {
      if (!mockupClassesToMockupInstances.isEmpty()) {
         for (MockUpInstances mockUpInstances : mockupClassesToMockupInstances.values()) {
            mockUpInstances.notifyMockUpOfTearDown();
         }

         mockupClassesToMockupInstances.clear();
      }
   }

   private void updateNumberOfMockupsForSingleInstanceForPreviousMockups(
      @Nonnull Map<Class<?>, Integer> previousMockupClassesAndMockupCounts)
   {
      for (Entry<Class<?>, Integer> mockUpClassAndCount : previousMockupClassesAndMockupCounts.entrySet()) {
         Class<?> mockupClass = mockUpClassAndCount.getKey();
         Integer mockupCount = mockUpClassAndCount.getValue();

         MockUpInstances mockUpData = mockupClassesToMockupInstances.get(mockupClass);
         mockUpData.numberOfMockupsForSingleInstance = mockupCount;
      }
   }

   public void discardStartupMocks()
   {
      for (MockUp<?> startupMockup : startupMocks.values()) {
         notifyOfTearDown(startupMockup);
      }
   }

   final class SavePoint
   {
      @Nonnull private final Map<Object, MockUp<?>> previousMockInstances;
      @Nonnull private final Map<Class<?>, Integer> previousMockupClassesAndMockupCounts;

      SavePoint()
      {
         previousMockInstances = new IdentityHashMap<Object, MockUp<?>>(mockedToMockupInstances);
         previousMockupClassesAndMockupCounts = new IdentityHashMap<Class<?>, Integer>();

         for (Entry<Class<?>, MockUpInstances> mockUpClassAndData : mockupClassesToMockupInstances.entrySet()) {
            Class<?> mockUpClass = mockUpClassAndData.getKey();
            MockUpInstances mockUpData = mockUpClassAndData.getValue();
            previousMockupClassesAndMockupCounts.put(mockUpClass, mockUpData.numberOfMockupsForSingleInstance);
         }
      }

      void rollback()
      {
         discardMockupInstances(previousMockInstances);

         if (!previousMockupClassesAndMockupCounts.isEmpty()) {
            discardMockupInstancesExceptPreviousOnes(previousMockupClassesAndMockupCounts);
         }
         else {
            discardAllMockupInstances();
         }
      }
   }
}
