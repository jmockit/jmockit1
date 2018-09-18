package mockit.asm;

import javax.annotation.Nonnull;

public class NestMembersWriter extends AttributeWriter
{
   @Nonnull private final ByteVector nestMembers;

   public NestMembersWriter(@Nonnull ConstantPoolGeneration cp) {
      super(cp, "NestMembers");
      nestMembers = new ByteVector();
   }

   @Override
   int getSize() {
      return nestMembers.length + 8;
   }

   void add(String nestMember) {
      int index = cp.newClass(nestMember);
      nestMembers.putShort(index);
   }

   @Override
   void put(@Nonnull ByteVector out) {
      put(out, nestMembers.length + 2);
      out.putShort(nestMembers.length / 2).putByteVector(nestMembers);
   }
}
