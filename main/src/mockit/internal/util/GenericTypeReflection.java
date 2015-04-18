/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.*;
import javax.annotation.*;
import static java.util.Collections.*;

public final class GenericTypeReflection
{
   @Nonnull private final Map<String, Type> typeParametersToTypeArguments;
   @Nonnull private final Map<String, String> typeParametersToTypeArgumentNames;
   private final boolean withSignatures;

   public GenericTypeReflection(@Nonnull Class<?> realClass, @Nullable Type mockedType)
   {
      typeParametersToTypeArguments = new HashMap<String, Type>(4);
      typeParametersToTypeArgumentNames = new HashMap<String, String>(4);
      withSignatures = true;
      discoverTypeMappings(realClass, mockedType);
   }

   public GenericTypeReflection(@Nonnull Field field)
   {
      typeParametersToTypeArguments = new HashMap<String, Type>(4);
      typeParametersToTypeArgumentNames = emptyMap();
      withSignatures = false;
      discoverTypeMappings(field.getType(), field.getGenericType());
   }

   private void discoverTypeMappings(@Nonnull Class<?> realClass, @Nullable Type mockedType)
   {
      if (mockedType instanceof ParameterizedType) {
         addMappingsFromTypeParametersToTypeArguments(realClass, (ParameterizedType) mockedType);
      }

      addGenericTypeMappingsForSuperTypes(realClass);
   }

   private void addGenericTypeMappingsForSuperTypes(@Nonnull Class<?> realClass)
   {
      Type superType = realClass;

      while (superType instanceof Class<?> && superType != Object.class) {
         Class<?> superClass = (Class<?>) superType;
         superType = superClass.getGenericSuperclass();

         if (superType != null) {
            superType = addGenericTypeMappingsIfParameterized(superType);
         }

         for (Type implementedInterface : superClass.getGenericInterfaces()) {
            addGenericTypeMappingsIfParameterized(implementedInterface);
         }
      }
   }

   @Nonnull
   private Type addGenericTypeMappingsIfParameterized(@Nonnull Type superType)
   {
      if (superType instanceof ParameterizedType) {
         ParameterizedType mockedSuperType = (ParameterizedType) superType;
         Type rawType = mockedSuperType.getRawType();
         addMappingsFromTypeParametersToTypeArguments((Class<?>) rawType, mockedSuperType);
         return rawType;
      }

      return superType;
   }

   private void addMappingsFromTypeParametersToTypeArguments(
      @Nonnull Class<?> mockedClass, @Nonnull ParameterizedType mockedType)
   {
      TypeVariable<?>[] typeParameters = mockedClass.getTypeParameters();
      Type[] typeArguments = mockedType.getActualTypeArguments();
      int n = typeParameters.length;

      for (int i = 0; i < n; i++) {
         TypeVariable<?> typeParam = typeParameters[i];
         String typeVarName = typeParam.getName();

         if (typeParametersToTypeArguments.containsKey(typeVarName)) {
            continue;
         }

         Type typeArg = typeArguments[i];
         Type mappedTypeArg;
         String mappedTypeArgName = null;

         if (typeArg instanceof Class<?>) {
            mappedTypeArg = typeArg;

            if (withSignatures) {
               mappedTypeArgName = 'L' + ((Class<?>) typeArg).getName().replace('.', '/');
            }
         }
         else if (typeArg instanceof TypeVariable<?>) {
            mappedTypeArg = typeArg;

            if (withSignatures) {
               String intermediateTypeArg = 'T' + ((TypeVariable<?>) typeArg).getName();
               mappedTypeArgName = typeParametersToTypeArgumentNames.get(intermediateTypeArg);
               // TODO: assert mappedTypeArgName != null : "Intermediate type argument not found: " + typeArg;
            }
         }
         else {
            mappedTypeArg = typeParam.getBounds()[0];

            if (withSignatures) {
               mappedTypeArgName = 'L' + getClassType(mappedTypeArg).getName().replace('.', '/');
            }
         }

         addTypeMapping(typeVarName, mappedTypeArg, mappedTypeArgName);
      }
   }

   @Nonnull
   private static Class<?> getClassType(@Nonnull Type type)
   {
      if (type instanceof ParameterizedType) {
         return (Class<?>) ((ParameterizedType) type).getRawType();
      }

      return (Class<?>) type;
   }

   private void addTypeMapping(
      @Nonnull String typeVarName, @Nonnull Type mappedTypeArg, @Nullable String mappedTypeArgName)
   {
      typeParametersToTypeArguments.put(typeVarName, mappedTypeArg);

      if (mappedTypeArgName != null) {
         addTypeMapping(typeVarName, mappedTypeArgName);
      }
   }

   private void addTypeMapping(@Nonnull String typeVarName, @Nonnull String mappedTypeArgName)
   {
      typeParametersToTypeArgumentNames.put('T' + typeVarName, mappedTypeArgName);
   }

   public final class GenericSignature
   {
      private final List<String> parameters;
      private final String parameterTypeDescs;
      private final int lengthOfParameterTypeDescs;
      private int currentPos;

