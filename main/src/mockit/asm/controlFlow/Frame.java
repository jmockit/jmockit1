package mockit.asm.controlFlow;

import javax.annotation.*;

import mockit.asm.constantPool.*;
import mockit.asm.jvmConstants.*;
import mockit.asm.types.*;
import static mockit.asm.controlFlow.FrameTypeMask.*;
import static mockit.asm.jvmConstants.Opcodes.*;

/**
 * Information about the input and output stack map frames of a basic block.
 * <p/>
 * Frames are computed in a two steps process: during the visit of each instruction, the state of the frame at the end of current basic
 * block is updated by simulating the action of the instruction on the previous state of this so called "output frame".
 * In visitMaxStack, a fix point algorithm is used to compute the "input frame" of each basic block, i.e. the stack map frame at the
 * beginning of the basic block, starting from the input frame of the first basic block (which is computed from the method descriptor), and
 * by using the previously computed output frames to compute the input state of the other blocks.
 * <p/>
 * All output and input frames are stored as arrays of integers. Reference and array types are represented by an index into a type table
 * (which is not the same as the constant pool of the class, in order to avoid adding unnecessary constants in the pool - not all computed
 * frames will end up being stored in the stack map table). This allows very fast type comparisons.
 * <p/>
 * Output stack map frames are computed relatively to the input frame of the basic block, which is not yet known when output frames are
 * computed. It is therefore necessary to be able to represent abstract types such as "the type at position x in the input frame locals" or
 * "the type at position x from the top of the input frame stack" or even "the type at position x in the input frame, with y more (or less)
 * array dimensions". This explains the rather complicated type format used in output frames.
 * <p/>
 * This format is the following: DIM KIND VALUE (4, 4 and 24 bits). DIM is a signed number of array dimensions (from -8 to 7).
 * KIND is either BASE, LOCAL or STACK. BASE is used for types that are not relative to the input frame. LOCAL is used for types that are
 * relative to the input local variable types. STACK is used for types that are relative to the input stack types. VALUE depends on KIND.
 * For LOCAL types, it is an index in the input local variable types. For STACK types, it is a position relatively to the top of input frame
 * stack. For BASE types, it is either one of the constants defined below, or for OBJECT and UNINITIALIZED types, a tag and an index in the
 * type table.
 * <p/>
 * Output frames can contain types of any kind and with a positive or negative dimension (and even unassigned types, represented by 0 -
 * which does not correspond to any valid type value). Input frames can only contain BASE types of positive or null dimension.
 * In all cases the type table contains only internal type names (array type descriptors are forbidden - dimensions must be represented
 * through the DIM field).
 * <p/>
 * The LONG and DOUBLE types are always represented by using two slots (LONG + TOP or DOUBLE + TOP), for local variable types as well as in
 * the operand stack. This is necessary to be able to simulate DUPx_y instructions, whose effect would be dependent on the actual type
 * values if types were always represented by a single slot in the stack (and this is not possible, since actual type values are not always
 * known - cf LOCAL and STACK type kinds).
 */
public final class Frame
{
   private static final int[] EMPTY_INPUT_STACK = new int[0];

   @Nonnull private final ConstantPoolGeneration cp;

   /**
    * The label (i.e. basic block) to which these input and output stack map frames correspond.
    */
   @Nonnull final Label owner;

   /**
    * The input stack map frame locals.
    */
   int[] inputLocals;

   /**
    * The input stack map frame stack.
    */
   int[] inputStack;

   /**
    * The output stack map frame locals.
    */
   @Nullable private int[] outputLocals;

   /**
    * The output stack map frame stack.
    */
   private int[] outputStack;

   /**
    * Relative size of the output stack. The exact semantics of this field depends on the algorithm that is used.
    * <p/>
    * When only the maximum stack size is computed, this field is the size of the output stack relatively to the top of
    * the input stack.
    * <p/>
    * When the stack map frames are completely computed, this field is the actual number of types in
    * {@link #outputStack}.
    */
   @Nonnegative private int outputStackTop;

   /**
    * Number of types that are initialized in the basic block.
    *
    * @see #initializations
    */
   private int initializationCount;

   /**
    * The types that are initialized in the basic block. A constructor invocation on an UNINITIALIZED or UNINITIALIZED_THIS type must
    * replace <em>every occurrence</em> of this type in the local variables and in the operand stack. This cannot be done during the first
    * phase of the algorithm since, during this phase, the local variables and the operand stack are not completely computed.
    * It is therefore necessary to store the types on which constructors are invoked in the basic block, in order to do this replacement
    * during the second phase of the algorithm, where the frames are fully computed.
    * Note that this array can contain types that are relative to input locals or to the input stack.
    */
   private int[] initializations;

