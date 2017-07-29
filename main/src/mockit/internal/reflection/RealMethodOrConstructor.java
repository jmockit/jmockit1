/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.reflection;

import java.lang.reflect.*;
import javax.annotation.*;

import mockit.internal.util.*;

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

   public RealMethodOrConstructor(@Nonnull Class<?> realClass, @Nonnull String methodName, @Nonnull String memberDesc)
      throws NoSuchMethodException
   {
      if (methodName.charAt(0) == '<') {
         member = findConstructor(realClass, memberDesc);
      }
      else {
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
            Method method = ownerClass.getDeclaredMethod(methodName, parameterTypes);

            if (method.isBridge()) {
               ownerClass = ownerClass.getSuperclass();
               continue;
            }

            return method;
         }
         catch (NoSuchMethodException e) {
            Method interfaceMethod = findInterfaceMethod(ownerClass, methodName, parameterTypes);

            if (interfaceMethod != null) {
               return interfaceMethod;
            }

            ownerClass = ownerClass.getSuperclass();

            if (ownerClass == null || ownerClass == Object.class) {
               throw e;
            }
         }
      }
   }

   @Nullable
   private static Method findInterfaceMethod(
      @Nonnull Class<?> aType, @Nonnull String methodName, @Nonnull Class<?>[] parameterTypes)
   {
      for (Class<?> anInterface : aType.getInterfaces()) {
         try { return anInterface.getMethod(methodName, parameterTypes); } catch (NoSuchMethodException ignore) {}
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
