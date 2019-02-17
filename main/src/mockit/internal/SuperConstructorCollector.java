/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal;

import java.util.*;

import javax.annotation.*;

import static java.lang.reflect.Modifier.*;

import mockit.asm.metadata.*;
import mockit.asm.metadata.ClassMetadataReader.*;

final class SuperConstructorCollector
{
   @Nonnull static final SuperConstructorCollector INSTANCE = new SuperConstructorCollector();
   @Nonnull private final Map<String, String> cache = new HashMap<>();

   private SuperConstructorCollector() {}

   @Nonnull
   synchronized String findConstructor(@Nonnull String classDesc, @Nonnull String superClassDesc) {
      String constructorDesc = cache.get(superClassDesc);

      if (constructorDesc != null) {
         return constructorDesc;
      }

      boolean samePackage = areBothClassesInSamePackage(classDesc, superClassDesc);

      byte[] classfile = ClassFile.getClassFile(superClassDesc);
      ClassMetadataReader cmr = new ClassMetadataReader(classfile);

      for (MethodInfo methodOrConstructor : cmr.getMethods()) {
         int access = methodOrConstructor.accessFlags;

         if (access != PRIVATE && (access != 0 || samePackage) && methodOrConstructor.isConstructor()) {
            if (constructorDesc == null || constructorDesc.length() > methodOrConstructor.desc.length()) {
               constructorDesc = methodOrConstructor.desc;
            }
         }
      }

      assert constructorDesc != null;
      cache.put(superClassDesc, constructorDesc);
      return constructorDesc;
   }

   private static boolean areBothClassesInSamePackage(@Nonnull String classDesc, @Nonnull String superClassDesc) {
      int p1 = classDesc.lastIndexOf('/');
      int p2 = superClassDesc.lastIndexOf('/');
      return p1 == p2 && (p1 < 0 || classDesc.substring(0, p1).equals(superClassDesc.substring(0, p2)));
   }
}