   Frame(@Nonnull ConstantPoolGeneration cp, @Nonnull Label owner) {
      this.cp = cp;
      this.owner = owner;
   }

   /**
    * Initializes the input frame of the first basic block from the method descriptor.
    *
    * @param access    the access flags of the method to which this label belongs.
    * @param args      the formal parameter types of this method.
    * @param maxLocals the maximum number of local variables of this method.
    */
   void initInputFrame(@Nonnull String classDesc, int access, @Nonnull JavaType[] args, @Nonnegative int maxLocals) {
      inputLocals = new int[maxLocals];
      inputStack = EMPTY_INPUT_STACK;

      int localIndex = initializeThisParameterIfApplicable(classDesc, access);
      localIndex = initializeFormalParameterTypes(localIndex, args);

      while (localIndex < maxLocals) {
         inputLocals[localIndex++] = TOP;
      }
   }

   @Nonnegative
   private int initializeThisParameterIfApplicable(@Nonnull String classDesc, int access) {
      if ((access & Access.STATIC) == 0) {
         inputLocals[0] = Access.isConstructor(access) ? UNINITIALIZED_THIS : OBJECT | cp.addNormalType(classDesc);
         return 1;
      }

      return 0;
   }

   @Nonnegative
   private int initializeFormalParameterTypes(@Nonnegative int localIndex, @Nonnull JavaType[] args) {
      for (JavaType arg : args) {
         int typeEncoding = getTypeEncoding(arg.getDescriptor());
         inputLocals[localIndex++] = typeEncoding;

         if (typeEncoding == LONG || typeEncoding == DOUBLE) {
            inputLocals[localIndex++] = TOP;
         }
      }

      return localIndex;
   }

   /**
    * Returns the {@linkplain FrameTypeMask int encoding} of the given type.
    *
    * @param typeDesc a type descriptor
    * @return the int encoding of the given type
    */
   @Nonnegative
   private int getTypeEncoding(@Nonnull String typeDesc) {
      int index = typeDesc.charAt(0) == '(' ? typeDesc.indexOf(')') + 1 : 0;

      switch (typeDesc.charAt(index)) {
         case 'V': return 0;
         case 'Z': case 'C': case 'B': case 'S': case 'I': return INTEGER;
         case 'F': return FLOAT;
         case 'J': return LONG;
         case 'D': return DOUBLE;
         case 'L': return getObjectTypeEncoding(typeDesc, index);
         case '[': return getArrayTypeEncoding(typeDesc, index);
         default: throw new IllegalArgumentException("Invalid type descriptor: " + typeDesc);
      }
   }

   @Nonnegative
   private int getObjectTypeEncoding(@Nonnull String typeDesc, @Nonnegative int index) {
      // Stores the internal name, not the descriptor!
      String t = typeDesc.substring(index + 1, typeDesc.length() - 1);
      return OBJECT | cp.addNormalType(t);
   }

   @Nonnegative
   private int getArrayTypeEncoding(@Nonnull String typeDesc, @Nonnegative int index) {
      int dims = getNumberOfDimensions(typeDesc, index);
      int data = getArrayElementTypeEncoding(typeDesc, index + dims);
      return dims << 28 | data;
   }

   @Nonnegative
   private static int getNumberOfDimensions(@Nonnull String typeDesc, @Nonnegative int index) {
      int dims = 1;

      while (typeDesc.charAt(index + dims) == '[') {
         dims++;
      }

      return dims;
   }

   @Nonnegative @SuppressWarnings("OverlyComplexMethod")
   private int getArrayElementTypeEncoding(@Nonnull String typeDesc, @Nonnegative int index) {
      switch (typeDesc.charAt(index)) {
         case 'Z': return BOOLEAN;
         case 'C': return CHAR;
         case 'B': return BYTE;
         case 'S': return SHORT;
         case 'I': return INTEGER;
         case 'F': return FLOAT;
         case 'J': return LONG;
         case 'D': return DOUBLE;
         case 'L': return getObjectTypeEncoding(typeDesc, index);
         default: throw new IllegalArgumentException("Invalid type descriptor: " + typeDesc);
      }
   }

   /**
    * Simulates the action of a IINC instruction on the output stack frame.
    */
   void executeIINC(@Nonnegative int varIndex) {
      set(varIndex, INTEGER);
   }

