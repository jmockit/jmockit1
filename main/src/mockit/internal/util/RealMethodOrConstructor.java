/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import java.lang.reflect.*;

import org.jetbrains.annotations.*;

public final class RealMethodOrConstructor
{
   @NotNull public final Member member;

   public RealMethodOrConstructor(@NotNull String classDesc, @NotNull String methodName, @NotNull String methodDesc)
   {
      ClassLoader cl = getClass().getClassLoader();
      Class<?> realClass = ClassLoad.loadFromLoader(cl, classDesc.replace('/', '.'));

      if (methodName.charAt(0) == '<') {
         member = findConstructor(realClass, methodDesc);
      }
      else {
         member = findMethod(realClass, methodName, methodDesc);
      }
   }

   public RealMethodOrConstructor(@NotNull String className, @NotNull String methodNameAndDesc)
   {
      this(ClassLoad.loadFromLoader(RealMethodOrConstructor.class.getClassLoader(), className), methodNameAndDesc);
   }

   public RealMethodOrConstructor(@NotNull Class<?> realClass, @NotNull String methodNameAndDesc)
   {
      int p = methodNameAndDesc.indexOf('(');
      String memberDesc = methodNameAndDesc.substring(p);

      if (methodNameAndDesc.charAt(0) == '<') {
         member = findConstructor(realClass, memberDesc);
      }
      else {
         String methodName = methodNameAndDesc.substring(0, p);
         member = findMethod(realClass, methodName, memberDesc);
      }
   }

   @NotNull
   private Constructor<?> findConstructor(@NotNull Class<?> realClass, @NotNull String methodDesc)
   {
      Class<?>[] parameterTypes = TypeDescriptor.getParameterTypes(methodDesc);
      return ConstructorReflection.findSpecifiedConstructor(realClass, parameterTypes);
   }

   @NotNull
   private Method findMethod(@NotNull Class<?> realClass, @NotNull String methodName, @NotNull String methodDesc)
   {
      Class<?>[] parameterTypes = TypeDescriptor.getParameterTypes(methodDesc);
      Class<?> ownerClass = realClass;

      while (true) {
         try {
            return ownerClass.getDeclaredMethod(methodName, parameterTypes);
         }
         catch (NoSuchMethodException e) {
            if (ownerClass.isInterface()) {
               Method interfaceMethod = findInterfaceMethod(ownerClass, methodName, parameterTypes);

               if (interfaceMethod == null) {
                  throw new RuntimeException(e);
               }

               return interfaceMethod;
            }
            else {
               ownerClass = ownerClass.getSuperclass();
            }

            if (ownerClass == Object.class) {
               throw new RuntimeException(e);
            }
         }
      }
   }

   @Nullable
   private Method findInterfaceMethod(
      @NotNull Class<?> anInterface, @NotNull String methodName, @NotNull Class<?>[] parameterTypes)
   {
      for (Class<?> superInterface : anInterface.getInterfaces()) {
         try { return superInterface.getMethod(methodName, parameterTypes); } catch (NoSuchMethodException ignore) {}
      }

      return null;
   }

   @NotNull public <M extends Member> M getMember()
   {
      //noinspection unchecked
      return (M) member;
   }
}
