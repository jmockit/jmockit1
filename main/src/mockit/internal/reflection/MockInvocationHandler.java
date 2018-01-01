/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.reflection;

import java.lang.annotation.*;
import java.lang.reflect.*;
import javax.annotation.*;

import mockit.internal.util.*;

/**
 * Handles invocations to all kinds of mock implementations created for interfaces and annotation types through any of
 * the mocking APIs.
 * <p/>
 * The <tt>java.lang.Object</tt> methods <tt>equals</tt>, <tt>hashCode</tt>, and <tt>toString</tt> are handled in a
 * meaningful way, returning a value that makes sense for the proxy instance.
 * The special {@linkplain Annotation} contracts for these three methods is <em>not</em> observed, though, since it
 * would require making dynamic calls to the mocked annotation attributes.
 * <p/>
 * Any other method invocation is handled by simply returning the default value according to the method's return type
 * (as defined in {@linkplain DefaultValues}).
 */
public final class MockInvocationHandler implements InvocationHandler
{
   public static final InvocationHandler INSTANCE = new MockInvocationHandler();

   @Nullable @Override
   public Object invoke(@Nonnull Object proxy, @Nonnull Method method, @Nullable Object[] args)
   {
      Class<?> declaringClass = method.getDeclaringClass();
      String methodName = method.getName();

      if (declaringClass == Object.class) {
         if ("equals".equals(methodName)) {
            assert args != null;
            return proxy == args[0];
         }
         else if ("hashCode".equals(methodName)) {
            return System.identityHashCode(proxy);
         }
         else { // "toString"
            return ObjectMethods.objectIdentity(proxy);
         }
      }

      if (declaringClass == Annotation.class) {
         return proxy.getClass().getInterfaces()[0];
      }

      Class<?> retType = method.getReturnType();
      return DefaultValues.computeForType(retType);
   }
}
