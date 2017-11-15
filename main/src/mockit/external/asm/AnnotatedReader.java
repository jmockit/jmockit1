package mockit.external.asm;

class AnnotatedReader extends BytecodeReader
{
   final AnnotationReader annotationReader = new AnnotationReader(this);

   AnnotatedReader(byte[] bytecode) { super(bytecode); }
   AnnotatedReader(BytecodeReader another) { super(another); }
}
