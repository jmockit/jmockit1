/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.state;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.*;

import mockit.*;
import mockit.internal.mockups.*;
import mockit.internal.util.*;

import org.jetbrains.annotations.*;

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
      @NotNull public final MockUp<?> initialMockUp;
      int numberOfMockupsForSingleInstance;

      MockUpInstances(@NotNull MockUp<?> initialMockUp)
      {
         this.initialMockUp = initialMockUp;
         numberOfMockupsForSingleInstance = 0;
      }

      public boolean hasMockUpsForSingleInstances() { return numberOfMockupsForSingleInstance > 0; }
   }

   @NotNull private final Map<String, MockUp<?>> startupMocks;
   @NotNull private final Map<Class<?>, MockUpInstances> mockupClassesToMockupInstances;
   @NotNull private final Map<Object, MockUp<?>> mockedToMockupInstances;
   @NotNull public final MockStates mockStates;

   MockClasses()
   {
      startupMocks = new IdentityHashMap<String, MockUp<?>>(8);
      mockupClassesToMockupInstances = new IdentityHashMap<Class<?>, MockUpInstances>();
      mockedToMockupInstances = new IdentityHashMap<Object, MockUp<?>>();
      mockStates = new MockStates();
   }

   public void addMock(@NotNull String mockClassDesc, @NotNull MockUp<?> mockUp)
   {
      startupMocks.put(mockClassDesc, mockUp);
   }

   public void addMock(@NotNull MockUp<?> mockUp)
   {
      Class<?> mockUpClass = mockUp.getClass();
      MockUpInstances newData = new MockUpInstances(mockUp);
      MockUpInstances previousData = mockupClassesToMockupInstances.put(mockUpClass, newData);
      assert previousData == null;
   }

   public void addMock(@NotNull MockUp<?> mockUp, @NotNull Object mockedInstance)
   {
      MockUp<?> previousMockup = mockedToMockupInstances.put(mockedInstance, mockUp);
      assert previousMockup == null;

      MockUpInstances mockUpInstances = mockupClassesToMockupInstances.get(mockUp.getClass());
      mockUpInstances.numberOfMockupsForSingleInstance++;
   }

   @Nullable
   MockUp<?> getMock(@NotNull String mockUpClassDesc, @Nullable Object mockedInstance)
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
   public MockUpInstances findPreviouslyAppliedMockUps(@NotNull MockUp<?> newMockUp)
   {
      Class<?> mockUpClass = newMockUp.getClass();
      MockUpInstances mockUpInstances = mockupClassesToMockupInstances.get(mockUpClass);

      if (mockUpInstances != null && mockUpInstances.hasMockUpsForSingleInstances()) {
         mockStates.copyMockStates(mockUpInstances.initialMockUp, newMockUp);
      }

      return mockUpInstances;
   }

   @NotNull
   public MockUpInstances removeMock(@NotNull MockUp<?> mockUp, @Nullable Object mockedInstance)
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
      @NotNull private final Set<Object> previousMockInstances;
      @NotNull private final Map<Class<?>, Integer> previousMockUpClassesAndMockupCounts;

      SavePoint()
      {
         previousMockInstances = new HashSet<Object>(mockedToMockupInstances.keySet());
         previousMockUpClassesAndMockupCounts = new IdentityHashMap<Class<?>, Integer>();

         for (Entry<Class<?>, MockUpInstances> mockUpClassAndData : mockupClassesToMockupInstances.entrySet()) {
            Class<?> mockUpClass = mockUpClassAndData.getKey();
            MockUpInstances mockUpData = mockUpClassAndData.getValue();
            previousMockUpClassesAndMockupCounts.put(mockUpClass, mockUpData.numberOfMockupsForSingleInstance);
         }
      }

      void rollback()
      {
         mockedToMockupInstances.keySet().retainAll(previousMockInstances);

         for (Entry<Class<?>, Integer> mockUpClassAndCount : previousMockUpClassesAndMockupCounts.entrySet()) {
            Class<?> mockUpClass = mockUpClassAndCount.getKey();
            MockUpInstances mockUpData = mockupClassesToMockupInstances.get(mockUpClass);
            mockUpData.numberOfMockupsForSingleInstance = mockUpClassAndCount.getValue();
         }

         mockupClassesToMockupInstances.keySet().retainAll(previousMockUpClassesAndMockupCounts.keySet());
      }
   }
}
