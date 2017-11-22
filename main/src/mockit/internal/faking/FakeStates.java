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
public final class FakeStates
{
   private static final Pattern SPACE = Pattern.compile(" ");

   /**
    * For each fake instance and each <tt>@Mock</tt> method containing the <tt>Invocation</tt> parameter, a runtime
    * state will be kept here.
    */
   @Nonnull private final Map<Object, List<FakeState>> fakesToFakeStates;
   @Nonnull private final Map<Object, List<FakeState>> startupFakesToFakeStates;

   public FakeStates()
   {
      startupFakesToFakeStates = new IdentityHashMap<Object, List<FakeState>>(2);
      fakesToFakeStates = new IdentityHashMap<Object, List<FakeState>>(8);
   }

   void addStartupFakeAndItsFakeStates(@Nonnull Object fake, @Nonnull List<FakeState> fakeStates)
   {
      startupFakesToFakeStates.put(fake, fakeStates);
   }

   void addFakeAndItsFakeStates(@Nonnull Object fake, @Nonnull List<FakeState> fakeStates)
   {
      fakesToFakeStates.put(fake, fakeStates);
   }

   public void removeClassState(@Nonnull Class<?> redefinedClass, @Nullable String internalNameForOneOrMoreFakeClasses)
   {
      removeFakeStates(redefinedClass);

      if (internalNameForOneOrMoreFakeClasses != null) {
         if (internalNameForOneOrMoreFakeClasses.indexOf(' ') < 0) {
            removeFakeStates(internalNameForOneOrMoreFakeClasses);
         }
         else {
            String[] fakeClassesInternalNames = SPACE.split(internalNameForOneOrMoreFakeClasses);

            for (String fakeClassInternalName : fakeClassesInternalNames) {
               removeFakeStates(fakeClassInternalName);
            }
         }
      }
   }

   private void removeFakeStates(@Nonnull Class<?> redefinedClass)
   {
      Iterator<List<FakeState>> itr = fakesToFakeStates.values().iterator();

      while (itr.hasNext()) {
         List<FakeState> fakeStates = itr.next();
         FakeState fakeState = fakeStates.get(0);

         if (fakeState.getRealClass() == redefinedClass) {
            fakeStates.clear();
            itr.remove();
         }
      }
   }

   private void removeFakeStates(@Nonnull String fakeClassInternalName)
   {
      Class<?> fakeClass = ClassLoad.loadClass(fakeClassInternalName.replace('/', '.'));
      Iterator<Entry<Object, List<FakeState>>> itr = fakesToFakeStates.entrySet().iterator();

      while (itr.hasNext()) {
         Entry<Object, List<FakeState>> fakeAndFakeStates = itr.next();
         Object fake = fakeAndFakeStates.getKey();

         if (fake.getClass() == fakeClass) {
            itr.remove();
         }
      }
   }

   public boolean updateFakeState(@Nonnull Object fake, int fakeStateIndex)
   {
      FakeState fakeState = getFakeState(fake, fakeStateIndex);
      return fakeState.update();
   }

   @Nonnull
   FakeState getFakeState(@Nonnull Object fake, int fakeStateIndex)
   {
      List<FakeState> fakeStates = startupFakesToFakeStates.get(fake);

      if (fakeStates == null) {
         fakeStates = fakesToFakeStates.get(fake);
      }

      FakeState fakeState = fakeStates.get(fakeStateIndex);
      assert fakeState != null;
      return fakeState;
   }
}
