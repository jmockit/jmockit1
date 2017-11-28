package mockit.external.asm;

import javax.annotation.*;

import static mockit.external.asm.ConstantPoolItemType.*;

/**
 * A bytecode reader having an {@link AnnotationReader} for reading the annotations of a class, field, or method.
 */
class AnnotatedReader extends BytecodeReader
{
   @Nonnull final AnnotationReader annotationReader = new AnnotationReader(this);

   /**
    * Maximum length of the strings contained in the constant pool of the class.
    */
   @Nonnegative final int maxStringLength;

   /**
    * The buffer used to read strings.
    */
   @Nonnull final char[] buf;

   /**
    * Start index of the class header information (access, name...) in {@link #code}.
    */
   @Nonnegative final int header;

   Context context;

   AnnotatedReader(@Nonnull byte[] code) {
      super(code);

      int maxSize = 0;
      int index = 10;

      for (int i = 1, n = items.length; i < n; i++) {
         items[i] = index + 1;
         int size;

         switch (code[index]) {
            case FIELD:
            case METH:
            case IMETH:
            case INT:
            case FLOAT:
            case NAME_TYPE:
            case INDY:
               size = 5;
               break;
            case LONG:
            case DOUBLE:
               size = 9;
               i++;
               break;
            case UTF8:
               size = 3 + readUnsignedShort(index + 1);

               if (size > maxSize) {
                  maxSize = size;
               }

               break;
            case HANDLE:
               size = 4;
               break;
            // case CLASS|STR|MTYPE
            default:
               size = 3;
               break;
         }

         index += size;
      }

      maxStringLength = maxSize;
      buf = new char[maxSize];
      header = index; // the class header information starts just after the constant pool
   }

   AnnotatedReader(@Nonnull AnnotatedReader another) {
      super(another);
      maxStringLength = another.maxStringLength;
      buf = another.buf;
      header = another.header;
   }

   /**
    * Reads a class constant pool item in {@link #code}.
    *
    * @param index the start index of an unsigned short value in {@link #code}, whose value is the index of a class
    *              constant pool item.
    * @return the String corresponding to the specified class item.
    */
   @Nullable
   final String readClass(@Nonnegative int index) {
      return readClass(index, buf);
   }
}
