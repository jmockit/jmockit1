/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.reflection;

import java.lang.reflect.*;
import javax.annotation.*;

import static mockit.internal.reflection.ParameterReflection.*;

public final class AnnotationReflection
{
   private AnnotationReflection() {}

   @Nonnull
   public static String readAnnotationAttribute(@Nonnull Object annotationInstance, @Nonnull String attributeName) {
      try { return readAttribute(annotationInstance, attributeName); } catch (NoSuchMethodException e) { throw new RuntimeException(e); }
   }

   @Nullable
   public static String readAnnotationAttributeIfAvailable(@Nonnull Object annotationInstance, @Nonnull String attributeName) {
      try { return readAttribute(annotationInstance, attributeName); } catch (NoSuchMethodException e) { return null; }
   }

   @Nonnull
   private static String readAttribute(@Nonnull Object annotationInstance, @Nonnull String attributeName) throws NoSuchMethodException {
      try {
         Method publicMethod = annotationInstance.getClass().getMethod(attributeName, NO_PARAMETERS);
         String result = (String) publicMethod.invoke(annotationInstance);
         return result;
      }
      catch (IllegalAccessException e) { throw new RuntimeException(e); }
      catch (InvocationTargetException e) { throw new RuntimeException(e.getCause()); }
   }
}