   /**
    * Sets the output frame local variable type at the given index.
    *
    * @param local the index of the local that must be set
    * @param type  the value of the local that must be set
    */
   private void set(@Nonnegative int local, int type) {
      // Creates and/or resizes the output local variables array if necessary.
      if (outputLocals == null) {
         outputLocals = new int[10];
      }

      int n = outputLocals.length;

      if (local >= n) {
         int[] t = new int[Math.max(local + 1, 2 * n)];
         System.arraycopy(outputLocals, 0, t, 0, n);
         outputLocals = t;
      }

      // Sets the local variable.
      outputLocals[local] = type;
   }

   /**
    * Simulates the action of a BIPUSH, SIPUSH, or NEWARRAY instruction on the output stack frame.
    *
    * @param opcode  the opcode of the instruction
    * @param operand the operand of the instruction, if any
    */
   void executeINT(@Nonnegative int opcode, int operand) {
      if (opcode == NEWARRAY) {
         executeNewArray(operand);
      }
      else {
         push(INTEGER);
      }
   }

   private void executeNewArray(@Nonnegative int arrayElementType) {
      pop();

      //noinspection SwitchStatementWithoutDefaultBranch
      switch (arrayElementType) {
         case ArrayElementType.BOOLEAN: push(ARRAY_OF | BOOLEAN); break;
         case ArrayElementType.CHAR:    push(ARRAY_OF | CHAR);    break;
         case ArrayElementType.BYTE:    push(ARRAY_OF | BYTE);    break;
         case ArrayElementType.SHORT:   push(ARRAY_OF | SHORT);   break;
         case ArrayElementType.INT:     push(ARRAY_OF | INTEGER); break;
         case ArrayElementType.FLOAT:   push(ARRAY_OF | FLOAT);   break;
         case ArrayElementType.DOUBLE:  push(ARRAY_OF | DOUBLE);  break;
         case ArrayElementType.LONG:    push(ARRAY_OF | LONG);
      }
   }

   /**
    * Pushes a new type onto the output frame stack.
    */
   private void push(int type) {
      // Creates and/or resizes the output stack array if necessary.
      if (outputStack == null) {
         outputStack = new int[10];
      }

      int n = outputStack.length;

      if (outputStackTop >= n) {
         int[] t = new int[Math.max(outputStackTop + 1, 2 * n)];
         System.arraycopy(outputStack, 0, t, 0, n);
         outputStack = t;
      }

      // Pushes the type on the output stack.
      outputStack[outputStackTop++] = type;

      owner.updateOutputStackMaxHeight(outputStackTop);
   }

   /**
    * Pops a type from the output frame stack and returns its value.
    */
   private int pop() {
      if (outputStackTop > 0) {
         return outputStack[--outputStackTop];
      }

      // If the output frame stack is empty, pops from the input stack.
      return STACK | -owner.decrementInputStackTop();
   }

   /**
    * Simulates the action of a LOOKUPSWITCH or TABLESWITCH instruction on the output stack frame.
    */
   void executeSWITCH() {
      pop(1);
   }

   /**
    * Pops the given number of types from the output frame stack.
    *
    * @param elements the number of types that must be popped.
    */
   private void pop(@Nonnegative int elements) {
      if (outputStackTop >= elements) {
         outputStackTop -= elements;
      }
      else {
         // If the number of elements to be popped is greater than the number of elements in the output stack, clear it,
         // and pops the remaining elements from the input stack.
         owner.decrementInputStackTop(elements - outputStackTop);
         outputStackTop = 0;
      }
   }

   /**
    * Pops a type from the output frame stack.
    *
    * @param desc the descriptor of the type to be popped. Can also be a method
    *             descriptor (in this case this method pops the types corresponding to the method arguments).
    */
   private void pop(@Nonnull String desc) {
      char c = desc.charAt(0);

      if (c == '(') {
         int elements = (JavaType.getArgumentsAndReturnSizes(desc) >> 2) - 1;
         pop(elements);
      }
      else if (c == 'J' || c == 'D') {
         pop(2);
      }
      else {
         pop(1);
      }
   }

   /**
    * Adds a new type to the list of types on which a constructor is invoked in the basic block.
    *
    * @param var a type on a which a constructor is invoked
    */
   private void init(int var) {
      // Creates and/or resizes the initializations array if necessary.
      if (initializations == null) {
         initializations = new int[2];
      }

      int n = initializations.length;

      if (initializationCount >= n) {
         int[] t = new int[Math.max(initializationCount + 1, 2 * n)];
         System.arraycopy(initializations, 0, t, 0, n);
         initializations = t;
      }

      // Stores the type to be initialized.
      initializations[initializationCount++] = var;
   }

