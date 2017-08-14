/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;
import javax.ejb.*;
import javax.enterprise.inject.*;
import javax.enterprise.util.*;
import javax.inject.*;
import javax.persistence.*;
import javax.servlet.*;

import static java.lang.Character.toUpperCase;

import static mockit.internal.util.ClassLoad.*;
import static mockit.internal.reflection.MethodReflection.*;
import static mockit.internal.reflection.ParameterReflection.*;
import static mockit.internal.util.Utilities.*;

public final class InjectionPoint
{
   public enum KindOfInjectionPoint { NotAnnotated, Required, Optional, WithValue }

   @Nullable public static final Class<? extends Annotation> INJECT_CLASS;
   @Nullable private static final Class<? extends Annotation> INSTANCE_CLASS;
   @Nullable private static final Class<? extends Annotation> EJB_CLASS;
   @Nullable public static final Class<? extends Annotation> PERSISTENCE_UNIT_CLASS;
   @Nullable public static final Class<?> SERVLET_CLASS;
   @Nullable public static final Class<?> CONVERSATION_CLASS;

   static
   {
      INJECT_CLASS = searchTypeInClasspath("javax.inject.Inject");
      INSTANCE_CLASS = searchTypeInClasspath("javax.enterprise.inject.Instance");
      EJB_CLASS = searchTypeInClasspath("javax.ejb.EJB");
      SERVLET_CLASS = searchTypeInClasspath("javax.servlet.Servlet");
      CONVERSATION_CLASS = searchTypeInClasspath("javax.enterprise.context.Conversation");

      Class<? extends Annotation> entity = searchTypeInClasspath("javax.persistence.Entity");

      if (entity == null) {
         PERSISTENCE_UNIT_CLASS = null;
      }
      else {
         PERSISTENCE_UNIT_CLASS = searchTypeInClasspath("javax.persistence.PersistenceUnit");
      }
   }

   @Nonnull public final Type type;
   @Nullable public final String name;
   @Nullable private final String normalizedName;
   public final boolean qualified;

   public InjectionPoint(@Nonnull Type type) { this(type, null, false); }
   public InjectionPoint(@Nonnull Type type, @Nullable String name) { this(type, name, false); }

   public InjectionPoint(@Nonnull Type type, @Nullable String name, boolean qualified)
   {
      this.type = type;
      this.name = name;
      normalizedName = name == null ? null : convertToLegalJavaIdentifierIfNeeded(name);
      this.qualified = qualified;
   }

   public InjectionPoint(@Nonnull Type type, @Nonnull String name, @Nullable String qualifiedName)
   {
      this.type = type;
      this.name = qualifiedName == null ? name : qualifiedName;
      normalizedName = this.name;
      qualified = qualifiedName != null;
   }

   @Nonnull
   public static String convertToLegalJavaIdentifierIfNeeded(@Nonnull String name)
   {
      if (name.indexOf('-') < 0 && name.indexOf('.') < 0) {
         return name;
      }

      StringBuilder identifier = new StringBuilder(name);

      for (int i = name.length() - 1; i >= 0; i--) {
         char c = identifier.charAt(i);

         if (c == '-' || c == '.') {
            identifier.deleteCharAt(i);
            c = identifier.charAt(i);
            identifier.setCharAt(i, toUpperCase(c));
         }
      }

      return identifier.toString();
   }

   @Override @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
   public boolean equals(Object other)
   {
      if (this == other) return true;

      InjectionPoint otherIP = (InjectionPoint) other;

      if (type instanceof TypeVariable<?> || otherIP.type instanceof TypeVariable<?>) {
         return false;
      }

      String thisName = normalizedName;

      return type.equals(otherIP.type) && (thisName == null || thisName.equals(otherIP.normalizedName));
   }

   @Override
   public int hashCode() { return 31 * type.hashCode() + (normalizedName == null ? 0 : normalizedName.hashCode()); }

   boolean hasSameName(InjectionPoint otherIP)
   {
      String thisName = normalizedName;
      return thisName != null && thisName.equals(otherIP.normalizedName);
   }

   public static boolean isServlet(@Nonnull Class<?> aClass)
   {
      return SERVLET_CLASS != null && Servlet.class.isAssignableFrom(aClass);
   }

   @Nonnull
   public static Object wrapInProviderIfNeeded(@Nonnull Type type, @Nonnull final Object value)
   {
      if (INJECT_CLASS != null && type instanceof ParameterizedType && !(value instanceof Provider)) {
         Type parameterizedType = ((ParameterizedType) type).getRawType();

         if (parameterizedType == Provider.class) {
            return new Provider<Object>() { @Override public Object get() { return value; } };
         }

         if (INSTANCE_CLASS != null && parameterizedType == Instance.class) {
            @SuppressWarnings("unchecked") List<Object> values = (List<Object>) value;
            return new Listed(values);
         }
      }

      return value;
   }

   private static final class Listed implements Instance<Object>
   {
      @Nonnull private final List<Object> instances;

      Listed(@Nonnull List<Object> instances) { this.instances = instances; }

      @Override public Instance<Object> select(Annotation... annotations) { return null; }
      @Override public <U> Instance<U> select(Class<U> uClass, Annotation... annotations) { return null; }
      @Override public <U> Instance<U> select(TypeLiteral<U> tl, Annotation... annotations) { return null; }
      @Override public boolean isUnsatisfied() { return false; }
      @Override public boolean isAmbiguous() { return false; }
      @Override public void destroy(Object instance) {}
      @Override public Iterator<Object> iterator() { return instances.iterator(); }
      @Override public Object get() { throw new RuntimeException("Unexpected"); }
   }

