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

         if (wrapperTypeDesc != null) {
            String desc = '(' + type.getDescriptor() + ")L" + wrapperTypeDesc + ';';
            mv.visitMethodInsn(INVOKESTATIC, wrapperTypeDesc, "valueOf", desc, false);
         }
      }
   }

   public static void generateCastFromObject(@Nonnull MethodVisitor mv, @Nonnull JavaType toType)
   {
      PrimitiveType primitiveType = toType instanceof PrimitiveType ? (PrimitiveType) toType : null;
      String owner = primitiveType == null ? null : primitiveType.getWrapperTypeDesc();

      if (primitiveType != null && owner == null) {
         mv.visitInsn(POP);
      }
      else {
         generateTypeCheck(mv, toType);

         if (primitiveType != null) {
            String methodName = primitiveType.getClassName() + "Value";
            String methodDesc = "()" + primitiveType.getTypeCode();
            mv.visitMethodInsn(INVOKEVIRTUAL, owner, methodName, methodDesc, false);
         }
      }
   }

   private static void generateTypeCheck(@Nonnull MethodVisitor mv, @Nonnull JavaType toType)
   {
      String typeDesc;

      if (toType instanceof ArrayType) {
         typeDesc = toType.getDescriptor();
      }
      else if (toType instanceof ObjectType) {
         typeDesc = ((ObjectType) toType).getInternalName();
      }
      else {
         typeDesc = ((PrimitiveType) toType).getWrapperTypeDesc();
      }

      if (typeDesc != null) {
         mv.visitTypeInsn(CHECKCAST, typeDesc);
      }
   }

   @SuppressWarnings("OverlyLongMethod")
   public static void generateCastOrUnboxing(@Nonnull MethodVisitor mv, @Nonnull JavaType parameterType, int opcode)
   {
      if (opcode == ASTORE) {
         generateTypeCheck(mv, parameterType);
         return;
      }

      String typeDesc;
      String methodName;
      String methodDesc;

      if (parameterType instanceof PrimitiveType) {
         PrimitiveType primitiveType = (PrimitiveType) parameterType;
         typeDesc = primitiveType.getWrapperTypeDesc();
         methodName = primitiveType.getClassName() + "Value";
         methodDesc = "()" + primitiveType.getTypeCode();
      }
      else {
         typeDesc = ((ReferenceType) parameterType).getInternalName();
         PrimitiveType primitiveType = PrimitiveType.getPrimitiveType(typeDesc);

         if (primitiveType.getType() != void.class) {
            methodName = primitiveType.getClassName() + "Value";
            methodDesc = "()" + primitiveType.getTypeCode();
         }
         else if (opcode == ISTORE && "java/lang/Number".equals(typeDesc)) { // TODO: no test getting here
            methodName = "intValue";
            methodDesc = "()I";
         }
         else if (opcode == FSTORE) {
            typeDesc = "java/lang/Float";
            methodName = "floatValue";
            methodDesc = "()F";
         }
         else if (opcode == LSTORE) {
            typeDesc = "java/lang/Long";
            methodName = "longValue";
            methodDesc = "()J";
         }
         else if (opcode == DSTORE) {
            typeDesc = "java/lang/Double";
            methodName = "doubleValue";
            methodDesc = "()D";
         }
         else {
            typeDesc = "java/lang/Integer";
            methodName = "intValue";
            methodDesc = "()I";
         }
      }

      //noinspection ConstantConditions
      mv.visitTypeInsn(CHECKCAST, typeDesc);
      mv.visitMethodInsn(INVOKEVIRTUAL, typeDesc, methodName, methodDesc, false);
   }

   public static boolean isPrimitiveWrapper(@Nonnull String typeDesc)
   {
      return PrimitiveType.getPrimitiveType(typeDesc).getType() != void.class;
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