   /**
    * Replaces the given type with the appropriate type if it is one of the types on which a constructor is invoked in the basic block.
    *
    * @return the given type or, if <tt>type</tt> is one of the types on which a constructor is invoked in the basic block, the type
    * corresponding to this constructor
    */
   @Nonnegative
   private int init(@Nonnull String classDesc, @Nonnegative int type) {
      int s;

      if (type == UNINITIALIZED_THIS) {
         s = OBJECT | cp.addNormalType(classDesc);
      }
      else if ((type & (DIM | BASE_KIND)) == UNINITIALIZED) {
         String typeDesc = cp.getInternalName(type & BASE_VALUE);
         s = OBJECT | cp.addNormalType(typeDesc);
      }
      else {
         return type;
      }

      for (int j = 0; j < initializationCount; j++) {
         int u = initializations[j];
         int dim = u & DIM;
         int kind = u & KIND;

         if (kind == LOCAL) {
            u = dim + inputLocals[u & VALUE];
         }
         else if (kind == STACK) {
            u = dim + inputStack[inputStack.length - (u & VALUE)];
         }

         if (type == u) {
            return s;
         }
      }

      return type;
   }

   /**
    * Simulates the action of an xLOAD or xSTORE instruction on the output stack frame.
    *
    * @param opcode the opcode of the instruction
    * @param varIndex the local variable index
    */
   void executeVAR(int opcode, @Nonnegative int varIndex) {
      //noinspection SwitchStatementWithoutDefaultBranch
      switch (opcode) {
         case ILOAD: push(INTEGER);           break;
         case LLOAD: push(LONG); push(TOP);   break;
         case FLOAD: push(FLOAT);             break;
         case DLOAD: push(DOUBLE); push(TOP); break;
         case ALOAD: push(get(varIndex));     break;
         case ISTORE: case FSTORE: case ASTORE: executeSingleWordStore(varIndex); break;
         case LSTORE: case DSTORE:              executeDoubleWordStore(varIndex);
      }
   }

   /**
    * Returns the output frame local variable type at the given index.
    */
   @Nonnegative
   private int get(@Nonnegative int localIndex) {
      if (outputLocals == null || localIndex >= outputLocals.length) {
         // This local has never been assigned in this basic block, so it's still equal to its value in the input frame.
         return LOCAL | localIndex;
      }

      int type = outputLocals[localIndex];

      if (type == 0) {
         // This local has never been assigned in this basic block, so it's still equal to its value in the input frame.
         type = outputLocals[localIndex] = LOCAL | localIndex;
      }

      return type;
   }

   private void executeSingleWordStore(@Nonnegative int arg) {
      int type1 = pop();
      set(arg, type1);
      executeStore(arg);
   }

   private void executeDoubleWordStore(@Nonnegative int arg) {
      pop(1);

      int type1 = pop();
      set(arg, type1);
      set(arg + 1, TOP);

      executeStore(arg);
   }

   private void executeStore(@Nonnegative int arg) {
      if (arg > 0) {
         int type2 = get(arg - 1);

         // If type2 is of kind STACK or LOCAL we cannot know its size!
         if (type2 == LONG || type2 == DOUBLE) {
            set(arg - 1, TOP);
         }
         else if ((type2 & KIND) != BASE) {
            set(arg - 1, type2 | TOP_IF_LONG_OR_DOUBLE);
         }
      }
   }

   /**
    * Simulates the action of a conditional/unconditional jump instruction on the output stack frame.
    */
   void executeJUMP(@Nonnegative int opcode) {
      //noinspection SwitchStatementWithoutDefaultBranch
      switch (opcode) {
         case IFEQ: case IFNE: case IFLT: case IFGE: case IFGT: case IFLE: case IFNULL: case IFNONNULL:
            pop(1);
            break;
         case IF_ICMPEQ: case IF_ICMPNE: case IF_ICMPLT: case IF_ICMPGE:
         case IF_ICMPGT: case IF_ICMPLE: case IF_ACMPEQ: case IF_ACMPNE:
            pop(2);
      }
   }

