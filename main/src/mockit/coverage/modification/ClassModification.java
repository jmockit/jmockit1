/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.modification;

import java.security.*;
import java.util.*;
import javax.annotation.*;

import mockit.asm.classes.*;

public final class ClassModification
{
   @Nonnull private final Set<String> modifiedClasses;
   @Nonnull final List<ProtectionDomain> protectionDomainsWithUniqueLocations;
   @Nonnull private final ClassSelection classSelection;

   public ClassModification() {
      modifiedClasses = new HashSet<>();
      protectionDomainsWithUniqueLocations = new ArrayList<>();
      classSelection = new ClassSelection();
   }

   public boolean shouldConsiderClassesNotLoaded() { return !classSelection.loadedOnly; }

   boolean isToBeConsideredForCoverage(@Nonnull String className, @Nonnull ProtectionDomain protectionDomain) {
      return !modifiedClasses.contains(className) && classSelection.isSelected(className, protectionDomain);
   }

   @Nullable
   public byte[] modifyClass(@Nonnull String className, @Nonnull ProtectionDomain protectionDomain, @Nonnull byte[] originalClassfile) {
      if (isToBeConsideredForCoverage(className, protectionDomain)) {
         try {
            byte[] modifiedClassfile = modifyClassForCoverage(className, originalClassfile);
            registerModifiedClass(className, protectionDomain);
            return modifiedClassfile;
         }
         catch (VisitInterruptedException ignore) {
            // Ignore the class if the modification was refused for some reason.
         }
         catch (RuntimeException | AssertionError | ClassCircularityError e) { e.printStackTrace(); }
      }

      return null;
   }

   @Nonnull
   private static byte[] modifyClassForCoverage(@Nonnull String className, @Nonnull byte[] classBytecode) {
      byte[] modifiedBytecode = CoverageModifier.recoverModifiedByteCodeIfAvailable(className);

      if (modifiedBytecode != null) {
         return modifiedBytecode;
      }

      ClassReader cr = new ClassReader(classBytecode);
      CoverageModifier modifier = new CoverageModifier(cr);
      cr.accept(modifier);
      return modifier.toByteArray();
   }

   private void registerModifiedClass(@Nonnull String className, @Nonnull ProtectionDomain pd) {
      modifiedClasses.add(className);

      if (pd.getClassLoader() != null && pd.getCodeSource() != null && pd.getCodeSource().getLocation() != null) {
         addProtectionDomainIfHasUniqueNewPath(pd);
      }
   }

   private void addProtectionDomainIfHasUniqueNewPath(@Nonnull ProtectionDomain newPD) {
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
}