/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import javax.annotation.*;

import mockit.external.asm.*;
import static mockit.external.asm.Opcodes.*;

public final class TypeConversion
{
   private TypeConversion() {}

   public static void generateCastToObject(@Nonnull MethodVisitor mv, @Nonnull JavaType type)
   {
      if (type instanceof PrimitiveType) {
         String wrapperTypeDesc = ((PrimitiveType) type).getWrapperTypeDesc();
         String desc = '(' + type.getDescriptor() + ")L" + wrapperTypeDesc + ';';

         mv.visitMethodInsn(INVOKESTATIC, wrapperTypeDesc, "valueOf", desc, false);
      }
   }

   public static void generateCastFromObject(@Nonnull MethodVisitor mv, @Nonnull JavaType toType)
   {
      if (toType instanceof PrimitiveType) {
         PrimitiveType primitiveType = (PrimitiveType) toType;

         if (primitiveType.getType() == void.class) {
            mv.visitInsn(POP);
         }
         else {
            generateTypeCheck(mv, primitiveType);
            generateUnboxing(mv, primitiveType);
         }
      }
      else {
         generateTypeCheck(mv, toType);
      }
   }

   private static void generateTypeCheck(@Nonnull MethodVisitor mv, @Nonnull JavaType toType)
   {
      String typeDesc;

      if (toType instanceof ReferenceType) {
         typeDesc = ((ReferenceType) toType).getInternalName();
      }
      else {
         typeDesc = ((PrimitiveType) toType).getWrapperTypeDesc();
      }

      mv.visitTypeInsn(CHECKCAST, typeDesc);
   }

   private static void generateUnboxing(@Nonnull MethodVisitor mv, @Nonnull PrimitiveType primitiveType)
   {
      String owner = primitiveType.getWrapperTypeDesc();
      String methodName = primitiveType.getClassName() + "Value";
      String methodDesc = "()" + primitiveType.getTypeCode();

      mv.visitMethodInsn(INVOKEVIRTUAL, owner, methodName, methodDesc, false);
   }

   public static void generateCastOrUnboxing(@Nonnull MethodVisitor mv, @Nonnull JavaType parameterType, int opcode)
   {
      if (opcode == ASTORE) {
         generateTypeCheck(mv, parameterType);
         return;
      }

      String typeDesc = ((ReferenceType) parameterType).getInternalName();
      mv.visitTypeInsn(CHECKCAST, typeDesc);

      PrimitiveType primitiveType = PrimitiveType.getCorrespondingPrimitiveTypeIfWrapperType(typeDesc);
      assert primitiveType != null;

      generateUnboxing(mv, primitiveType);
   }

   public static boolean isPrimitiveWrapper(@Nonnull String typeDesc)
   {
      return PrimitiveType.getCorrespondingPrimitiveTypeIfWrapperType(typeDesc) != null;
   }

   public static boolean isBoxing(@Nonnull String owner, @Nonnull String name, @Nonnull String desc)
   {
      return desc.charAt(2) == ')' && "valueOf".equals(name) && isPrimitiveWrapper(owner);
   }

   public static boolean isUnboxing(int opcode, @Nonnull String owner, @Nonnull String desc)
   {
      return opcode == INVOKEVIRTUAL && desc.charAt(1) == ')' && isPrimitiveWrapper(owner);
   }
}