   /**
    * Simulates the action of the given zero-operand instruction on the output stack frame.
    */
   @SuppressWarnings({"OverlyComplexMethod", "OverlyLongMethod"})
   public void execute(@Nonnegative int opcode) {
      //noinspection SwitchStatementWithoutDefaultBranch
      switch (opcode) {
         case NOP: case INEG: case LNEG: case FNEG: case DNEG: case I2B: case I2C: case I2S: case GOTO: case RETURN:
            break;
         case ACONST_NULL:
            push(NULL);
            break;
         case ICONST_M1: case ICONST_0: case ICONST_1: case ICONST_2: case ICONST_3: case ICONST_4: case ICONST_5:
            push(INTEGER);
            break;
         case LCONST_0: case LCONST_1:
            push(LONG);
            push(TOP);
            break;
         case FCONST_0: case FCONST_1: case FCONST_2:
            push(FLOAT);
            break;
         case DCONST_0: case DCONST_1:
            push(DOUBLE);
            push(TOP);
            break;
         case IALOAD: case BALOAD: case CALOAD: case SALOAD:
            pop(2);
            push(INTEGER);
            break;
         case LALOAD: case D2L:
            pop(2);
            push(LONG);
            push(TOP);
            break;
         case FALOAD:
            pop(2);
            push(FLOAT);
            break;
         case DALOAD: case L2D:
            pop(2);
            push(DOUBLE);
            push(TOP);
            break;
         case AALOAD:
            executeAALOAD();
            break;
         case IASTORE: case BASTORE: case CASTORE: case SASTORE: case FASTORE: case AASTORE:
            pop(3);
            break;
         case LASTORE: case DASTORE:
            pop(4);
            break;
         case POP: case IRETURN: case FRETURN: case ARETURN: case ATHROW: case MONITORENTER: case MONITOREXIT:
            pop(1);
            break;
         case POP2: case LRETURN: case DRETURN:
            pop(2);
            break;
         case DUP:
            executeDUP();
            break;
         case DUP_X1:
            executeDUP_X1();
            break;
         case DUP_X2:
            executeDUP_X2();
            break;
         case DUP2:
            executeDUP2();
            break;
         case DUP2_X1:
            executeDUP2_X1();
            break;
         case DUP2_X2:
            executeDUP2_X2();
            break;
         case SWAP:
            executeSWAP();
            break;
         case IADD:  case ISUB: case IMUL: case IDIV:  case IREM:
         case IAND:  case IOR:  case IXOR: case ISHL:  case ISHR:
         case IUSHR: case L2I:  case D2I:  case FCMPL: case FCMPG:
            pop(2);
            push(INTEGER);
            break;
         case LADD: case LSUB: case LMUL: case LDIV: case LREM: case LAND: case LOR: case LXOR:
            pop(4);
            push(LONG);
            push(TOP);
            break;
         case FADD: case FSUB: case FMUL: case FDIV: case FREM: case L2F: case D2F:
            pop(2);
            push(FLOAT);
            break;
         case DADD: case DSUB: case DMUL: case DDIV: case DREM:
            pop(4);
            push(DOUBLE);
            push(TOP);
            break;
         case LSHL: case LSHR: case LUSHR:
            pop(3);
            push(LONG);
            push(TOP);
            break;
         case I2L: case F2L:
            pop(1);
            push(LONG);
            push(TOP);
            break;
         case I2F:
            pop(1);
            push(FLOAT);
            break;
         case I2D: case F2D:
            pop(1);
            push(DOUBLE);
            push(TOP);
            break;
         case F2I: case ARRAYLENGTH:
            pop(1);
            push(INTEGER);
            break;
         case LCMP: case DCMPL: case DCMPG:
            pop(4);
            push(INTEGER);
      }
   }

   private void executeAALOAD() {
      pop(1);
      int type = pop();
      push(ELEMENT_OF + type);
   }

   private void executeDUP() {
      int type = pop();
      push(type);
      push(type);
   }

   private void executeDUP_X1() {
      int type1 = pop();
      int type2 = pop();
      push(type1);
      push(type2);
      push(type1);
   }

   private void executeDUP_X2() {
      int t1 = pop();
      int t2 = pop();
      int t3 = pop();
      push(t1);
      push(t3);
      push(t2);
      push(t1);
   }

   private void executeDUP2() {
      int t1 = pop();
      int t2 = pop();
      push(t2);
      push(t1);
      push(t2);
      push(t1);
   }

   private void executeDUP2_X1() {
      int t1 = pop();
      int t2 = pop();
      int t3 = pop();
      push(t2);
      push(t1);
      push(t3);
      push(t2);
      push(t1);
   }

