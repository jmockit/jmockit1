/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.modification;

import java.lang.instrument.*;
import java.security.*;
import java.util.*;
import javax.annotation.*;

import mockit.external.asm.*;
import mockit.internal.startup.*;

public final class ClassModification
{
   private static final Class<?>[] NO_CLASSES = {};

   @Nonnull private final Set<String> modifiedClasses;
   @Nonnull final List<ProtectionDomain> protectionDomainsWithUniqueLocations;
   @Nonnull private final ClassSelection classSelection;

   public ClassModification()
   {
      modifiedClasses = new HashSet<String>();
      protectionDomainsWithUniqueLocations = new ArrayList<ProtectionDomain>();
      classSelection = new ClassSelection();
      redefineClassesAlreadyLoadedForCoverage();
   }

   private void redefineClassesAlreadyLoadedForCoverage()
   {
      Instrumentation inst = Startup.instrumentation();
      Class<?>[] previousLoadedClasses = NO_CLASSES;

      while (true) {
         Class<?>[] loadedClasses = inst.getAllLoadedClasses();
         if (loadedClasses.length <= previousLoadedClasses.length) break;
         redefineClassesForCoverage(previousLoadedClasses, loadedClasses);
         previousLoadedClasses = loadedClasses;
      }
   }

   private void redefineClassesForCoverage(@Nonnull Class<?>[] previousClasses, @Nonnull Class<?>[] newClasses)
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

   private void redefineClassForCoverage(@Nonnull Class<?> loadedClass)
   {
      byte[] modifiedClassfile = readAndModifyClassForCoverage(loadedClass);

      if (modifiedClassfile != null) {
         redefineClassForCoverage(loadedClass, modifiedClassfile);
         registerModifiedClass(loadedClass.getName(), loadedClass.getProtectionDomain());
      }
   }

   private void registerModifiedClass(@Nonnull String className, @Nonnull ProtectionDomain pd)
   {
      modifiedClasses.add(className);

      if (pd.getClassLoader() != null && pd.getCodeSource() != null && pd.getCodeSource().getLocation() != null) {
         addProtectionDomainIfHasUniqueNewPath(pd);
      }
   }

   private void addProtectionDomainIfHasUniqueNewPath(@Nonnull ProtectionDomain newPD)
   {
      String newPath = newPD.getCodeSource().getLocation().getPath();

      for (int i = protectionDomainsWithUniqueLocations.size() - 1; i >= 0; i--) {
         ProtectionDomain previousPD = protectionDomainsWithUniqueLocations.get(i);
         String previousPath = previousPD.getCodeSource().getLocation().getPath();

         if (previousPath.startsWith(newPath)) {
            return;
         }
         else if (newPath.startsWith(previousPath)) {
            protectionDomainsWithUniqueLocations.set(i, newPD);
            return;
         }
      }

      protectionDomainsWithUniqueLocations.add(newPD);
   }

   @Nullable
   private static byte[] readAndModifyClassForCoverage(@Nonnull Class<?> aClass)
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

   @Nullable
   private static byte[] modifyClassForCoverage(@Nonnull Class<?> aClass)
   {
      String className = aClass.getName();
      byte[] modifiedBytecode = CoverageModifier.recoverModifiedByteCodeIfAvailable(className);

      if (modifiedBytecode != null) {
         return modifiedBytecode;
      }

      ClassReader cr = CoverageModifier.createClassReader(aClass);

      return cr == null ? null : modifyClassForCoverage(cr);
   }

   @Nonnull
   private static byte[] modifyClassForCoverage(@Nonnull ClassReader cr)
   {
      CoverageModifier modifier = new CoverageModifier(cr);
      cr.accept(modifier, 0);
      return modifier.toByteArray();
   }

   private static void redefineClassForCoverage(@Nonnull Class<?> loadedClass, @Nonnull byte[] modifiedClassfile)
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

   boolean isToBeConsideredForCoverage(@Nonnull String className, @Nonnull ProtectionDomain protectionDomain)
   {
      return !modifiedClasses.contains(className) && classSelection.isSelected(className, protectionDomain);
   }

   @Nullable
   public byte[] modifyClass(
      @Nonnull String className, @Nonnull ProtectionDomain protectionDomain, @Nonnull byte[] originalClassfile)
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
         catch (RuntimeException e) { e.printStackTrace(); }
         catch (AssertionError e) { e.printStackTrace(); }
         catch (ClassCircularityError e) { e.printStackTrace(); }
      }

      return null;
   }

   @Nonnull
   private static byte[] modifyClassForCoverage(@Nonnull String className, @Nonnull byte[] classBytecode)
   {
      byte[] modifiedBytecode = CoverageModifier.recoverModifiedByteCodeIfAvailable(className);

      if (modifiedBytecode != null) {
         return modifiedBytecode;
      }

      ClassReader cr = new ClassReader(classBytecode);
      return modifyClassForCoverage(cr);
   }
}