      GenericSignature(@Nonnull String signature)
      {
         int p = signature.indexOf('(');
         int q = signature.lastIndexOf(')');
         parameterTypeDescs = signature.substring(p + 1, q);
         lengthOfParameterTypeDescs = parameterTypeDescs.length();
         parameters = new ArrayList<String>();
         addTypeDescsToList();
      }

      private void addTypeDescsToList()
      {
         while (currentPos < lengthOfParameterTypeDescs) {
            addNextParameter();
         }
      }

      private void addNextParameter()
      {
         int startPos = currentPos;
         int endPos;
         char c = parameterTypeDescs.charAt(startPos);

         if (c == 'T') {
            endPos = parameterTypeDescs.indexOf(';', startPos);
            currentPos = endPos;
         }
         else if (c == 'L') {
            endPos = advanceToEndOfTypeDesc();
         }
         else if (c == '[') {
            char elemTypeStart = firstCharacterOfArrayElementType();

            if (elemTypeStart == 'T') {
               endPos = parameterTypeDescs.indexOf(';', startPos);
               currentPos = endPos;
            }
            else if (elemTypeStart == 'L') {
               endPos = advanceToEndOfTypeDesc();
            }
            else {
               endPos = currentPos + 1;
            }
         }
         else {
            endPos = currentPos + 1;
         }

         currentPos++;
         String parameter = parameterTypeDescs.substring(startPos, endPos);
         parameters.add(parameter);
      }

      private int advanceToEndOfTypeDesc()
      {
         char c = '\0';

         do {
            currentPos++;
            if (currentPos == lengthOfParameterTypeDescs) break;
            c = parameterTypeDescs.charAt(currentPos);
         } while (c != ';' && c != '<');

         int endPos = currentPos;

         if (c == '<') {
            advancePastTypeArguments();
            currentPos++;
         }

         return endPos;
      }

      private char firstCharacterOfArrayElementType()
      {
         char c;

         do {
            currentPos++;
            c = parameterTypeDescs.charAt(currentPos);
         } while (c == '[');

         return c;
      }

      private void advancePastTypeArguments()
      {
         int angleBracketDepth = 1;

         do {
            currentPos++;
            char c = parameterTypeDescs.charAt(currentPos);
            if (c == '>') angleBracketDepth--; else if (c == '<') angleBracketDepth++;
         } while (angleBracketDepth > 0);
      }

      public boolean satisfiesGenericSignature(@Nonnull String otherSignature)
      {
         GenericSignature other = new GenericSignature(otherSignature);
         int n = parameters.size();

         if (n != other.parameters.size()) {
            return false;
         }

         for (int i = 0; i < n; i++) {
            String p1 = other.parameters.get(i);
            String p2 = parameters.get(i);

            if (!areParametersOfSameType(p1, p2)) {
               return false;
            }
         }

         return true;
      }

      private boolean areParametersOfSameType(@Nonnull String param1, @Nonnull String param2)
      {
         if (param1.equals(param2)) return true;

         int i = -1;
         char c;
         do { i++; c = param1.charAt(i); } while (c == '[');
         if (c != 'T') return false;

         String typeArg1 = typeParametersToTypeArgumentNames.get(param1.substring(i));
         return param2.substring(i).equals(typeArg1);
      }
   }

   @Nonnull
   public GenericSignature parseSignature(@Nonnull String signature)
   {
      return new GenericSignature(signature);
   }

   @Nonnull
   public String resolveReturnType(@Nonnull String signature)
   {
      addTypeArgumentsIfAvailable(signature);

      int p = signature.lastIndexOf(')') + 1;
      int q = signature.length();
      String returnType = signature.substring(p, q);
      String resolvedReturnType = replaceTypeParametersWithActualTypes(returnType);

      StringBuilder finalSignature = new StringBuilder(signature);
      finalSignature.replace(p, q, resolvedReturnType);
      return finalSignature.toString();
   }

   private void addTypeArgumentsIfAvailable(@Nonnull String signature)
   {
      int firstParen = signature.indexOf('(');
      if (firstParen == 0) return;

      int p = 1;
      boolean lastMappingFound = false;

      while (!lastMappingFound) {
         int q = signature.indexOf(':', p);
         String typeVar = signature.substring(p, q);

         q++;

         if (signature.charAt(q) == ':') {
            q++; // an unbounded type argument uses ":" as separator, while a bounded one uses "::"
         }

         int r = signature.indexOf(':', q);

         if (r < 0) {
            r = firstParen - 2;
            lastMappingFound = true;
         }
         else {
            r = signature.lastIndexOf(';', r);
            p = r + 1;
         }

         String typeArg = signature.substring(q, r);
         addTypeMapping(typeVar, typeArg);
      }
   }

