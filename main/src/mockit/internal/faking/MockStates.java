/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.faking;

import java.util.*;
import java.util.Map.*;
import java.util.regex.*;
import javax.annotation.*;

import mockit.internal.util.*;

/**
 * Holds state associated with fake class containing {@linkplain mockit.Mock annotated fakes}.
 */
public final class MockStates
{
   private static final Pattern SPACE = Pattern.compile(" ");

   /**
    * For each fake instance and each {@code @Mock} method containing the {@code Invocation} parameter, a runtime state
    * will be kept here.
    */
   @Nonnull private final Map<Object, List<MockState>> fakesToFakeStates;
   @Nonnull private final Map<Object, List<MockState>> startupFakesToFakeStates;

   public MockStates()
   {
      startupFakesToFakeStates = new IdentityHashMap<Object, List<MockState>>(2);
      fakesToFakeStates = new IdentityHashMap<Object, List<MockState>>(8);
   }

   void addStartupFakeAndItsFakeStates(@Nonnull Object fake, @Nonnull List<MockState> fakeStates)
   {
      startupFakesToFakeStates.put(fake, fakeStates);
   }

   void addFakeAndItsFakeStates(@Nonnull Object fake, @Nonnull List<MockState> fakeStates)
   {
      fakesToFakeStates.put(fake, fakeStates);
   }

   public void copyMockStates(@Nonnull Object previousFake, @Nonnull Object newFake)
   {
      List<MockState> mockStates = fakesToFakeStates.get(previousFake);

      if (mockStates != null) {
         List<MockState> copiedMockStates = new ArrayList<MockState>(mockStates.size());

         for (MockState mockState : mockStates) {
            copiedMockStates.add(new MockState(mockState));
         }

         fakesToFakeStates.put(newFake, copiedMockStates);
      }
   }

   public void removeClassState(@Nonnull Class<?> redefinedClass, @Nullable String internalNameForOneOrMoreFakeClasses)
   {
      removeFakeStates(redefinedClass);

      if (internalNameForOneOrMoreFakeClasses != null) {
         if (internalNameForOneOrMoreFakeClasses.indexOf(' ') < 0) {
            removeFakeStates(internalNameForOneOrMoreFakeClasses);
         }
         else {
            String[] mockClassesInternalNames = SPACE.split(internalNameForOneOrMoreFakeClasses);

            for (String mockClassInternalName : mockClassesInternalNames) {
               removeFakeStates(mockClassInternalName);
            }
         }
      }
   }

   private void removeFakeStates(@Nonnull Class<?> redefinedClass)
   {
      Iterator<List<MockState>> itr = fakesToFakeStates.values().iterator();

      while (itr.hasNext()) {
         List<MockState> fakeStates = itr.next();
         MockState fakeState = fakeStates.get(0);

         if (fakeState.getRealClass() == redefinedClass) {
            fakeStates.clear();
            itr.remove();
         }
      }
   }

   private void removeFakeStates(@Nonnull String fakeClassInternalName)
   {
      Class<?> fakeClass = ClassLoad.loadClass(fakeClassInternalName.replace('/', '.'));
      Iterator<Entry<Object, List<MockState>>> itr = fakesToFakeStates.entrySet().iterator();

      while (itr.hasNext()) {
         Entry<Object, List<MockState>> fakeAndFakeStates = itr.next();
         Object fake = fakeAndFakeStates.getKey();

         if (fake.getClass() == fakeClass) {
            itr.remove();
         }
      }
   }

   public boolean updateMockState(@Nonnull Object fake, int mockStateIndex)
   {
      MockState mockState = getMockState(fake, mockStateIndex);
      return mockState.update();
   }

   @Nonnull
   MockState getMockState(@Nonnull Object fake, int mockStateIndex)
   {
      List<MockState> mockStates = startupFakesToFakeStates.get(fake);

      if (mockStates == null) {
         mockStates = fakesToFakeStates.get(fake);
      }

      MockState mockState = mockStates.get(mockStateIndex);
      assert mockState != null;
      return mockState;
   }
}
