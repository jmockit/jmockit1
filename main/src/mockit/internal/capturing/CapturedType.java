/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.capturing;

import java.net.*;
import java.security.*;
import static java.lang.reflect.Proxy.*;

import org.jetbrains.annotations.*;

import static mockit.internal.util.GeneratedClasses.*;

final class CapturedType
{
   @NotNull final Class<?> baseType;

   CapturedType(@NotNull Class<?> baseType) { this.baseType = baseType; }

   @SuppressWarnings("UnnecessaryFullyQualifiedName")
   boolean isToBeCaptured(@NotNull Class<?> aClass)
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

      if (aClass.isInterface() || isProxyClass(aClass)) {
         return false;
      }

      return !isNotToBeCaptured(aClass.getClassLoader(), aClass.getProtectionDomain(), aClass.getName());
   }

   boolean isNotToBeCaptured(
      @Nullable ClassLoader loader, @Nullable ProtectionDomain protectionDomain, @NotNull String classNameOrDesc)
   {
      if (
         loader == null || protectionDomain == null || protectionDomain.getClassLoader() == null ||
         isGeneratedClass(classNameOrDesc)
      ) {
         return true;
      }

      if (
         classNameOrDesc.endsWith("Test") ||
         classNameOrDesc.startsWith("junit") || classNameOrDesc.startsWith("sun") ||
         classNameOrDesc.startsWith("org") && (
            hasSubPackage(classNameOrDesc, 4, "junit") || hasSubPackage(classNameOrDesc, 4, "testng") ||
            hasSubPackage(classNameOrDesc, 4, "hamcrest")
         ) ||
         classNameOrDesc.startsWith("com") && (
            hasSubPackage(classNameOrDesc, 4, "sun") || hasSubPackage(classNameOrDesc, 4, "intellij")
         )
      ) {
         return true;
      }

      CodeSource codeSource = protectionDomain.getCodeSource();

      if (codeSource == null || !classNameOrDesc.startsWith("mockit")) {
         return false;
      }

      URL location = codeSource.getLocation();
      return location != null && location.getPath().endsWith("/main/classes/");
   }

   private static boolean hasSubPackage(@NotNull String nameOrDesc, int offset, @NotNull String subPackage)
   {
      return nameOrDesc.regionMatches(offset, subPackage, 0, subPackage.length());
   }
}
