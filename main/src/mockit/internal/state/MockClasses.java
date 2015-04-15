/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
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
   static
   {
      INVOKED_INSTANCE_FIELD = FieldReflection.getDeclaredField(MockUp.class, "invokedInstance", true);
      INVOKED_INSTANCE_FIELD.setAccessible(true);
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
   public MockUpInstances removeMock(@Nonnull MockUp<?> mockUp, @Nullable Object mockedInstance)
   {
      Class<?> mockUpClass = mockUp.getClass();

      if (mockedInstance == null) {
         MockUpInstances mockUpInstances = mockupClassesToMockupInstances.remove(mockUpClass);
         assert !mockUpInstances.hasMockUpsForSingleInstances();
         return mockUpInstances;
      }

      MockUp<?> previousMockUpForMockedInstance = mockedToMockupInstances.remove(mockedInstance);
      assert previousMockUpForMockedInstance == mockUp;

      MockUpInstances mockUpInstances = mockupClassesToMockupInstances.get(mockUpClass);
      int decrementedNumberOfMockupsForSingleInstance = --mockUpInstances.numberOfMockupsForSingleInstance;

      if (decrementedNumberOfMockupsForSingleInstance == 0) {
         mockupClassesToMockupInstances.remove(mockUpClass);
      }

      return mockUpInstances;
   }

   final class SavePoint
   {
      @Nonnull private final Map<Object, MockUp<?>> previousMockInstances;
      @Nonnull private final Map<Class<?>, Integer> previousMockUpClassesAndMockupCounts;

      SavePoint()
      {
         previousMockInstances = new IdentityHashMap<Object, MockUp<?>>(mockedToMockupInstances);
         previousMockUpClassesAndMockupCounts = new IdentityHashMap<Class<?>, Integer>();

         for (Entry<Class<?>, MockUpInstances> mockUpClassAndData : mockupClassesToMockupInstances.entrySet()) {
            Class<?> mockUpClass = mockUpClassAndData.getKey();
            MockUpInstances mockUpData = mockUpClassAndData.getValue();
            previousMockUpClassesAndMockupCounts.put(mockUpClass, mockUpData.numberOfMockupsForSingleInstance);
         }
      }

      void rollback()
      {
         mockedToMockupInstances.entrySet().retainAll(previousMockInstances.entrySet());

         for (Entry<Class<?>, Integer> mockUpClassAndCount : previousMockUpClassesAndMockupCounts.entrySet()) {
            Class<?> mockUpClass = mockUpClassAndCount.getKey();
            MockUpInstances mockUpData = mockupClassesToMockupInstances.get(mockUpClass);
            mockUpData.numberOfMockupsForSingleInstance = mockUpClassAndCount.getValue();
         }

         mockupClassesToMockupInstances.keySet().retainAll(previousMockUpClassesAndMockupCounts.keySet());
      }
   }
}