   private void executeDUP2_X2() {
      int t1 = pop();
      int t2 = pop();
      int t3 = pop();
      int t4 = pop();
      push(t2);
      push(t1);
      push(t4);
      push(t3);
      push(t2);
      push(t1);
   }

   private void executeSWAP() {
      int t1 = pop();
      int t2 = pop();
      push(t1);
      push(t2);
   }

   /**
    * Simulates the action of an LCD instruction on the output stack frame.
    *
    * @param item the operand of the instructions
    */
   void executeLDC(@Nonnull Item item) {
      switch (item.getType()) {
         case ConstantPoolTypes.INTEGER:
            push(INTEGER);
            break;
         case ConstantPoolTypes.LONG:
            push(LONG);
            push(TOP);
            break;
         case ConstantPoolTypes.FLOAT:
            push(FLOAT);
            break;
         case ConstantPoolTypes.DOUBLE:
            push(DOUBLE);
            push(TOP);
            break;
         case ConstantPoolTypes.CLASS:
            push(OBJECT | cp.addNormalType("java/lang/Class"));
            break;
         case ConstantPoolTypes.STRING:
            push(OBJECT | cp.addNormalType("java/lang/String"));
            break;
         case ConstantPoolTypes.METHOD_TYPE:
            push(OBJECT | cp.addNormalType("java/lang/invoke/MethodType"));
            break;
         case ConstantPoolTypes.METHOD_HANDLE:
            push(OBJECT | cp.addNormalType("java/lang/invoke/MethodHandle"));
            break;
         case ConstantPoolTypes.DYNAMIC:
            DynamicItem dynamicItem = (DynamicItem) item;
            String desc = dynamicItem.getDesc();
            if (desc.length() == 1) {
               // primitive
               char descChar = desc.charAt(0);
               switch(descChar) {
                  case 'Z':
                  case 'B':
                  case 'S':
                  case 'I':
                     push(INTEGER);
                     break;
                  case 'F':
                     push(FLOAT);
                     break;
                  case 'D':
                     push(DOUBLE);
                     break;
                  case 'J':
                     push(LONG);
                     break;
                  default:
                     throw new IllegalArgumentException("Unsupported dynamic local 'primitive' type: " + desc);
               }
            } else if (desc.charAt(0) == '['){
               // array
               ArrayType arrayType = ArrayType.create(desc);
               JavaType elementType = arrayType.getElementType();
               int mask;
               if (elementType instanceof PrimitiveType) {
                  PrimitiveType primitiveElementType = (PrimitiveType) elementType;
                  char descChar = primitiveElementType.getTypeCode();
                  switch(descChar) {
                     case 'Z':
                     case 'B':
                     case 'C':
                     case 'S':
                     case 'I':
                        mask = INTEGER;
                        break;
                     case 'F':
                        mask = FLOAT;
                        break;
                     case 'D':
                        mask = DOUBLE;
                        break;
                     case 'J':
                        mask = LONG;
                        break;
                     default:
                        throw new IllegalArgumentException("Unsupported array element 'primitive' type: " + desc);
                  }
               } else {
                  mask = OBJECT;
               }
               push(ARRAY_OF | mask);
            } else {
               // object, substring 'L' and ';'
               push(OBJECT | cp.addNormalType(desc.substring(1, desc.length() - 1)));
            }
            break;
         default:
            throw new IllegalArgumentException("Unknown item type, cannot execute frame: " + item.getType());
      }
   }

   /**
    * Simulates the action of a NEW, ANEWARRAY, CHECKCAST or INSTANCEOF instruction on the output stack frame.
    *
    * @param opcode the opcode of the instruction
    * @param codeLength the operand of the instruction, if any
    * @param item the operand of the instruction
    */
   void executeTYPE(@Nonnegative int opcode, @Nonnegative int codeLength, @Nonnull StringItem item) {
      //noinspection SwitchStatementWithoutDefaultBranch
      switch (opcode) {
         case NEW:
            push(UNINITIALIZED | cp.addUninitializedType(item.getValue(), codeLength));
            break;
         case ANEWARRAY:
            executeANewArray(item);
            break;
         case CHECKCAST:
            executeCheckCast(item);
            break;
         case INSTANCEOF:
            pop(1);
            push(INTEGER);
      }
   }

   private void executeANewArray(@Nonnull StringItem item) {
      String s = item.getValue();
      pop();

      if (s.charAt(0) == '[') {
         push('[' + s);
      }
      else {
         push(ARRAY_OF | OBJECT | cp.addNormalType(s));
      }
   }

