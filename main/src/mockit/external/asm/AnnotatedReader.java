package mockit.external.asm;

import javax.annotation.*;

/**
 * A bytecode reader having an {@link AnnotationReader} for reading the annotations of a class, field, or method.
 */
class AnnotatedReader extends BytecodeReader
{
   final AnnotationReader annotationReader = new AnnotationReader(this);

   AnnotatedReader(@Nonnull byte[] bytecode) { super(bytecode); }
   AnnotatedReader(@Nonnull BytecodeReader another) { super(another); }
}
