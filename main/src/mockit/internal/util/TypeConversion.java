/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import javax.annotation.*;

import mockit.external.asm.*;
import mockit.external.asm.PrimitiveType.*;
import static mockit.external.asm.Opcodes.*;

public final class TypeConversion
{
   private static final String PRIMITIVE_WRAPPER_TYPES =
      "java/lang/Boolean   java/lang/Character java/lang/Byte      java/lang/Short     " +
      "java/lang/Integer   java/lang/Float     java/lang/Long      java/lang/Double";
   private static final String[] UNBOXING_NAME = {
      null, "booleanValue", "charValue", "byteValue", "shortValue", "intValue", "floatValue", "longValue", "doubleValue"
   };
   private static final String[] UNBOXING_DESC = {null, "()Z", "()C", "()B", "()S", "()I", "()F", "()J", "()D"};

   private TypeConversion() {}

   public static void generateCastToObject(@Nonnull MethodVisitor mv, @Nonnull JavaType type)
   {
      if (type instanceof PrimitiveType) {
         String wrapperType = ((PrimitiveType) type).getWrapperTypeDesc();

         if (wrapperType != null) {
            String desc = '(' + type.getDescriptor() + ")L" + wrapperType + ';';
            mv.visitMethodInsn(INVOKESTATIC, wrapperType, "valueOf", desc, false);
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
            int sort = primitiveType.getSort();
            mv.visitMethodInsn(INVOKEVIRTUAL, owner, UNBOXING_NAME[sort], UNBOXING_DESC[sort], false);
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

   @SuppressWarnings({"ConstantConditions", "OverlyLongMethod"})
   public static void generateCastOrUnboxing(@Nonnull MethodVisitor mv, @Nonnull JavaType parameterType, int opcode)
   {
      if (opcode == ASTORE) {
         generateTypeCheck(mv, parameterType);
         return;
      }

      int sort;
      String typeDesc;

      if (parameterType instanceof PrimitiveType) {
         PrimitiveType primitiveType = (PrimitiveType) parameterType;
         sort = primitiveType.getSort();
         typeDesc = primitiveType.getWrapperTypeDesc();
      }
      else {
         typeDesc = ((ReferenceType) parameterType).getInternalName();
         int i = PRIMITIVE_WRAPPER_TYPES.indexOf(typeDesc);

         if (i >= 0) {
            sort = i / 20 + 1;
         }
         else if (opcode == ISTORE && "java/lang/Number".equals(typeDesc)) {
            sort = Sort.INT;
         }
         else {
            switch (opcode) {
               case FSTORE: sort = Sort.FLOAT;  typeDesc = "java/lang/Float";  break;
               case LSTORE: sort = Sort.LONG;   typeDesc = "java/lang/Long";   break;
               case DSTORE: sort = Sort.DOUBLE; typeDesc = "java/lang/Double"; break;
               default:     sort = Sort.INT;    typeDesc = "java/lang/Integer";
            }
         }
      }

      mv.visitTypeInsn(CHECKCAST, typeDesc);
      mv.visitMethodInsn(INVOKEVIRTUAL, typeDesc, UNBOXING_NAME[sort], UNBOXING_DESC[sort], false);
   }

   public static boolean isPrimitiveWrapper(@Nonnull String typeDesc)
   {
      return PRIMITIVE_WRAPPER_TYPES.contains(typeDesc);
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
