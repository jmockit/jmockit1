/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.mockups;

import java.lang.reflect.*;
import java.lang.reflect.Type;
import java.util.*;
import javax.annotation.*;

import mockit.*;
import mockit.external.asm.*;
import mockit.internal.*;
import mockit.internal.startup.*;
import mockit.internal.state.*;
import static mockit.external.asm.ClassReader.*;

public final class MockClassSetup
{
   @Nonnull final Class<?> realClass;
   @Nullable private ClassReader rcReader;
   @Nonnull private final FakeMethods mockMethods;
   @Nonnull final MockUp<?> mockUp;
   private final boolean forStartupMock;

   public MockClassSetup(
      @Nonnull Class<?> realClass, @Nonnull Class<?> classToMock, @Nullable Type mockedType, @Nonnull MockUp<?> mockUp)
   {
      this(realClass, classToMock, mockedType, mockUp, null);
   }

   MockClassSetup(
      @Nonnull Class<?> realClass, @Nullable Type mockedType, @Nonnull MockUp<?> mockUp, @Nullable byte[] realClassCode)
   {
      this(realClass, realClass, mockedType, mockUp, realClassCode);
   }

   private MockClassSetup(
      @Nonnull Class<?> realClass, @Nonnull Class<?> classToMock, @Nullable Type mockedType, @Nonnull MockUp<?> mockUp,
      @Nullable byte[] realClassCode)
   {
      this.realClass = classToMock;
      mockMethods = new FakeMethods(realClass, mockedType);
      this.mockUp = mockUp;
      forStartupMock = Startup.initializing;
      rcReader = realClassCode == null ? null : new ClassReader(realClassCode);

      Class<?> mockUpClass = mockUp.getClass();
      new MockMethodCollector(mockMethods).collectMockMethods(mockUpClass);

      mockMethods.registerMockStates(mockUp, forStartupMock);

      FakeClasses fakeClasses = TestRun.getFakeClasses();

      if (forStartupMock) {
         fakeClasses.addFake(mockMethods.getFakeClassInternalName(), mockUp);
      }
      else {
         fakeClasses.addFake(mockUp);
      }
   }

   void redefineMethodsInGeneratedClass()
   {
      byte[] modifiedClassFile = modifyRealClass(realClass);

      if (modifiedClassFile != null) {
         applyClassModifications(realClass, modifiedClassFile);
      }
   }

   @Nonnull
   public Set<Class<?>> redefineMethods()
   {
      Set<Class<?>> redefinedClasses = new HashSet<Class<?>>();
      @Nullable Class<?> classToModify = realClass;

      while (classToModify != null && mockMethods.hasUnusedFakes()) {
         byte[] modifiedClassFile = modifyRealClass(classToModify);

         if (modifiedClassFile != null) {
            applyClassModifications(classToModify, modifiedClassFile);
            redefinedClasses.add(classToModify);
         }

         Class<?> superClass = classToModify.getSuperclass();
         classToModify = superClass == Object.class || superClass == Proxy.class ? null : superClass;
         rcReader = null;
      }

      return redefinedClasses;
   }

   @Nullable
   private byte[] modifyRealClass(@Nonnull Class<?> classToModify)
   {
      if (rcReader == null) {
         rcReader = ClassFile.createReaderFromLastRedefinitionIfAny(classToModify);
      }

      MockupsModifier modifier = new MockupsModifier(rcReader, classToModify, mockUp, mockMethods);
      rcReader.accept(modifier, SKIP_FRAMES);

      return modifier.wasModified() ? modifier.toByteArray() : null;
   }

   @Nonnull
   BaseClassModifier createClassModifier(@Nonnull ClassReader cr)
   {
      return new MockupsModifier(cr, realClass, mockUp, mockMethods);
   }

   void applyClassModifications(@Nonnull Class<?> classToModify, @Nonnull byte[] modifiedClassFile)
   {
      Startup.redefineMethods(classToModify, modifiedClassFile);

      if (forStartupMock) {
         CachedClassfiles.addClassfile(classToModify, modifiedClassFile);
      }
      else {
         String mockClassDesc = mockMethods.getFakeClassInternalName();
         TestRun.mockFixture().addRedefinedClass(mockClassDesc, classToModify, modifiedClassFile);
      }
   }
}
