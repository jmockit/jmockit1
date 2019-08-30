/*
 * Copyright (c) 2006 JMockit developers
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

   CapturedType(@Nonnull Class<?> baseType) { this.baseType = baseType; }

   boolean isToBeCaptured(@Nonnull Class<?> aClass) {
      if (aClass == baseType || aClass.isArray() || !baseType.isAssignableFrom(aClass) || extendsJMockitBaseType(aClass)) {
         return false;
      }

      return !aClass.isInterface() && !isNotToBeCaptured(aClass.getProtectionDomain(), aClass.getName());
   }

   @SuppressWarnings("UnnecessaryFullyQualifiedName")
   private static boolean extendsJMockitBaseType(@Nonnull Class<?> aClass) {
      return
         mockit.MockUp.class.isAssignableFrom(aClass) ||
         mockit.Expectations.class.isAssignableFrom(aClass) || mockit.Verifications.class.isAssignableFrom(aClass) ||
         mockit.Delegate.class.isAssignableFrom(aClass);
   }

   static boolean isNotToBeCaptured(@Nullable ProtectionDomain pd, @Nonnull String classNameOrDesc) {
      return
         pd == JMOCKIT_DOMAIN ||
         classNameOrDesc.endsWith("Test") ||
         isNonEligibleInternalJDKClass(classNameOrDesc) ||
         isNonEligibleStandardJavaClass(classNameOrDesc) ||
         isNonEligibleClassFromIDERuntime(classNameOrDesc) ||
         isNonEligibleClassFromThirdPartyLibrary(classNameOrDesc) ||
         isGeneratedClass(classNameOrDesc) || isExternallyGeneratedSubclass(classNameOrDesc);
   }

   private static boolean isNonEligibleInternalJDKClass(@Nonnull String classNameOrDesc) {
      return
         classNameOrDesc.startsWith("jdk/") ||
         classNameOrDesc.startsWith("sun") && !hasSubPackage(classNameOrDesc, 4, "management") ||
         classNameOrDesc.startsWith("com") &&  hasSubPackage(classNameOrDesc, 4, "sun") && !hasSubPackages(classNameOrDesc, 8, "proxy org");
   }

   private static boolean isNonEligibleStandardJavaClass(@Nonnull String classNameOrDesc) {
      return classNameOrDesc.startsWith("java") && !hasSubPackage(classNameOrDesc, 10, "concurrent");
   }

   private static boolean isNonEligibleClassFromIDERuntime(@Nonnull String classNameOrDesc) {
      return classNameOrDesc.startsWith("com") && hasSubPackage(classNameOrDesc, 4, "intellij");
   }

   private static boolean isNonEligibleClassFromThirdPartyLibrary(@Nonnull String classNameOrDesc) {
      return
         classNameOrDesc.startsWith("junit") ||
         classNameOrDesc.startsWith("org") && hasSubPackages(classNameOrDesc, 4, "junit testng hamcrest gradle");
   }

   private static boolean hasSubPackage(@Nonnull String nameOrDesc, @Nonnegative int offset, @Nonnull String subPackage) {
      return nameOrDesc.regionMatches(offset, subPackage, 0, subPackage.length());
   }

   private static boolean hasSubPackages(@Nonnull String nameOrDesc, @Nonnegative int offset, @Nonnull String subPackages) {
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