   /**
    * Pushes a new type onto the output frame stack.
    *
    * @param desc the descriptor of the type to be pushed; can also be a method descriptor (in this case this method pushes its return type
    *             onto the output frame stack)
    */
   private void push(@Nonnull String desc) {
      int type = getTypeEncoding(desc);

      if (type != 0) {
         push(type);

         if (type == LONG || type == DOUBLE) {
            push(TOP);
         }
      }
   }

   private void executeCheckCast(@Nonnull StringItem item) {
      String s = item.getValue();
      pop();

      if (s.charAt(0) == '[') {
         push(s);
      }
      else {
         push(OBJECT | cp.addNormalType(s));
      }
   }

   /**
    * Simulates the action of a MULTIANEWARRAY instruction on the output stack frame.
    *
    * @param dims the number of dimensions of the array
    * @param arrayType the type of the array elements
    */
   void executeMULTIANEWARRAY(@Nonnegative int dims, @Nonnull StringItem arrayType) {
      pop(dims);
      push(arrayType.getValue());
   }

   /**
    * Simulates the action of the given instruction on the output stack frame.
    *
    * @param opcode the opcode of the instruction
    * @param item   the operand of the instruction
    */
   public void execute(@Nonnegative int opcode, @Nonnull TypeOrMemberItem item) {
      if (opcode == INVOKEDYNAMIC) {
         executeInvokeDynamic(item);
      }
      else {
         //noinspection SwitchStatementWithoutDefaultBranch
         switch (opcode) {
            case GETSTATIC:
               push(item.getDesc());
               break;
            case PUTSTATIC:
               pop(item.getDesc());
               break;
            case GETFIELD:
               pop(1);
               push(item.getDesc());
               break;
            case PUTFIELD:
               pop(item.getDesc());
               pop();
               break;
            case INVOKEVIRTUAL:
            case INVOKESPECIAL:
            case INVOKESTATIC:
            case INVOKEINTERFACE:
               executeInvoke(opcode, item);
         }
      }
   }

   private void executeInvoke(@Nonnegative int opcode, @Nonnull TypeOrMemberItem item) {
      String methodDesc = item.getDesc();
      pop(methodDesc);

      if (opcode != INVOKESTATIC) {
         int var = pop();

         if (opcode == INVOKESPECIAL && item.getName().charAt(0) == '<') {
            init(var);
         }
      }

      push(methodDesc);
   }

   private void executeInvokeDynamic(@Nonnull TypeOrMemberItem item) {
      String desc = item.getDesc();
      pop(desc);
      push(desc);
   }

   /**
    * Merges the input frame of the given basic block with the input and output frames of this basic block.
    *
    * @param frame the basic block whose input frame must be updated
    * @param edge  the kind of the {@link Edge} between this label and 'label'; see {@link Edge#info}
    *
    * @return <tt>true</tt> if the input frame of the given label has been changed by this operation
    */
   boolean merge(@Nonnull String classDesc, @Nonnull Frame frame, @Nonnegative int edge) {
      int nLocal = inputLocals.length;
      int nStack = inputStack.length;
      boolean changed = false;

      if (frame.inputLocals == null) {
         frame.inputLocals = new int[nLocal];
         changed = true;
      }

      int i;
      int s;
      int dim;
      int type;

      for (i = 0; i < nLocal; i++) {
         if (outputLocals != null && i < outputLocals.length) {
            s = outputLocals[i];

            if (s == 0) {
               type = inputLocals[i];
            }
            else {
               dim = s & DIM;
               int kind = s & KIND;

               if (kind == BASE) {
                  type = s;
               }
               else {
                  if (kind == LOCAL) {
                     type = dim + inputLocals[s & VALUE];
                  }
                  else {
                     type = dim + inputStack[nStack - (s & VALUE)];
                  }

                  if ((s & TOP_IF_LONG_OR_DOUBLE) != 0 && (type == LONG || type == DOUBLE)) {
                     type = TOP;
                  }
               }
            }
         }
         else {
            type = inputLocals[i];
         }

         if (initializations != null) {
            type = init(classDesc, type);
         }

         changed |= merge(type, frame.inputLocals, i);
      }

      if (edge > 0) {
         for (i = 0; i < nLocal; ++i) {
            type = inputLocals[i];
            changed |= merge(type, frame.inputLocals, i);
         }

         if (frame.inputStack == null) {
            frame.inputStack = new int[1];
            changed = true;
         }

         changed |= merge(edge, frame.inputStack, 0);
         return changed;
      }

      int nInputStack = inputStack.length + owner.inputStackTop;

      if (frame.inputStack == null) {
         frame.inputStack = new int[nInputStack + outputStackTop];
         changed = true;
      }

      for (i = 0; i < nInputStack; i++) {
         type = inputStack[i];

         if (initializations != null) {
            type = init(classDesc, type);
         }

         changed |= merge(type, frame.inputStack, i);
      }

      for (i = 0; i < outputStackTop; i++) {
         s = outputStack[i];
         dim = s & DIM;
         int kind = s & KIND;

         if (kind == BASE) {
            type = s;
         }
         else {
            if (kind == LOCAL) {
               type = dim + inputLocals[s & VALUE];
            }
            else {
               type = dim + inputStack[nStack - (s & VALUE)];
            }

            if ((s & TOP_IF_LONG_OR_DOUBLE) != 0 && (type == LONG || type == DOUBLE)) {
               type = TOP;
            }
         }

         if (initializations != null) {
            type = init(classDesc, type);
         }

         changed |= merge(type, frame.inputStack, nInputStack + i);
      }

      return changed;
   }

