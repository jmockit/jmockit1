/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.capturing;

import java.security.*;
import javax.annotation.*;

import mockit.internal.util.*;
import static mockit.internal.util.GeneratedClasses.*;

final class CapturedType
{
   private static final ProtectionDomain JMOCKIT_DOMAIN = CapturedType.class.getProtectionDomain();

   @Nonnull final Class<?> baseType;

   CapturedType(@Nonnull Class<?> baseType) { this.baseType = baseType; }

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

      if (aClass.isInterface()) {
         return false;
      }

      return !isNotToBeCaptured(aClass.getClassLoader(), aClass.getProtectionDomain(), aClass.getName());
   }

   static boolean isNotToBeCaptured(
      @Nullable ClassLoader loader, @Nullable ProtectionDomain protectionDomain, @Nonnull String classNameOrDesc)
   {
      if (
         loader == null && classNameOrDesc.startsWith("java") ||
         protectionDomain == JMOCKIT_DOMAIN || isGeneratedClass(classNameOrDesc)
      ) {
         return true;
      }

      return
         classNameOrDesc.endsWith("Test") ||
         classNameOrDesc.startsWith("junit") || classNameOrDesc.startsWith("sun") ||
         classNameOrDesc.startsWith("org") && (
            hasSubPackage(classNameOrDesc, "junit") || hasSubPackage(classNameOrDesc, "testng") ||
            hasSubPackage(classNameOrDesc, "hamcrest")
         ) ||
         classNameOrDesc.startsWith("com") && hasSubPackage(classNameOrDesc, "intellij") ||
         ClassLoad.isGeneratedSubclass(classNameOrDesc);
   }

   private static boolean hasSubPackage(@Nonnull String nameOrDesc, @Nonnull String subPackage)
   {
      return nameOrDesc.regionMatches(4, subPackage, 0, subPackage.length());
   }
}
