package mockit.asm;

import javax.annotation.*;

import mockit.asm.annotations.*;
import mockit.asm.jvmConstants.*;
import mockit.asm.util.*;

/**
 * A bytecode reader for reading common elements (signature, annotations) of a class, field, or method.
 */
public abstract class AnnotatedReader extends BytecodeReader
{
   @Nonnull private final AnnotationReader annotationReader = new AnnotationReader(this);
   @Nonnegative private int annotationsCodeIndex;

   /**
    * The access flags of the class, field, or method currently being parsed.
    */
   protected int access;

   /**
    * The generic type signature of the class/field/method, if it has one.
    */
   @Nullable protected String signature;

   protected AnnotatedReader(@Nonnull byte[] code) { super(code); }
   protected AnnotatedReader(@Nonnull AnnotatedReader another) { super(another); }

   protected final void readAttributes() {
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
            //noinspection SwitchStatementWithoutDefaultBranch
            switch (attributeName) {
               case "RuntimeVisibleAnnotations": annotationsCodeIndex = codeIndex; break;
               case "Deprecated": access = Access.asDeprecated(access); break;
               case "Synthetic":  access = Access.asSynthetic(access);
            }
         }

         codeIndex += codeOffsetToNextAttribute;
      }
   }

   @Nullable
   protected abstract Boolean readAttribute(@Nonnull String attributeName);

   protected final void readAnnotations(@Nonnull BaseWriter visitor) {
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

   protected final void readAnnotationValues(@Nullable AnnotationVisitor av) {
      codeIndex = annotationReader.readNamedAnnotationValues(codeIndex, av);
   }
}