   /**
    * Merges the type at the given index in the given type array with the given type.
    *
    * @param type1 the type with which the type array element must be merged
    * @param types an array of types
    * @param index the index of the type that must be merged in 'types'
    * @return <tt>true</tt> if the type array has been modified by this operation
    */
   private boolean merge(@Nonnegative int type1, @Nonnull int[] types, @Nonnegative int index) {
      int type2 = types[index];

      if (type2 == type1) {
         // If the types are equal, there is no change.
         return false;
      }

      if ((type1 & ~DIM) == NULL) {
         if (type2 == NULL) {
            return false;
         }

         //noinspection AssignmentToMethodParameter
         type1 = NULL;
      }

      if (type2 == 0) {
         // If types[index] has never been assigned, merge(type2, type1) = type1.
         types[index] = type1;
         return true;
      }

      int v;

      if ((type2 & BASE_KIND) == OBJECT || (type2 & DIM) != 0) {
         // If type2 is a reference type of any dimension.
         if (type1 == NULL) {
            // If type1 is the NULL type, merge(type2, type1) = type2, so there is no change.
            return false;
         }
         else if ((type1 & (DIM | BASE_KIND)) == (type2 & (DIM | BASE_KIND))) {
            // If type1 and type2 have the same dimension and same base kind.
            if ((type2 & BASE_KIND) == OBJECT) {
               // If type1 is also a reference type, and if type2 and type1 have the same dimension
               // merge(type2, type1) = dim(type1) | common parent of the element types of type2 and type1.
               v = (type1 & DIM) | OBJECT | cp.getMergedType(type1 & BASE_VALUE, type2 & BASE_VALUE);
            }
            else {
               // If type2 and type1 are array types, but not with the same element type,
               // merge(type2, type1) = dim(type2) - 1 | java/lang/Object.
               int dim = ELEMENT_OF + (type2 & DIM);
               v = dim | OBJECT | cp.addNormalType("java/lang/Object");
            }
         }
         else if ((type1 & BASE_KIND) == OBJECT || (type1 & DIM) != 0) {
            // If type1 is any other reference or array type, the merged type is min(uDim, tDim) | java/lang/Object,
            // where uDim is the array dimension of type2, minus 1 if type2 is an array type with a primitive element
            // type (and similarly for tDim).
            int tDim = (((type1 & DIM) == 0 || (type1 & BASE_KIND) == OBJECT) ? 0 : ELEMENT_OF) + (type1 & DIM);
            int uDim = (((type2 & DIM) == 0 || (type2 & BASE_KIND) == OBJECT) ? 0 : ELEMENT_OF) + (type2 & DIM);
            v = Math.min(tDim, uDim) | OBJECT | cp.addNormalType("java/lang/Object");
         }
         else {
            // If type1 is any other type, merge(type2, type1) = TOP.
            v = TOP;
         }
      }
      else if (type2 == NULL) {
         // If type2 is the NULL type, merge(type2, type1) = type1, or TOP if type1 is not a reference type.
         v = (type1 & BASE_KIND) == OBJECT || (type1 & DIM) != 0 ? type1 : TOP;
      }
      else {
         // If type2 is any other type, merge(type2, type1) = TOP whatever type1.
         v = TOP;
      }

      if (type2 != v) {
         types[index] = v;
         return true;
      }

      return false;
   }
}
