package mockit.external.asm;

import javax.annotation.*;

/**
 * A bytecode reader for reading common elements (signature, annotations) of a class, field, or method.
 */
class AnnotatedReader extends BytecodeReader
{
   @Nonnull private final AnnotationReader annotationReader = new AnnotationReader(this);
   @Nonnegative int annotationsCodeIndex;

   /**
    * The access flags of the class, field, or method currently being parsed.
    */
   int access;

   /**
    * The generic type signature of the class/field/method, if it has one.
    */
   @Nullable String signature;

   AnnotatedReader(@Nonnull byte[] code) { super(code); }
   AnnotatedReader(@Nonnull AnnotatedReader another) { super(another); }

   final boolean readSignature(@Nonnull String attrName) {
      if ("Signature".equals(attrName)) {
         signature = readNonnullUTF8();
         return true;
      }

      return false;
   }

   final void readMarkerAttributes(@Nonnull String attrName) {
      if ("Deprecated".equals(attrName)) {
         access = Access.asDeprecated(access);
      }
      else if ("Synthetic".equals(attrName)) {
         access = Access.asSynthetic(access);
      }
   }

   final boolean readRuntimeVisibleAnnotations(@Nonnull String attrName) {
      if ("RuntimeVisibleAnnotations".equals(attrName)) {
         annotationsCodeIndex = codeIndex;
         return true;
      }

      return false;
   }

   final void readAnnotations(@Nonnull BaseWriter visitor) {
      if (annotationsCodeIndex > 0) {
         int previousCodeIndex = codeIndex;
         codeIndex = annotationsCodeIndex;

         for (int annotationCount = readUnsignedShort(); annotationCount > 0; annotationCount--) {
            String annotationTypeDesc = readNonnullUTF8();
            AnnotationVisitor av = visitor.visitAnnotation(annotationTypeDesc);
            readAnnotationValues(av);
         }

         codeIndex = previousCodeIndex;
      }
   }

   final void readAnnotationValues(@Nullable AnnotationVisitor av) {
      codeIndex = annotationReader.readNamedAnnotationValues(codeIndex, av);
   }
}
