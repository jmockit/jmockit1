/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import java.util.*;
import javax.annotation.*;

import mockit.internal.state.*;

public final class MethodFormatter
{
   @Nonnull private final StringBuilder out;
   @Nonnull private final List<String> parameterTypes;
   @Nonnull private final String classDesc;
   @Nonnull private String methodDesc;

   // Auxiliary fields for handling method parameters:
   private int parameterIndex;
   private int typeDescPos;
   private char typeCode;
   private int arrayDimensions;

   public MethodFormatter(@Nonnull String classDesc, @Nonnull String methodNameAndDesc)
   {
      this(classDesc, methodNameAndDesc, true);
   }

   public MethodFormatter(@Nonnull String classDesc, @Nonnull String methodNameAndDesc, boolean withParametersAppended)
   {
      out = new StringBuilder();
      parameterTypes = new ArrayList<String>(5);
      this.classDesc = classDesc;
      methodDesc = "";
      methodDesc = methodNameAndDesc;
      appendFriendlyMethodSignature(withParametersAppended);
   }

   @Override
   public String toString() { return out.toString(); }

   @Nonnull public List<String> getParameterTypes() { return parameterTypes; }

   private void appendFriendlyMethodSignature(boolean withParametersAppended)
   {
      String className = classDesc.replace('/', '.');
      out.append(className).append('#');

      String constructorName = getConstructorName(className);
      String friendlyDesc = methodDesc.replace("<init>", constructorName);

      int leftParenNextPos = friendlyDesc.indexOf('(') + 1;
      int rightParenPos = friendlyDesc.indexOf(')');

      if (leftParenNextPos < rightParenPos) {
         out.append(friendlyDesc.substring(0, leftParenNextPos));

         String concatenatedParameterTypes = friendlyDesc.substring(leftParenNextPos, rightParenPos);

         if (withParametersAppended) {
            parameterIndex = 0;
            appendParameterTypesAndNames(concatenatedParameterTypes);
            out.append(')');
         }
         else {
            addParameterTypes(concatenatedParameterTypes);
         }
      }
      else {
         out.append(friendlyDesc.substring(0, rightParenPos + 1));
      }
   }

   @Nonnull
   private static String getConstructorName(@Nonnull String className)
   {
      int p = className.lastIndexOf('.');
      String constructorName = p < 0 ? className : className.substring(p + 1);

      //noinspection ReuseOfLocalVariable
      p = constructorName.lastIndexOf('$');

      if (p > 0) {
         constructorName = constructorName.substring(p + 1);
      }

      return constructorName;
   }

   private void appendParameterTypesAndNames(@Nonnull String typeDescs)
   {
      String sep = "";

      for (String typeDesc : typeDescs.split(";")) {
         out.append(sep);

         if (typeDesc.charAt(0) == 'L') {
            appendParameterType(friendlyReferenceType(typeDesc));
            appendParameterName();
         }
         else {
            appendPrimitiveParameterTypesAndNames(typeDesc);
         }

         sep = ", ";
      }
   }

   @Nonnull
   private static String friendlyReferenceType(@Nonnull String typeDesc)
   {
      return typeDesc.substring(1).replace("java/lang/", "").replace('/', '.');
   }

   private void appendParameterType(@Nonnull String friendlyTypeDesc)
   {
      out.append(friendlyTypeDesc);
      parameterTypes.add(friendlyTypeDesc);
   }

   private void appendParameterName()
   {
      String name = ParameterNames.getName(classDesc, methodDesc, parameterIndex);

      if (name != null) {
         out.append(' ').append(name);
      }

      parameterIndex++;
   }

   private void appendPrimitiveParameterTypesAndNames(@Nonnull String typeDesc)
   {
      String sep = "";

      for (typeDescPos = 0; typeDescPos < typeDesc.length(); typeDescPos++) {
         typeCode = typeDesc.charAt(typeDescPos);
         advancePastArrayDimensionsIfAny(typeDesc);

         out.append(sep);

         String paramType = getTypeNameForTypeDesc(typeDesc) + getArrayBrackets();
         appendParameterType(paramType);
         appendParameterName();

         sep = ", ";
      }
   }

   private void addParameterTypes(@Nonnull String typeDescs)
   {
      for (String typeDesc : typeDescs.split(";")) {
         if (typeDesc.charAt(0) == 'L') {
            parameterTypes.add(friendlyReferenceType(typeDesc));
         }
         else {
            addPrimitiveParameterTypes(typeDesc);
         }
      }
   }

   private void addPrimitiveParameterTypes(@Nonnull String typeDesc)
   {
      for (typeDescPos = 0; typeDescPos < typeDesc.length(); typeDescPos++) {
         typeCode = typeDesc.charAt(typeDescPos);
         advancePastArrayDimensionsIfAny(typeDesc);

         String paramType = getTypeNameForTypeDesc(typeDesc) + getArrayBrackets();
         parameterTypes.add(paramType);
      }
   }

   @Nonnull @SuppressWarnings("OverlyComplexMethod")
   private String getTypeNameForTypeDesc(@Nonnull String typeDesc)
   {
      String paramType;

      switch (typeCode) {
         case 'B': return "byte";
         case 'C': return "char";
         case 'D': return "double";
         case 'F': return "float";
         case 'I': return "int";
         case 'J': return "long";
         case 'S': return "short";
         case 'V': return "void";
         case 'Z': return "boolean";
         case 'L':
            paramType = friendlyReferenceType(typeDesc.substring(typeDescPos));
            typeDescPos = typeDesc.length();
            break;
         default:
            paramType = typeDesc.substring(typeDescPos);
            typeDescPos = typeDesc.length();
      }

      return paramType;
   }

   private void advancePastArrayDimensionsIfAny(@Nonnull String param)
   {
      arrayDimensions = 0;

      while (typeCode == '[') {
         typeDescPos++;
         typeCode = param.charAt(typeDescPos);
         arrayDimensions++;
      }
   }

   @Nonnull
   private String getArrayBrackets()
   {
      @SuppressWarnings("NonConstantStringShouldBeStringBuffer")
      String result = "";

      for (int i = 0; i < arrayDimensions; i++) {
         //noinspection StringContatenationInLoop
         result += "[]";
      }

      return result;
   }

   public void append(@Nonnull String text) { out.append(text); }
}
