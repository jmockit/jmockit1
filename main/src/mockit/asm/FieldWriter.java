package mockit.asm;

import javax.annotation.*;

import mockit.asm.types.*;

/**
 * A {@link FieldVisitor} that generates Java fields in bytecode form.
 */
final class FieldWriter extends FieldVisitor
{
   /**
    * The index of the constant pool item that contains the name of this field.
    */
   @Nonnegative private final int nameItemIndex;

   /**
    * The index of the constant pool item that contains the descriptor of this field.
    */
   @Nonnegative private final int descItemIndex;

   @Nullable private final SignatureWriter signatureWriter;

   /**
    * The index of the constant pool item that contains the constant value of this field.
    */
   @Nonnegative private final int valueItemIndex;

   /**
    * Initializes a new Field Writer.
    *
    * @param cw        the class writer to which this field must be added.
    * @param access    the field's access flags (see {@link Opcodes}).
    * @param name      the field's name.
    * @param desc      the field's descriptor (see {@link JavaType}).
    * @param signature the field's signature.
    * @param value     the field's constant value.
    */
   FieldWriter(
      @Nonnull ClassWriter cw, int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable Object value
   ) {
      cp = cw.cp;
      classOrMemberAccess = access;
      nameItemIndex = cp.newUTF8(name);
      descItemIndex = cp.newUTF8(desc);
      signatureWriter = signature == null ? null : new SignatureWriter(cp, signature);
      valueItemIndex = value == null ? 0 : cp.newConstItem(value).index;

      createMarkerAttributes(cw.classVersion);
   }

   /**
    * Returns the size of this field.
    */
   @Nonnegative
   int getSize() {
      int size = 8 + getMarkerAttributesSize() + getAnnotationsSize();

      if (valueItemIndex != 0) {
         cp.newUTF8("ConstantValue");
         size += 8;
      }

      if (signatureWriter != null) {
         size += signatureWriter.getSize();
      }

      return size;
   }

   /**
    * Puts the content of this field into the given byte vector.
    */
   @Override
   void put(@Nonnull ByteVector out) {
      putAccess(out, 0);
      out.putShort(nameItemIndex);
      out.putShort(descItemIndex);

      int attributeCount = getAttributeCount();
      out.putShort(attributeCount);

      if (valueItemIndex != 0) {
         out.putShort(cp.newUTF8("ConstantValue"));
         out.putInt(2).putShort(valueItemIndex);
      }

      putMarkerAttributes(out);

      if (signatureWriter != null) {
         signatureWriter.put(out);
      }

      putAnnotations(out);
   }

   @Nonnegative
   private int getAttributeCount() {
      int attributeCount = getMarkerAttributeCount();

      if (valueItemIndex != 0) {
         attributeCount++;
      }

      if (signatureWriter != null) {
         attributeCount++;
      }

      if (annotations != null) {
         attributeCount++;
      }

      return attributeCount;
   }
}
