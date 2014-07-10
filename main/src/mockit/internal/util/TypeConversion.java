/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import org.jetbrains.annotations.*;

import mockit.external.asm4.*;

import static mockit.external.asm4.Opcodes.*;

public final class TypeConversion
{
   private static final String[] PRIMITIVE_WRAPPER_TYPE = {
      null, "java/lang/Boolean", "java/lang/Character", "java/lang/Byte", "java/lang/Short", "java/lang/Integer",
      "java/lang/Float", "java/lang/Long", "java/lang/Double"
   };
   private static final String PRIMITIVE_WRAPPER_TYPES =
      "java/lang/Boolean   java/lang/Character java/lang/Byte      java/lang/Short     " +
      "java/lang/Integer   java/lang/Float     java/lang/Long      java/lang/Double";
   private static final String[] UNBOXING_NAME = {
      null, "booleanValue", "charValue", "byteValue", "shortValue", "intValue", "floatValue", "longValue", "doubleValue"
   };
   private static final String[] UNBOXING_DESC = {null, "()Z", "()C", "()B", "()S", "()I", "()F", "()J", "()D"};

   public static void generateCastToObject(@NotNull MethodVisitor mv, @NotNull Type type)
   {
      int sort = type.getSort();

      if (sort < Type.ARRAY) {
         String wrapperType = PRIMITIVE_WRAPPER_TYPE[sort];
         String desc = '(' + type.getDescriptor() + ")L" + wrapperType + ';';
         mv.visitMethodInsn(INVOKESTATIC, wrapperType, "valueOf", desc);
      }
   }

   public static void generateCastFromObject(@NotNull MethodVisitor mv, @NotNull Type toType)
   {
      int sort = toType.getSort();

      if (sort == Type.VOID) {
         mv.visitInsn(POP);
      }
      else {
         generateTypeCheck(mv, toType);

         if (sort < Type.ARRAY) {
            mv.visitMethodInsn(INVOKEVIRTUAL, PRIMITIVE_WRAPPER_TYPE[sort], UNBOXING_NAME[sort], UNBOXING_DESC[sort]);
         }
      }
   }

   private static void generateTypeCheck(@NotNull MethodVisitor mv, @NotNull Type toType)
   {
      int sort = toType.getSort();
      String typeDesc;

      switch (sort) {
         case Type.ARRAY: typeDesc = toType.getDescriptor(); break;
         case Type.OBJECT: typeDesc = toType.getInternalName(); break;
         default: typeDesc = PRIMITIVE_WRAPPER_TYPE[sort];
      }

      mv.visitTypeInsn(CHECKCAST, typeDesc);
   }

   public static void generateCastOrUnboxing(@NotNull MethodVisitor mv, @NotNull Type parameterType, int opcode)
   {
      if (opcode == ASTORE) {
         generateTypeCheck(mv, parameterType);
         return;
      }

      int sort = parameterType.getSort();
      String typeDesc;

      if (sort < Type.ARRAY) {
         typeDesc = PRIMITIVE_WRAPPER_TYPE[sort];
      }
      else {
         typeDesc = parameterType.getInternalName();
         int i = PRIMITIVE_WRAPPER_TYPES.indexOf(typeDesc);

         if (i >= 0) {
            sort = i / 20 + 1;
         }
         else if (opcode == ISTORE && "java/lang/Number".equals(typeDesc)) {
            sort = Type.INT;
         }
         else {
            sort = Type.INT;
            switch (opcode) {
               case FSTORE: sort = Type.FLOAT; break;
               case LSTORE: sort = Type.LONG; break;
               case DSTORE: sort = Type.DOUBLE;
            }
            typeDesc = PRIMITIVE_WRAPPER_TYPE[sort];
         }
      }

      mv.visitTypeInsn(CHECKCAST, typeDesc);
      mv.visitMethodInsn(INVOKEVIRTUAL, typeDesc, UNBOXING_NAME[sort], UNBOXING_DESC[sort]);
   }

   public static boolean isPrimitiveWrapper(@NotNull String typeDesc)
   {
      return PRIMITIVE_WRAPPER_TYPES.contains(typeDesc);
   }

   public static boolean isBoxing(@NotNull String owner, @NotNull String name, @NotNull String desc)
   {
      return desc.charAt(2) == ')' && "valueOf".equals(name) && isPrimitiveWrapper(owner);
   }

   public static boolean isUnboxing(int opcode, @NotNull String owner, @NotNull String desc)
   {
      return opcode == INVOKEVIRTUAL && desc.charAt(1) == ')' && isPrimitiveWrapper(owner);
   }
}
