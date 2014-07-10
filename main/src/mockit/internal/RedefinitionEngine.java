/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal;

import java.lang.instrument.*;
import java.util.*;
import java.util.Map.*;

import org.jetbrains.annotations.*;

import mockit.internal.startup.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

public final class RedefinitionEngine
{
   @NotNull private Class<?> realClass;

   public RedefinitionEngine() {}
   public RedefinitionEngine(@NotNull Class<?> realClass) { this.realClass = realClass; }

   public static void redefineClasses(@NotNull ClassDefinition... definitions)
   {
      Startup.redefineMethods(definitions);

      MockFixture mockFixture = TestRun.mockFixture();
      
      for (ClassDefinition def : definitions) {
         mockFixture.addRedefinedClass(def.getDefinitionClass(), def.getDefinitionClassFile());
      }
   }

   public void redefineMethodsWhileRegisteringTheClass(@NotNull byte[] modifiedClassfile)
   {
      redefineMethods(modifiedClassfile);
      addToMapOfRedefinedClasses(null, modifiedClassfile);
      TestRun.mockFixture().registerMockedClass(realClass);
   }

   private void addToMapOfRedefinedClasses(@Nullable String mockClassInternalName, @NotNull byte[] modifiedClassfile)
   {
      TestRun.mockFixture().addRedefinedClass(mockClassInternalName, realClass, modifiedClassfile);
   }

   private void redefineMethods(@NotNull byte[] modifiedClassfile)
   {
      Startup.redefineMethods(realClass, modifiedClassfile);
   }

   public void redefineMethods(@NotNull Map<Class<?>, byte[]> modifiedClassfiles)
   {
      ClassDefinition[] classDefs = new ClassDefinition[modifiedClassfiles.size()];
      int i = 0;

      for (Entry<Class<?>, byte[]> classAndBytecode : modifiedClassfiles.entrySet()) {
         realClass = classAndBytecode.getKey();
         byte[] modifiedClassfile = classAndBytecode.getValue();

         classDefs[i++] = new ClassDefinition(realClass, modifiedClassfile);
         addToMapOfRedefinedClasses(null, modifiedClassfile);
      }

      Startup.redefineMethods(classDefs);
   }

   public void restoreDefinition(@NotNull Class<?> aClass, @Nullable byte[] previousDefinition)
   {
      if (previousDefinition == null) {
         restoreOriginalDefinition(aClass);
      }
      else {
         restoreToDefinition(aClass, previousDefinition);
      }
   }

   public void restoreOriginalDefinition(@NotNull Class<?> aClass)
   {
      if (!GeneratedClasses.isGeneratedImplementationClass(aClass)) {
         realClass = aClass;
         byte[] realClassFile = ClassFile.createReaderOrGetFromCache(aClass).b;
         redefineMethods(realClassFile);
      }
   }

   public void restoreToDefinition(@NotNull Class<?> aClass, @NotNull byte[] definitionToRestore)
   {
      realClass = aClass;
      redefineMethods(definitionToRestore);
   }
}