   @Nonnull
   private String replaceTypeParametersWithActualTypes(@Nonnull String typeDesc)
   {
      if (typeDesc.charAt(0) == 'T') {
         String typeParameter = typeDesc.substring(0, typeDesc.length() - 1);
         String typeArg = typeParametersToTypeArgumentNames.get(typeParameter);
         return typeArg == null ? typeDesc : typeArg + ';';
      }

      int p = typeDesc.indexOf('<');

      if (p < 0) {
         return typeDesc;
      }

      String resolvedTypeDesc = typeDesc;

      for (Entry<String, String> paramAndArg : typeParametersToTypeArgumentNames.entrySet()) {
         String typeParam = paramAndArg.getKey() + ';';
         String typeArg = paramAndArg.getValue() + ';';
         resolvedTypeDesc = resolvedTypeDesc.replace(typeParam, typeArg);
      }

      return resolvedTypeDesc;
   }

   @Nonnull
   public Type resolveReturnType(@Nonnull TypeVariable<?> genericReturnType)
   {
      Type typeArgument = typeParametersToTypeArguments.get(genericReturnType.getName());

      if (typeArgument == null) {
         typeArgument = genericReturnType.getBounds()[0];

         if (typeArgument instanceof TypeVariable<?>) {
            typeArgument = resolveReturnType((TypeVariable<?>) typeArgument);
         }
      }

      return typeArgument;
   }

   public boolean areMatchingTypes(@Nonnull Type declarationType, @Nonnull Type realizationType)
   {
      if (declarationType.equals(realizationType)) {
         return true;
      }

      if (declarationType instanceof TypeVariable<?>) {
         if (realizationType instanceof TypeVariable<?>) {
            return false;
         }

         if (areMatchingTypes((TypeVariable<?>) declarationType, realizationType)) {
            return true;
         }
      }

      return
         declarationType instanceof ParameterizedType && realizationType instanceof ParameterizedType &&
         areMatchingTypes((ParameterizedType) declarationType, (ParameterizedType) realizationType);
   }

   private boolean areMatchingTypes(@Nonnull TypeVariable<?> declarationType, @Nonnull Type realizationType)
   {
      Type resolvedType = typeParametersToTypeArguments.get(declarationType.getName());
      return resolvedType.equals(realizationType) || typeSatisfiesResolvedTypeVariable(resolvedType, realizationType);
   }

   private boolean areMatchingTypes(
      @Nonnull ParameterizedType declarationType, @Nonnull ParameterizedType realizationType)
   {
      if (!declarationType.getRawType().equals(realizationType.getRawType())) {
         return false;
      }

      return haveMatchingActualTypeArguments(declarationType, realizationType);
   }

   private boolean haveMatchingActualTypeArguments(
      @Nonnull ParameterizedType declarationType, @Nonnull ParameterizedType realizationType)
   {
      Type[] declaredTypeArguments = declarationType.getActualTypeArguments();
      Type[] concreteTypeArguments = realizationType.getActualTypeArguments();

      for (int i = 0, n = declaredTypeArguments.length; i < n; i++) {
         Type declaredTypeArg = declaredTypeArguments[i];
         Type concreteTypeArg = concreteTypeArguments[i];

         if (declaredTypeArg instanceof TypeVariable<?>) {
            if (areMatchingTypeArguments((TypeVariable<?>) declaredTypeArg, concreteTypeArg)) {
               continue;
            }
         }
         else if (areMatchingTypes(declaredTypeArg, concreteTypeArg)) {
            continue;
         }

         return false;
      }

      return true;
   }

   private boolean areMatchingTypeArguments(@Nonnull TypeVariable<?> declaredType, @Nonnull Type concreteType)
   {
      Type resolvedType = typeParametersToTypeArguments.get(declaredType.getName());

      if (resolvedType != null) {
         if (resolvedType.equals(concreteType)) {
            return true;
         }

         if (
            concreteType instanceof Class<?> &&
            typeSatisfiesResolvedTypeVariable(resolvedType, (Class<?>) concreteType)
         ) {
            return true;
         }

         if (
            concreteType instanceof WildcardType &&
            typeSatisfiesUpperBounds(resolvedType, ((WildcardType) concreteType).getUpperBounds())
         ) {
            return true;
         }
      }
      else if (typeSatisfiesUpperBounds(concreteType, declaredType.getBounds())) {
         return true;
      }

      return false;
   }

   private static boolean typeSatisfiesResolvedTypeVariable(@Nonnull Type resolvedType, @Nonnull Type realizationType)
   {
      Class<?> realizationClass = getClassType(realizationType);
      return typeSatisfiesResolvedTypeVariable(resolvedType, realizationClass);
   }

   private static boolean typeSatisfiesResolvedTypeVariable(
      @Nonnull Type resolvedType, @Nonnull Class<?> realizationType)
   {
      Class<?> resolvedClass = getClassType(resolvedType);
      return resolvedClass.isAssignableFrom(realizationType);
   }

   private static boolean typeSatisfiesUpperBounds(@Nonnull Type type, @Nonnull Type[] upperBounds)
   {
      Class<?> classType = getClassType(type);

      for (Type upperBound : upperBounds) {
         Class<?> classBound = getClassType(upperBound);

         if (!classBound.isAssignableFrom(classType)) {
            return false;
         }
      }

      return true;
   }
}
