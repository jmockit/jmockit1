/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.capturing;

import java.security.*;
import javax.annotation.*;

import static mockit.internal.util.GeneratedClasses.*;

final class CapturedType
{
   private static final ProtectionDomain JMOCKIT_DOMAIN = CapturedType.class.getProtectionDomain();

   @Nonnull final Class<?> baseType;
   @Nullable private final ProtectionDomain baseTypePD;
   private final boolean baseTypePDWithCodeSource;

   CapturedType(@Nonnull Class<?> baseType)
   {
      this.baseType = baseType;
      baseTypePD = baseType.getProtectionDomain();
      baseTypePDWithCodeSource = baseTypePD != null && baseTypePD.getCodeSource() != null;
   }

   @SuppressWarnings("UnnecessaryFullyQualifiedName")
   boolean isToBeCaptured(@Nonnull Class<?> aClass)
   {
      if (baseType == Object.class) {
         if (
            aClass.isArray() ||
            mockit.MockUp.class.isAssignableFrom(aClass) || mockit.Delegate.class.isAssignableFrom(aClass) ||
            mockit.Expectations.class.isAssignableFrom(aClass) || mockit.Verifications.class.isAssignableFrom(aClass)
         ) {
            return false;
         }
      }
      else if (aClass == baseType || !baseType.isAssignableFrom(aClass)) {
         return false;
      }

      return
         !aClass.isInterface() &&
         !isNotToBeCaptured(aClass.getClassLoader(), aClass.getProtectionDomain(), aClass.getName());
   }

   @SuppressWarnings("OverlyComplexMethod")
   boolean isNotToBeCaptured(
      @Nullable ClassLoader loader, @Nullable ProtectionDomain pd, @Nonnull String classNameOrDesc)
   {
      //noinspection SimplifiableIfStatement
      if (
         loader == null && (classNameOrDesc.startsWith("java") || classNameOrDesc.startsWith("jdk/")) ||
         pd == JMOCKIT_DOMAIN || isGeneratedClass(classNameOrDesc) ||
         pd != baseTypePD && pd != null && pd.getCodeSource() != null && pd.getCodeSource().getLocation() != null &&
         baseTypePDWithCodeSource && pd.getCodeSource().getLocation().getPath().endsWith(".jar")
      ) {
         return true;
      }

      return
         classNameOrDesc.endsWith("Test") ||
         classNameOrDesc.startsWith("junit") ||
         classNameOrDesc.startsWith("sun") && !hasSubPackage(classNameOrDesc, 4, "management") ||
         classNameOrDesc.startsWith("org") && hasSubPackage(classNameOrDesc, 4, "junit testng hamcrest gradle") ||
         classNameOrDesc.startsWith("com") && (
            hasSubPackage(classNameOrDesc, 4, "sun") && !hasSubPackage(classNameOrDesc, 8, "proxy org") ||
            hasSubPackage(classNameOrDesc, 4, "intellij")
         ) ||
         isExternallyGeneratedSubclass(classNameOrDesc);
   }

   private static boolean hasSubPackage(@Nonnull String nameOrDesc, int offset, @Nonnull String subPackages)
   {
      int subPackageStart = 0;
      int subPackageEnd;

      do {
         subPackageEnd = subPackages.indexOf(' ', subPackageStart);
         int subPackageLength = (subPackageEnd > 0 ? subPackageEnd : subPackages.length()) - subPackageStart;

         if (nameOrDesc.regionMatches(offset, subPackages, subPackageStart, subPackageLength)) {
            return true;
         }

         subPackageStart = subPackageEnd + 1;
      } while (subPackageEnd > 0);

      return false;
   }
}
