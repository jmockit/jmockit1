/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.faking;

import java.util.*;
import javax.annotation.*;

import mockit.*;
import mockit.asm.*;
import mockit.asm.ClassMetadataReader.*;
import mockit.asm.types.*;
import mockit.internal.*;
import mockit.internal.faking.FakeMethods.FakeMethod;
import mockit.internal.util.*;
import static mockit.asm.jvmConstants.Access.*;

/**
 * Responsible for collecting the signatures of all methods defined in a given fake class which are explicitly annotated
 * as {@link Mock fakes}.
 */
final class FakeMethodCollector
{
   private static final int INVALID_METHOD_ACCESSES = BRIDGE + SYNTHETIC + ABSTRACT + NATIVE;
   private static final EnumSet<Attribute> ANNOTATIONS = EnumSet.of(Attribute.Annotations);

   @Nonnull private final FakeMethods fakeMethods;
   private boolean collectingFromSuperClass;

   FakeMethodCollector(@Nonnull FakeMethods fakeMethods) { this.fakeMethods = fakeMethods; }

   void collectFakeMethods(@Nonnull Class<?> fakeClass) {
      ClassLoad.registerLoadedClass(fakeClass);
      fakeMethods.setFakeClassInternalName(JavaType.getInternalName(fakeClass));

      Class<?> classToCollectFakesFrom = fakeClass;

      do {
         byte[] classfileBytes = ClassFile.readBytesFromClassFile(classToCollectFakesFrom);
         ClassMetadataReader cmr = new ClassMetadataReader(classfileBytes, ANNOTATIONS);
         List<MethodInfo> methods = cmr.getMethods();
         addFakeMethods(methods);

         classToCollectFakesFrom = classToCollectFakesFrom.getSuperclass();
         collectingFromSuperClass = true;
      }
      while (classToCollectFakesFrom != MockUp.class);
   }

   private void addFakeMethods(@Nonnull List<MethodInfo> methods) {
      for (MethodInfo method : methods) {
         int access = method.accessFlags;
         String methodName = method.name;
         String methodDesc = method.desc;

         if ((access & INVALID_METHOD_ACCESSES) == 0 && !"<init>".equals(methodName) && method.hasAnnotation("Lmockit/Mock;")) {
            FakeMethod fakeMethod = fakeMethods.addMethod(collectingFromSuperClass, access, methodName, methodDesc);

            if (fakeMethod != null && fakeMethod.requiresFakeState()) {
               FakeState fakeState = new FakeState(fakeMethod);
               fakeMethods.addFakeState(fakeState);
            }
         }
      }
   }
}
