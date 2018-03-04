package mockit.asm;

/**
 * A visitor to visit a Java field.
 * The methods of this class must be called in the following order: ({@link #visitAnnotation})* {@link #visitEnd}.
 */
public class FieldVisitor extends BaseWriter
{
   FieldVisitor() {}
}
