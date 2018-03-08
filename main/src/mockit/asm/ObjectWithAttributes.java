package mockit.asm;

import java.util.*;
import javax.annotation.*;

import mockit.asm.ClassMetadataReader.*;

class ObjectWithAttributes {
   @Nullable public List<AnnotationInfo> annotations;

   public final boolean hasAnnotation(@Nonnull String annotationName) {
      if (annotations != null) {
         for (AnnotationInfo annotation : annotations) {
            if (annotationName.equals(annotation.name)) {
               return true;
            }
         }
      }

      return false;
   }
}
