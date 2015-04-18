/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.mockups;

import java.util.*;
import java.util.Map.*;
import java.util.regex.*;
import javax.annotation.*;

import mockit.internal.util.*;

/**
 * Holds state associated with mock class containing {@linkplain mockit.Mock annotated mocks}.
 */
public final class MockStates
{
   private static final Pattern SPACE = Pattern.compile(" ");

   /**
    * For each mockup instance and each {@code @Mock} method containing the {@code Invocation} parameter or an
    * invocation count constraint, a runtime state will be kept here.
    */
   @Nonnull private final Map<Object, List<MockState>> mockUpsToMockStates;
   @Nonnull private final Map<Object, List<MockState>> startupMockUpsToMockStates;

   /**
    * For each annotated mock method with at least one invocation expectation, its mock state will
    * also be kept here, as an optimization.
    */
   @Nonnull private final Set<MockState> mockStatesWithExpectations;

   public MockStates()
   {
      startupMockUpsToMockStates = new IdentityHashMap<Object, List<MockState>>(2);
      mockUpsToMockStates = new IdentityHashMap<Object, List<MockState>>(8);
      mockStatesWithExpectations = new LinkedHashSet<MockState>(10);
   }

   void addStartupMockUpAndItsMockStates(@Nonnull Object mockUp, @Nonnull List<MockState> mockStates)
   {
      startupMockUpsToMockStates.put(mockUp, mockStates);
   }

   void addMockStates(@Nonnull Iterable<MockState> mockStates)
   {
      for (MockState mockState : mockStates) {
         if (mockState.isWithExpectations()) {
            mockStatesWithExpectations.add(mockState);
         }
      }
   }

   void addMockUpAndItsMockStates(@Nonnull Object mockUp, @Nonnull List<MockState> mockStates)
   {
      mockUpsToMockStates.put(mockUp, mockStates);
   }

   public void copyMockStates(@Nonnull Object previousMockUp, @Nonnull Object newMockUp)
   {
      List<MockState> mockStates = mockUpsToMockStates.get(previousMockUp);

      if (mockStates != null) {
         List<MockState> copiedMockStates = new ArrayList<MockState>(mockStates.size());

         for (MockState mockState : mockStates) {
            copiedMockStates.add(new MockState(mockState));
         }

         mockUpsToMockStates.put(newMockUp, copiedMockStates);
      }
   }

   public void removeClassState(@Nonnull Class<?> redefinedClass, @Nullable String internalNameForOneOrMoreMockClasses)
   {
      removeMockStates(redefinedClass);

      if (internalNameForOneOrMoreMockClasses != null) {
         if (internalNameForOneOrMoreMockClasses.indexOf(' ') < 0) {
            removeMockStates(internalNameForOneOrMoreMockClasses);
         }
         else {
            String[] mockClassesInternalNames = SPACE.split(internalNameForOneOrMoreMockClasses);

            for (String mockClassInternalName : mockClassesInternalNames) {
               removeMockStates(mockClassInternalName);
            }
         }
      }
   }

   private void removeMockStates(@Nonnull Class<?> redefinedClass)
   {
      for (Iterator<List<MockState>> itr = mockUpsToMockStates.values().iterator(); itr.hasNext(); ) {
         List<MockState> mockStates = itr.next();
         MockState mockState = mockStates.get(0);

         if (mockState.getRealClass() == redefinedClass) {
            mockStatesWithExpectations.removeAll(mockStates);
            mockStates.clear();
            itr.remove();
         }
      }
   }

   private void removeMockStates(@Nonnull String mockClassInternalName)
   {
      Class<?> mockUpClass = ClassLoad.loadClass(mockClassInternalName.replace('/', '.'));

      for (Iterator<Entry<Object, List<MockState>>> itr = mockUpsToMockStates.entrySet().iterator(); itr.hasNext(); ) {
         Entry<Object, List<MockState>> mockUpAndMockStates = itr.next();
         Object mockUp = mockUpAndMockStates.getKey();

         if (mockUp.getClass() == mockUpClass) {
            List<MockState> mockStates = mockUpAndMockStates.getValue();

            if (mockStates != null) {
               mockStatesWithExpectations.removeAll(mockStates);
            }

            itr.remove();
         }
      }
   }

   public boolean updateMockState(@Nonnull Object mockUp, int mockStateIndex)
   {
      MockState mockState = getMockState(mockUp, mockStateIndex);
      return mockState.update();
   }

   @Nonnull
   MockState getMockState(@Nonnull Object mockUp, int mockStateIndex)
   {
      List<MockState> mockStates = startupMockUpsToMockStates.get(mockUp);

      if (mockStates == null) {
         mockStates = mockUpsToMockStates.get(mockUp);
      }

      MockState mockState = mockStates.get(mockStateIndex);
      assert mockState != null;
      return mockState;
   }

   public void verifyMissingInvocations()
   {
      for (MockState mockState : mockStatesWithExpectations) {
         mockState.verifyMissingInvocations();
      }
   }

   public void resetExpectations()
   {
      for (MockState mockState : mockStatesWithExpectations) {
         mockState.reset();
      }
   }
}
