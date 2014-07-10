/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.modification;

import java.lang.instrument.*;
import java.security.*;
import java.util.*;

import org.jetbrains.annotations.*;

import mockit.coverage.standalone.*;
import mockit.external.asm4.*;

public final class ClassModification
{
   @NotNull private final Set<String> modifiedClasses;
   @NotNull final List<ProtectionDomain> protectionDomains;
   @NotNull private final ClassSelection classSelection;

   public ClassModification()
   {
      modifiedClasses = new HashSet<String>();
      protectionDomains = new ArrayList<ProtectionDomain>();
      classSelection = new ClassSelection();
      redefineClassesAlreadyLoadedForCoverage();
   }

   private void redefineClassesAlreadyLoadedForCoverage()
   {
      Instrumentation inst = Startup.instrumentation();
      Class<?>[] previousLoadedClasses = {};

      while (true) {
         Class<?>[] loadedClasses = inst.getAllLoadedClasses();
         if (loadedClasses.length <= previousLoadedClasses.length) break;
         redefineClassesForCoverage(previousLoadedClasses, loadedClasses);
         previousLoadedClasses = loadedClasses;
      }
   }

   private void redefineClassesForCoverage(@NotNull Class<?>[] previousClasses, @NotNull Class<?>[] newClasses)
   {
      int m = previousClasses.length;

      for (int i = 0, n = newClasses.length; i < n; i++) {
         Class<?> loadedClass = newClasses[i];

         if (
            (i >= m || loadedClass != previousClasses[i]) &&
            loadedClass.getClassLoader() != null && !loadedClass.isAnnotation() && !loadedClass.isSynthetic() &&
            isToBeConsideredForCoverage(loadedClass.getName(), loadedClass.getProtectionDomain())
         ) {
            redefineClassForCoverage(loadedClass);
         }
      }
   }

   private void redefineClassForCoverage(@NotNull Class<?> loadedClass)
   {
      byte[] modifiedClassfile = readAndModifyClassForCoverage(loadedClass);

      if (modifiedClassfile != null) {
         redefineClassForCoverage(loadedClass, modifiedClassfile);
         registerModifiedClass(loadedClass.getName(), loadedClass.getProtectionDomain());
      }
   }

   private void registerModifiedClass(@NotNull String className, @NotNull ProtectionDomain pd)
   {
      modifiedClasses.add(className);

      if (pd.getClassLoader() != null && pd.getCodeSource() != null && pd.getCodeSource().getLocation() != null) {
         addProtectionDomainIfHasUniqueNewPath(pd);
      }
   }

   private void addProtectionDomainIfHasUniqueNewPath(ProtectionDomain newPD)
   {
      String newPath = newPD.getCodeSource().getLocation().getPath();

      for (int i = protectionDomains.size() - 1; i >= 0; i--) {
         ProtectionDomain previousPD = protectionDomains.get(i);
         String previousPath = previousPD.getCodeSource().getLocation().getPath();

         if (previousPath.startsWith(newPath)) {
            return;
         }
         else if (newPath.startsWith(previousPath)) {
            protectionDomains.set(i, newPD);
            return;
         }
      }

      protectionDomains.add(newPD);
   }

   @Nullable private byte[] readAndModifyClassForCoverage(@NotNull Class<?> aClass)
   {
      try {
         return modifyClassForCoverage(aClass);
      }
      catch (VisitInterruptedException ignore) {
         // Ignore the class if the modification was refused for some reason.
      }
      catch (RuntimeException e) {
         e.printStackTrace();
      }
      catch (AssertionError e) {
         e.printStackTrace();
      }

      return null;
   }

   @Nullable private byte[] modifyClassForCoverage(@NotNull Class<?> aClass)
   {
      String className = aClass.getName();
      byte[] modifiedBytecode = CoverageModifier.recoverModifiedByteCodeIfAvailable(className);

      if (modifiedBytecode != null) {
         return modifiedBytecode;
      }

      ClassReader cr = CoverageModifier.createClassReader(aClass);

      return cr == null ? null : modifyClassForCoverage(cr);
   }

   @NotNull private byte[] modifyClassForCoverage(@NotNull ClassReader cr)
   {
      CoverageModifier modifier = new CoverageModifier(cr);
      cr.accept(modifier, 0);
      return modifier.toByteArray();
   }

   private void redefineClassForCoverage(@NotNull Class<?> loadedClass, @NotNull byte[] modifiedClassfile)
   {
      ClassDefinition[] classDefs = {new ClassDefinition(loadedClass, modifiedClassfile)};

      try {
         Startup.instrumentation().redefineClasses(classDefs);
      }
      catch (ClassNotFoundException e) {
         throw new RuntimeException(e);
      }
      catch (UnmodifiableClassException e) {
         throw new RuntimeException(e);
      }
   }

   public boolean shouldConsiderClassesNotLoaded() { return !classSelection.loadedOnly; }

   boolean isToBeConsideredForCoverage(@NotNull String className, @NotNull ProtectionDomain protectionDomain)
   {
      return !modifiedClasses.contains(className) && classSelection.isSelected(className, protectionDomain);
   }

   @Nullable
   public byte[] modifyClass(
      @NotNull String className, @NotNull ProtectionDomain protectionDomain, @NotNull byte[] originalClassfile)
   {
      boolean modifyClassForCoverage = isToBeConsideredForCoverage(className, protectionDomain);

      if (modifyClassForCoverage) {
         try {
            byte[] modifiedClassfile = modifyClassForCoverage(className, originalClassfile);
            registerModifiedClass(className, protectionDomain);
            return modifiedClassfile;
         }
         catch (VisitInterruptedException ignore) {
            // Ignore the class if the modification was refused for some reason.
         }
         catch (RuntimeException e) {
            e.printStackTrace();
         }
         catch (AssertionError e) {
            e.printStackTrace();
         }
      }

      return null;
   }

   @NotNull private byte[] modifyClassForCoverage(@NotNull String className, @NotNull byte[] classBytecode)
   {
      byte[] modifiedBytecode = CoverageModifier.recoverModifiedByteCodeIfAvailable(className);

      if (modifiedBytecode != null) {
         return modifiedBytecode;
      }

      ClassReader cr = new ClassReader(classBytecode);
      return modifyClassForCoverage(cr);
   }
}
