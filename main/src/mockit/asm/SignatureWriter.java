package mockit.asm;

import javax.annotation.*;

import mockit.asm.constantPool.*;
import mockit.asm.util.*;

public final class SignatureWriter extends AttributeWriter
{
   @Nonnegative private final int signatureIndex;

   public SignatureWriter(@Nonnull ConstantPoolGeneration cp, @Nonnull String signature) {
      super(cp, "Signature");
      signatureIndex = cp.newUTF8(signature);
   }

   @Nonnegative @Override
   public int getSize() { return 8; }

   @Override
   public void put(@Nonnull ByteVector out) {
      super.put(out);
      out.putShort(signatureIndex);
   }
}