   @Nonnull
   public static KindOfInjectionPoint kindOfInjectionPoint(@Nonnull AccessibleObject fieldOrConstructor)
   {
      Annotation[] annotations = fieldOrConstructor.getDeclaredAnnotations();

      if (annotations.length == 0) {
         return KindOfInjectionPoint.NotAnnotated;
      }

      if (INJECT_CLASS != null && isAnnotated(annotations, Inject.class)) {
         return KindOfInjectionPoint.Required;
      }

      KindOfInjectionPoint kind = isAutowired(annotations);

      if (kind != KindOfInjectionPoint.NotAnnotated || fieldOrConstructor instanceof Constructor) {
         return kind;
      }

      if (hasValue(annotations)) {
         return KindOfInjectionPoint.WithValue;
      }

      if (isRequired(annotations)) {
         return KindOfInjectionPoint.Required;
      }

      return KindOfInjectionPoint.NotAnnotated;
   }

   private static boolean isAnnotated(
      @Nonnull Annotation[] declaredAnnotations, @Nonnull Class<? extends Annotation> annotationOfInterest)
   {
      Annotation annotation = getAnnotation(declaredAnnotations, annotationOfInterest);
      return annotation != null;
   }

   @Nullable
   private static <A extends Annotation> A getAnnotation(
      @Nonnull Annotation[] declaredAnnotations, @Nonnull Class<A> annotationOfInterest)
   {
      for (Annotation declaredAnnotation : declaredAnnotations) {
         if (declaredAnnotation.annotationType() == annotationOfInterest) {
            //noinspection unchecked
            return (A) declaredAnnotation;
         }
      }

      return null;
   }

   @Nonnull
   private static KindOfInjectionPoint isAutowired(@Nonnull Annotation[] declaredAnnotations)
   {
      for (Annotation declaredAnnotation : declaredAnnotations) {
         Class<? extends Annotation> annotationType = declaredAnnotation.annotationType();

         if (annotationType.getName().endsWith(".Autowired")) {
            Boolean required = invokePublicIfAvailable(annotationType, declaredAnnotation, "required", NO_PARAMETERS);
            return required != null && required ? KindOfInjectionPoint.Required : KindOfInjectionPoint.Optional;
         }
      }

      return KindOfInjectionPoint.NotAnnotated;
   }

   private static boolean hasValue(@Nonnull Annotation[] declaredAnnotations)
   {
      for (Annotation declaredAnnotation : declaredAnnotations) {
         Class<? extends Annotation> annotationType = declaredAnnotation.annotationType();

         if (annotationType.getName().endsWith(".Value")) {
            return true;
         }
      }

      return false;
   }

   private static boolean isRequired(@Nonnull Annotation[] annotations)
   {
      return
         isAnnotated(annotations, Resource.class) ||
         EJB_CLASS != null && isAnnotated(annotations, EJB.class) ||
         PERSISTENCE_UNIT_CLASS != null && (
            isAnnotated(annotations, PersistenceContext.class) || isAnnotated(annotations, PersistenceUnit.class)
         );
   }

   @Nullable
   public static Object getValueFromAnnotation(@Nonnull Field field)
   {
      String value = null;

      for (Annotation declaredAnnotation : field.getDeclaredAnnotations()) {
         Class<? extends Annotation> annotationType = declaredAnnotation.annotationType();

         if (annotationType.getName().endsWith(".Value")) {
            value = invokePublicIfAvailable(annotationType, declaredAnnotation, "value", NO_PARAMETERS);
            break;
         }
      }

      Object convertedValue = convertFromString(field.getType(), value);
      return convertedValue;
   }

   @Nonnull
   public static Type getTypeOfInjectionPointFromVarargsParameter(@Nonnull Type parameterType)
   {
      if (parameterType instanceof Class<?>) {
         return ((Class<?>) parameterType).getComponentType();
      }

      return ((GenericArrayType) parameterType).getGenericComponentType();
   }

   @Nullable
   public static String getQualifiedName(@Nonnull Annotation[] annotationsOnInjectionPoint)
   {
      for (Annotation annotation : annotationsOnInjectionPoint) {
         Class<? extends Annotation> annotationType = annotation.annotationType();
         String annotationName = annotationType.getName();

         if ("javax.annotation.Resource javax.ejb.EJB".contains(annotationName)) {
            String name = readAnnotationAttribute(annotation, "name");

            if (name.isEmpty()) {
               name = readAnnotationAttribute(annotation, "lookup");
               name = getNameFromJNDILookup(name);
            }

            return name;
         }

         if ("javax.inject.Named".equals(annotationName) || annotationName.endsWith(".Qualifier")) {
            String qualifiedName = readAnnotationAttribute(annotation, "value");
            return qualifiedName;
         }
      }

      return null;
   }

   @Nonnull
   public static String getNameFromJNDILookup(@Nonnull String jndiLookup)
   {
      int p = jndiLookup.lastIndexOf('/');

      if (p >= 0) {
         jndiLookup = jndiLookup.substring(p + 1);
      }

      return jndiLookup;
   }
}
