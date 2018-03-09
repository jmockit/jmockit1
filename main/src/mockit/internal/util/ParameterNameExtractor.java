/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import java.util.*;
import javax.annotation.*;

import mockit.asm.*;
import mockit.asm.ClassMetadataReader.*;
import mockit.internal.*;
import mockit.internal.state.*;
import static mockit.internal.util.Utilities.JAVA_VERSION;

public final class ParameterNameExtractor
{
   private static final EnumSet<Attribute> PARAMETERS = EnumSet.of(Attribute.Parameters);

   private ParameterNameExtractor() {}

   @Nonnull
   public static String extractNames(@Nonnull Class<?> classOfInterest) {
      String className = classOfInterest.getName();
      String classDesc = className.replace('.', '/');

      if (!ParameterNames.hasNamesForClass(classDesc)) {
         // JRE 1.6 (but not 1.7+) discards parameter names on retransformation.
         byte[] classfile = JAVA_VERSION < 1.7F ? ClassFile.readBytesFromClassFile(classDesc) : ClassFile.getClassFile(classOfInterest);
         ClassMetadataReader cmr = new ClassMetadataReader(classfile, PARAMETERS);
         List<MethodInfo> methods = cmr.getMethods();

         for (MethodInfo method : methods) {
            if (!method.isSynthetic()) {
               String[] parameters = method.parameters;

               if (parameters != null) {
                  ParameterNames.register(classDesc, method.name, method.desc, parameters);
               }
            }
         }
      }

      return classDesc;
   }
}
