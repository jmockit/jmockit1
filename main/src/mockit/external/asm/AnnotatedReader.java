package mockit.external.asm;

import javax.annotation.*;

/**
 * A bytecode reader having an {@link AnnotationReader} for reading the annotations of a class, field, or method.
 */
class AnnotatedReader extends BytecodeReader
{
   @Nonnull final AnnotationReader annotationReader = new AnnotationReader(this);
   @Nonnegative int annotationsCodeIndex;

   /**
    * The access flags of the class, field, or method currently being parsed.
    */
   int access;

   AnnotatedReader(@Nonnull byte[] code) { super(code); }
   AnnotatedReader(@Nonnull AnnotatedReader another) { super(another); }

   final void readAccessAttribute(@Nonnull String attrName) {
      if ("Deprecated".equals(attrName)) {
         access = Access.asDeprecated(access);
      }
      else if ("Synthetic".equals(attrName)) {
         access = Access.asSynthetic(access);
      }
   }

   final void readAnnotations(@Nonnull BaseWriter visitor) {
      if (annotationsCodeIndex > 0) {
         int previousCodeIndex = codeIndex;
         codeIndex = annotationsCodeIndex;

         for (int annotationCount = readUnsignedShort(); annotationCount > 0; annotationCount--) {
            String annotationTypeDesc = readNonnullUTF8();
            AnnotationVisitor av = visitor.visitAnnotation(annotationTypeDesc);

            codeIndex = annotationReader.readNamedAnnotationValues(codeIndex, av);
         }

         codeIndex = previousCodeIndex;
      }
   }
}
