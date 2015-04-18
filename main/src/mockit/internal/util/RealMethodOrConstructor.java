/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import java.lang.reflect.*;
import javax.annotation.*;

public final class RealMethodOrConstructor
{
   @Nonnull public final Member member;

   public RealMethodOrConstructor(@Nonnull String classDesc, @Nonnull String methodName, @Nonnull String methodDesc)
      throws NoSuchMethodException
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

   public RealMethodOrConstructor(@Nonnull String className, @Nonnull String methodNameAndDesc)
      throws NoSuchMethodException
   {
      this(ClassLoad.loadFromLoader(RealMethodOrConstructor.class.getClassLoader(), className), methodNameAndDesc);
   }

   public RealMethodOrConstructor(@Nonnull Class<?> realClass, @Nonnull String methodNameAndDesc)
      throws NoSuchMethodException
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

   @Nonnull
   private static Constructor<?> findConstructor(@Nonnull Class<?> realClass, @Nonnull String methodDesc)
   {
      Class<?>[] parameterTypes = TypeDescriptor.getParameterTypes(methodDesc);
      return ConstructorReflection.findSpecifiedConstructor(realClass, parameterTypes);
   }

   @Nonnull
   private static Method findMethod(@Nonnull Class<?> realClass, @Nonnull String methodName, @Nonnull String methodDesc)
      throws NoSuchMethodException
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
                  throw e;
               }

               return interfaceMethod;
            }
            else {
               ownerClass = ownerClass.getSuperclass();
            }

            if (ownerClass == Object.class) {
               throw e;
            }
         }
      }
   }

   @Nullable
   private static Method findInterfaceMethod(
      @Nonnull Class<?> anInterface, @Nonnull String methodName, @Nonnull Class<?>[] parameterTypes)
   {
      for (Class<?> superInterface : anInterface.getInterfaces()) {
         try { return superInterface.getMethod(methodName, parameterTypes); } catch (NoSuchMethodException ignore) {}
      }

      return null;
   }

   @Nonnull
   public <M extends Member> M getMember()
   {
      //noinspection unchecked
      return (M) member;
   }
}
