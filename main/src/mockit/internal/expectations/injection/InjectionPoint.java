/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import java.lang.annotation.*;
import java.lang.reflect.*;
import javax.annotation.*;
import javax.ejb.EJB;
import javax.inject.*;
import javax.persistence.*;
import javax.servlet.GenericServlet;

import static mockit.internal.util.ClassLoad.*;

import org.jetbrains.annotations.*;

final class InjectionPoint
{
   @Nullable static final Class<? extends Annotation> INJECT_CLASS;
   @Nullable private static final Class<? extends Annotation> EJB_CLASS;
   @Nullable static final Class<? extends Annotation> PERSISTENCE_UNIT_CLASS;
   @Nullable private static final Class<?> SERVLET_CLASS;
   static final boolean WITH_INJECTION_API_IN_CLASSPATH;

   static
   {
      INJECT_CLASS = searchTypeInClasspath("javax.inject.Inject");
      EJB_CLASS = searchTypeInClasspath("javax.ejb.EJB");
      PERSISTENCE_UNIT_CLASS = searchTypeInClasspath("javax.persistence.PersistenceUnit");
      SERVLET_CLASS = searchTypeInClasspath("javax.servlet.GenericServlet");
      WITH_INJECTION_API_IN_CLASSPATH = INJECT_CLASS != null || PERSISTENCE_UNIT_CLASS != null;
   }

   static boolean isServlet(@NotNull Class<?> aClass)
   {
      return SERVLET_CLASS != null && GenericServlet.class.isAssignableFrom(aClass);
   }

   private InjectionPoint() {}

   @NotNull
   static Object wrapInProviderIfNeeded(@NotNull Type type, @NotNull final Object value)
   {
      if (
         INJECT_CLASS != null && type instanceof ParameterizedType && !(value instanceof Provider) &&
         ((ParameterizedType) type).getRawType() == Provider.class
      ) {
         return new Provider<Object>() { @Override public Object get() { return value; } };
      }

      return value;
   }

   static boolean isAnnotated(@NotNull Field field)
   {
      return
         field.isAnnotationPresent(Resource.class) ||
         INJECT_CLASS != null && field.isAnnotationPresent(Inject.class) ||
         EJB_CLASS != null && field.isAnnotationPresent(EJB.class) ||
         PERSISTENCE_UNIT_CLASS != null && (
            field.isAnnotationPresent(PersistenceContext.class) || field.isAnnotationPresent(PersistenceUnit.class)
         );
   }

   @NotNull
   static Type getTypeOfInjectionPointFromVarargsParameter(@NotNull Type[] parameterTypes, int varargsParameterIndex)
   {
      Type parameterType = parameterTypes[varargsParameterIndex];

      if (parameterType instanceof Class<?>) {
         return ((Class<?>) parameterType).getComponentType();
      }
      else {
         return ((GenericArrayType) parameterType).getGenericComponentType();
      }
   }
}
