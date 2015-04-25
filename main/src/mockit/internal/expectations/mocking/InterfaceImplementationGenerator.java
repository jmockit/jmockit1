/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.reflect.Type;
import javax.annotation.*;

import mockit.external.asm.*;
import mockit.internal.classGeneration.*;
import static mockit.external.asm.Opcodes.*;
import static mockit.internal.expectations.mocking.MockedTypeModifier.*;

final class InterfaceImplementationGenerator extends BaseImplementationGenerator
{
   @Nonnull private final MockedTypeInfo mockedTypeInfo;
   private String interfaceName;

   InterfaceImplementationGenerator(
      @Nonnull ClassReader classReader, @Nonnull Type mockedType, @Nonnull String implementationClassName)
   {
      super(classReader, implementationClassName);
      mockedTypeInfo = new MockedTypeInfo(mockedType);
   }

   @Override
   public void visit(
      int version, int access, @Nonnull String name, @Nullable String signature, @Nullable String superName,
      @Nullable String[] interfaces)
   {
      interfaceName = name;
      String classSignature = signature == null ? null : signature + mockedTypeInfo.implementationSignature;
      super.visit(version, access, name, classSignature, superName, interfaces);
   }

   @Override
   protected void generateMethodBody(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions)
   {
      String resolvedSignature = signature;

      if (signature != null) {
         resolvedSignature = mockedTypeInfo.genericTypeMap.resolveReturnType(signature);
      }

      mw = cw.visitMethod(ACC_PUBLIC, name, desc, resolvedSignature, exceptions);
      generateDirectCallToHandler(mw, interfaceName, access, name, desc, resolvedSignature);
      generateReturnWithObjectAtTopOfTheStack(desc);
      mw.visitMaxs(1, 0);
   }
}
