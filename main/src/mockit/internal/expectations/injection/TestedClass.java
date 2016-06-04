/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import java.io.*;
import java.lang.reflect.*;
import java.security.*;
import javax.annotation.*;

import mockit.internal.util.*;

final class TestedClass
{
   @Nonnull final Type declaredType;
   @Nonnull final Class<?> targetClass;
   @Nullable final GenericTypeReflection genericType;
   @Nonnull final ProtectionDomain protectionDomainOfTestedClass;
   @Nullable final String codeLocationParentPath;
   @Nonnull final String nameOfTestedClass;

   TestedClass(@Nonnull Type declaredType, @Nonnull Class<?> targetClass)
   {
      this.declaredType = declaredType;
      this.targetClass = targetClass;
      genericType = declaredType == targetClass ? null : new GenericTypeReflection(targetClass, declaredType);
      protectionDomainOfTestedClass = targetClass.getProtectionDomain();
      CodeSource codeSource = protectionDomainOfTestedClass.getCodeSource();
      codeLocationParentPath = codeSource == null || codeSource.getLocation() == null ?
         null : new File(codeSource.getLocation().getPath()).getParent();
      nameOfTestedClass = targetClass.getName();
   }

   boolean isClassFromSameModuleOrSystemAsTestedClass(@Nonnull Class<?> anotherClass)
   {
      if (anotherClass.getClassLoader() == null) {
         return false;
      }

      ProtectionDomain anotherProtectionDomain = anotherClass.getProtectionDomain();

      if (anotherProtectionDomain == null) {
         return false;
      }

      if (anotherProtectionDomain == protectionDomainOfTestedClass) {
         return true;
      }

      CodeSource anotherCodeSource = anotherProtectionDomain.getCodeSource();

      if (anotherCodeSource == null || anotherCodeSource.getLocation() == null) {
         return false;
      }

      if (codeLocationParentPath != null) {
         String anotherClassPath = anotherCodeSource.getLocation().getPath();
         String anotherClassParentPath = new File(anotherClassPath).getParent();

         if (anotherClassParentPath.equals(codeLocationParentPath)) {
            return true;
         }
      }

      return isInSameSubpackageAsTestedClass(anotherClass);
   }

   boolean isInSameSubpackageAsTestedClass(@Nonnull Class<?> anotherClass)
   {
      String nameOfAnotherClass = anotherClass.getName();
      int p1 = nameOfAnotherClass.indexOf('.');
      int p2 = nameOfTestedClass.indexOf('.');
      boolean differentPackages = p1 != p2 || p1 == -1;

      if (differentPackages) {
         return false;
      }

      p1 = nameOfAnotherClass.indexOf('.', p1 + 1);
      p2 = nameOfTestedClass.indexOf('.', p2 + 1);
      boolean eitherClassDirectlyInFirstPackageLevel = p1 == -1 || p2 == -1;

      if (eitherClassDirectlyInFirstPackageLevel) {
         return true;
      }

      boolean differentSubpackages = p1 != p2;

      return !differentSubpackages && nameOfAnotherClass.substring(0, p1).equals(nameOfTestedClass.substring(0, p2));
   }
}
