/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.modification;

import java.io.*;
import java.security.*;
import java.util.*;
import javax.annotation.*;

/**
 * Finds and loads all classes that should also be measured, but were not loaded until now.
 */
public final class ClassesNotLoaded
{
   @Nonnull private final ClassModification classModification;
   private int firstPosAfterParentDir;

   public ClassesNotLoaded(@Nonnull ClassModification classModification) { this.classModification = classModification; }

   public void gatherCoverageData()
   {
      Set<ProtectionDomain> protectionDomainsSoFar =
         new HashSet<ProtectionDomain>(classModification.protectionDomainsWithUniqueLocations);

      for (ProtectionDomain pd : protectionDomainsSoFar) {
         File classPathEntry = new File(pd.getCodeSource().getLocation().getPath());

         if (!classPathEntry.getPath().endsWith(".jar")) {
            firstPosAfterParentDir = classPathEntry.getPath().length() + 1;
            loadAdditionalClasses(classPathEntry, pd);
         }
      }
   }

   private void loadAdditionalClasses(@Nonnull File classPathEntry, @Nonnull ProtectionDomain protectionDomain)
   {
      File[] filesInDir = classPathEntry.listFiles();

      if (filesInDir != null) {
         for (File fileInDir : filesInDir) {
            if (fileInDir.isDirectory()) {
               loadAdditionalClasses(fileInDir, protectionDomain);
            }
            else {
               loadAdditionalClass(fileInDir.getPath(), protectionDomain);
            }
         }
      }
   }

   private void loadAdditionalClass(@Nonnull String filePath, @Nonnull ProtectionDomain protectionDomain)
   {
      int p = filePath.lastIndexOf(".class");

      if (p > 0) {
         String relativePath = filePath.substring(firstPosAfterParentDir, p);
         String className = relativePath.replace(File.separatorChar, '.');

         if (classModification.isToBeConsideredForCoverage(className, protectionDomain)) {
            loadClass(className, protectionDomain);
         }
      }
   }

   private static void loadClass(@Nonnull String className, @Nonnull ProtectionDomain protectionDomain)
   {
      try {
         Class.forName(className, false, protectionDomain.getClassLoader());
      }
      catch (ClassNotFoundException ignore) {}
      catch (NoClassDefFoundError ignored) {}
   }
}
