package mockit.external.asm;

import javax.annotation.*;

final class SignatureWriter extends AttributeWriter
{
   @Nonnegative private final int signatureIndex;

   SignatureWriter(@Nonnull ConstantPoolGeneration cp, @Nonnull String signature) {
      super(cp, "Signature");
      signatureIndex = cp.newUTF8(signature);
   }

   @Nonnegative @Override
   int getSize() { return 8; }

   @Override
   void put(@Nonnull ByteVector out) {
      super.put(out);
      out.putShort(signatureIndex);
   }
}
