package mockit.external.asm;

import javax.annotation.*;

/**
 * A bytecode reader for reading common elements (signature, annotations) of a class, field, or method.
 */
abstract class AnnotatedReader extends BytecodeReader
{
   @Nonnull private final AnnotationReader annotationReader = new AnnotationReader(this);
   @Nonnegative private int annotationsCodeIndex;

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

   final void readAttributes() {
      signature = null;
      annotationsCodeIndex = 0;

      for (int attributeCount = readUnsignedShort(); attributeCount > 0; attributeCount--) {
         String attributeName = readNonnullUTF8();
         int codeOffsetToNextAttribute = readInt();

         if ("Signature".equals(attributeName)) {
            signature = readNonnullUTF8();
            continue;
         }

         Boolean outcome = readAttribute(attributeName);

         if (outcome == Boolean.TRUE) {
            continue;
         }

         if (outcome == null) {
            if ("RuntimeVisibleAnnotations".equals(attributeName)) {
               annotationsCodeIndex = codeIndex;
            }
            else if ("Deprecated".equals(attributeName)) {
               access = Access.asDeprecated(access);
            }
            else if ("Synthetic".equals(attributeName)) {
               access = Access.asSynthetic(access);
            }
         }

         codeIndex += codeOffsetToNextAttribute;
      }
   }

   @Nullable
   abstract Boolean readAttribute(@Nonnull String attributeName);

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
