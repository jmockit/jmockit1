package mockit.asm.jvmConstants;

/**
 * Constant data about all JVM bytecode instructions: {@link #SIZE}, {@link #TYPE}.
 */
public final class JVMInstruction
{
   /**
    * The stack size variation corresponding to each JVM instruction.
    * Said variation equals the size of the values produced by an instruction, minus the size of the values consumed by the instruction.
    */
   public static final int[] SIZE;
   static {
      String s =
         "EFFFFFFFFGGFFFGGFFFEEFGFGFEEEEEEEEEEEEEEEEEEEEDEDEDDDDD" +
         "CDCDEEEEEEEEEEEEEEEEEEEEBABABBBBDCFFFGGGEDCDCDCDCDCDCDCDCD" +
         "CDCEEEEDDDDDDDCDCDCEFEFDDEEFFDEDEEEBDDBBDDDDDDCCCCCCCCEFED" +
         "DDCDCDEEEEEEEEEEFEEEEEEDDEEDDEE";
      int n = s.length();
      int[] sizes = new int[n];

      for (int i = 0; i < n; i++) {
         //noinspection CharUsedInArithmeticContext
         sizes[i] = s.charAt(i) - 'E';
      }

      SIZE = sizes;
   }

   /**
    * Constants that subdivide the 220 {@linkplain Opcodes instruction opcodes} in 18 types of instructions.
    * Such types vary in the number and size of arguments the instruction takes (no argument, a signed byte, a signed short), on whether it
    * takes a local variable index, a jump target label, etc. Some types contain a single instruction, such as LDC and IINC.
    */
   public interface InstructionType {
      int NOARG       = 0; // instructions without any argument
      int SBYTE       = 1; // instructions with a signed byte argument
      int SHORT       = 2; // instructions with a signed short argument
      int VAR         = 3; // instructions with a local variable index argument
      int IMPLVAR     = 4; // instructions with an implicit local variable index argument
      int TYPE_INSN   = 5; // instructions with a type descriptor argument
      int FIELDORMETH = 6; // field and method invocations instructions
      int ITFMETH     = 7; // INVOKEINTERFACE/INVOKEDYNAMIC instruction
      int INDYMETH    = 8; // INVOKEDYNAMIC instruction
      int LABEL       = 9; // instructions with a 2 bytes bytecode offset label
      int LABELW     = 10; // instructions with a 4 bytes bytecode offset label
      int LDC_INSN   = 11; // the LDC instruction
      int LDCW_INSN  = 12; // the LDC_W and LDC2_W instructions
      int IINC_INSN  = 13; // the IINC instruction
      int TABL_INSN  = 14; // the TABLESWITCH instruction
      int LOOK_INSN  = 15; // the LOOKUPSWITCH instruction
      int MANA_INSN  = 16; // the MULTIANEWARRAY instruction
      int WIDE_INSN  = 17; // the WIDE instruction
   }

   /**
    * The {@linkplain InstructionType instruction types} of all JVM opcodes, one value for each instruction opcode.
    */
   public static final byte[] TYPE;
   static {
      String s =
         "AAAAAAAAAAAAAAAABCLMMDDDDDEEEEEEEEEEEEEEEEEEEEAAAAAAAADD" +
         "DDDEEEEEEEEEEEEEEEEEEEEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
         "AAAAAAAAAAAAAAAAANAAAAAAAAAAAAAAAAAAAAJJJJJJJJJJJJJJJJDOPAA" +
         "AAAAGGGGGGGHIFBFAAFFAARQJJKKJJJJJJJJJJJJJJJJJJ";
      int n = s.length();
      byte[] types = new byte[n];

      for (int i = 0; i < n; i++) {
         //noinspection NumericCastThatLosesPrecision,CharUsedInArithmeticContext
         types[i] = (byte) (s.charAt(i) - 'A');
      }

      TYPE = types;
   }

   private JVMInstruction() {}
}
