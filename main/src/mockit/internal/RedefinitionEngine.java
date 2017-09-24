/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal;

import java.lang.instrument.*;
import java.util.*;
import java.util.Map.*;
import javax.annotation.*;

import mockit.internal.startup.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

public final class RedefinitionEngine
{
   @Nonnull private Class<?> realClass;

   public RedefinitionEngine() {}
   public RedefinitionEngine(@Nonnull Class<?> realClass) { this.realClass = realClass; }

   public static void redefineClasses(@Nonnull ClassDefinition... definitions)
   {
      Startup.redefineMethods(definitions);

      MockFixture mockFixture = TestRun.mockFixture();
      
      for (ClassDefinition def : definitions) {
         mockFixture.addRedefinedClass(def.getDefinitionClass(), def.getDefinitionClassFile());
      }
   }

   public void redefineMethodsWhileRegisteringTheClass(@Nonnull byte[] modifiedClassfile)
   {
      Startup.redefineMethods(realClass, modifiedClassfile);

      MockFixture mockFixture = TestRun.mockFixture();
      mockFixture.addRedefinedClass(null, realClass, modifiedClassfile);
      mockFixture.registerMockedClass(realClass);
   }

   public void redefineMethods(@Nonnull Map<Class<?>, byte[]> modifiedClassfiles)
   {
      ClassDefinition[] classDefs = new ClassDefinition[modifiedClassfiles.size()];
      MockFixture mockFixture = TestRun.mockFixture();
      int i = 0;

      for (Entry<Class<?>, byte[]> classAndBytecode : modifiedClassfiles.entrySet()) {
         realClass = classAndBytecode.getKey();
         byte[] modifiedClassfile = classAndBytecode.getValue();

         classDefs[i++] = new ClassDefinition(realClass, modifiedClassfile);
         mockFixture.addRedefinedClass(null, realClass, modifiedClassfile);
      }

      Startup.redefineMethods(classDefs);
   }

   public void restoreDefinition(@Nonnull Class<?> aClass, @Nullable byte[] previousDefinition)
   {
      if (previousDefinition == null) {
         restoreOriginalDefinition(aClass);
      }
      else {
         restoreToDefinition(aClass, previousDefinition);
      }
   }

   public void restoreOriginalDefinition(@Nonnull Class<?> aClass)
   {
      if (!GeneratedClasses.isGeneratedImplementationClass(aClass)) {
         realClass = aClass;
         byte[] realClassFile = ClassFile.createReaderOrGetFromCache(aClass).b;
         Startup.redefineMethods(aClass, realClassFile);
      }
   }

   public void restoreToDefinition(@Nonnull Class<?> aClass, @Nonnull byte[] definitionToRestore)
   {
      realClass = aClass;
      Startup.redefineMethods(aClass, definitionToRestore);
   }
}
