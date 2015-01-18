/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.reflect.Type;

import mockit.external.asm.*;
import mockit.internal.classGeneration.*;
import static mockit.external.asm.Opcodes.*;
import static mockit.internal.expectations.mocking.MockedTypeModifier.*;

import org.jetbrains.annotations.*;

final class InterfaceImplementationGenerator extends BaseImplementationGenerator
{
   @NotNull private final MockedTypeInfo mockedTypeInfo;
   private String interfaceName;

   InterfaceImplementationGenerator(
      @NotNull ClassReader classReader, @NotNull Type mockedType, @NotNull String implementationClassName)
   {
      super(classReader, implementationClassName);
      mockedTypeInfo = new MockedTypeInfo(mockedType);
   }

   @Override
   public void visit(
      int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superName,
      @Nullable String[] interfaces)
   {
      interfaceName = name;
      String classSignature = signature == null ? null : signature + mockedTypeInfo.implementationSignature;
      super.visit(version, access, name, classSignature, superName, interfaces);
   }

   @Override
   @SuppressWarnings("AssignmentToMethodParameter")
   protected void generateMethodBody(
      int access, @NotNull String name, @NotNull String desc, @Nullable String signature, @Nullable String[] exceptions)
   {
      if (signature != null) {
         signature = mockedTypeInfo.genericTypeMap.resolveReturnType(signature);
      }

      mw = cw.visitMethod(ACC_PUBLIC, name, desc, signature, exceptions);
      generateDirectCallToHandler(mw, interfaceName, access, name, desc, signature);
      generateReturnWithObjectAtTopOfTheStack(desc);
      mw.visitMaxs(1, 0);
   }
}
