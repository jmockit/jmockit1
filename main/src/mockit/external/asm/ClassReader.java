/*
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package mockit.external.asm;

import java.io.*;

import static mockit.external.asm.ClassWriter.*;

/**
 * A Java class parser to make a {@link ClassVisitor} visit an existing class.
 * This class parses a byte array conforming to the Java class file format and calls the appropriate visit methods of a
 * given class visitor for each field, method and bytecode instruction encountered.
 *
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
public final class ClassReader
{
    /**
     * Flag to skip method code. If this class is set <code>CODE</code> attribute won't be visited.
     * This can be used, for example, to retrieve annotations for methods and method parameters.
     */
    public static final int SKIP_CODE = 1;

    /**
     * Flag to skip the debug information in the class. If this flag is set the debug information of the class is not
     * visited, i.e. the {@link MethodVisitor#visitLocalVariable visitLocalVariable} and
     * {@link MethodVisitor#visitLineNumber visitLineNumber} methods will not be called.
     */
    public static final int SKIP_DEBUG = 2;

    /**
     * The class to be parsed. <em>The content of this array must not be modified.</em>
     */
    public final byte[] b;

    /**
     * The start index of each constant pool item in {@link #b b}, plus one.
     * The one byte offset skips the constant pool item tag that indicates its type.
     */
    private final int[] items;

    /**
     * The String objects corresponding to the CONSTANT_Utf8 items. This cache avoids multiple parsing of a given
     * CONSTANT_Utf8 constant pool item, which GREATLY improves performances (by a factor 2 to 3). This caching
     * strategy could be extended to all constant pool items, but its benefit would not be so great for these items
     * (because they are much less expensive to parse than CONSTANT_Utf8 items).
     */
    private final String[] strings;

    /**
     * Maximum length of the strings contained in the constant pool of the class.
     */
    private final int maxStringLength;

    /**
     * Start index of the class header information (access, name...) in {@link #b b}.
     */
    private final int header;

    // Helper fields.
    private int access;
    private String name;
    private String superClass;
    private String[] interfaces;
    private String[] exceptions;
    private Attribute attributes;
    private String signature;
    private String sourceFile;
    private String sourceDebug;
    private String enclosingOwner;
    private String enclosingName;
    private String enclosingDesc;

    /**
     * Constructs a new {@link ClassReader} object.
     *
     * @param b the bytecode of the class to be read.
     */
    public ClassReader(byte[] b) {
        this.b = b;

        // parses the constant pool
        items = new int[readUnsignedShort(8)];
        int n = items.length;
        strings = new String[n];
        int max = 0;
        int index = 10;

        for (int i = 1; i < n; ++i) {
            items[i] = index + 1;
            int size;

            switch (b[index]) {
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
                    ++i;
                    break;
                case UTF8:
                    size = 3 + readUnsignedShort(index + 1);
                    if (size > max) {
                        max = size;
                    }
                    break;
                case HANDLE:
                    size = 4;
                    break;
                // case ClassWriter.CLASS:
                // case ClassWriter.STR:
                // case ClassWriter.MTYPE
                default:
                    size = 3;
                    break;
            }

            index += size;
        }

        maxStringLength = max;
        // the class header information starts just after the constant pool
        header = index;
    }

    /**
     * Returns the class's access flags (see {@link Opcodes}). This value may not reflect Deprecated and Synthetic flags
     * when bytecode is before 1.5 and those flags are represented by attributes.
     *
     * @return the class access flags
     *
     * @see ClassVisitor#visit(int, int, String, String, String, String[])
     */
    public int getAccess() {
        return readUnsignedShort(header);
    }

    /**
     * Returns the internal name of the class (see {@link Type#getInternalName() getInternalName}).
     *
     * @return the internal class name
     *
     * @see ClassVisitor#visit(int, int, String, String, String, String[])
     */
    public String getClassName() {
        return readClass(header + 2, new char[maxStringLength]);
    }

    /**
     * Returns the internal of name of the super class (see {@link Type#getInternalName() getInternalName}).
     * For interfaces, the super class is {@link Object}.
     *
     * @return the internal name of super class, or <tt>null</tt> for {@link Object} class.
     *
     * @see ClassVisitor#visit(int, int, String, String, String, String[])
     */
    public String getSuperName() {
        return readClass(header + 4, new char[maxStringLength]);
    }

    /**
     * Returns the internal names of the class's interfaces (see {@link Type#getInternalName() getInternalName}).
     *
     * @return the array of internal names for all implemented interfaces or <tt>null</tt>.
     *
     * @see ClassVisitor#visit(int, int, String, String, String, String[])
     */
    public String[] getInterfaces() {
        int index = header + 6;
        int n = readUnsignedShort(index);
        String[] interfaces = new String[n];

        if (n > 0) {
            char[] buf = new char[maxStringLength];

            for (int i = 0; i < n; ++i) {
                index += 2;
                interfaces[i] = readClass(index, buf);
            }
        }

        return interfaces;
    }

    /**
     * Copies the constant pool data into the given {@link ClassWriter}.
     */
    void copyPool(ClassWriter classWriter) {
        char[] buf = new char[maxStringLength];
        int ll = items.length;
        Item[] items2 = new Item[ll];

        for (int i = 1; i < ll; i++) {
            int index = items[i];
            int tag = b[index - 1];
            Item item = new Item(i);
            int nameType;

            switch (tag) {
                case FIELD:
                case METH:
                case IMETH:
                    nameType = items[readUnsignedShort(index + 2)];
                    item.set(tag, readClass(index, buf), readUTF8(nameType, buf), readUTF8(nameType + 2, buf));
                    break;
                case INT:
                    item.set(readInt(index));
                    break;
                case FLOAT:
                    item.set(Float.intBitsToFloat(readInt(index)));
                    break;
                case NAME_TYPE:
                    item.set(tag, readUTF8(index, buf), readUTF8(index + 2, buf), null);
                    break;
                case LONG:
                    item.set(readLong(index));
                    ++i;
                    break;
                case DOUBLE:
                    item.set(Double.longBitsToDouble(readLong(index)));
                    ++i;
                    break;
                case UTF8: {
                    String s = strings[i];

                    if (s == null) {
                        index = items[i];
                        s = strings[i] = readUTF(index + 2, readUnsignedShort(index), buf);
                    }

                    item.set(tag, s, null, null);
                    break;
                }
                case HANDLE: {
                    int fieldOrMethodRef = items[readUnsignedShort(index + 1)];
                    nameType = items[readUnsignedShort(fieldOrMethodRef + 2)];
                    item.set(
                       HANDLE_BASE + readByte(index), readClass(fieldOrMethodRef, buf),
                       readUTF8(nameType, buf), readUTF8(nameType + 2, buf));
                    break;
                }
                case INDY:
                    if (classWriter.bootstrapMethods == null) {
                        copyBootstrapMethods(classWriter, items2, buf);
                    }

                    nameType = items[readUnsignedShort(index + 2)];
                    item.set(readUTF8(nameType, buf), readUTF8(nameType + 2, buf), readUnsignedShort(index));
                    break;
                // case ClassWriter.STR:
                // case ClassWriter.CLASS:
                // case ClassWriter.MTYPE
                default:
                    item.set(tag, readUTF8(index, buf), null, null);
                    break;
            }

            int index2 = item.hashCode % items2.length;
            item.next = items2[index2];
            items2[index2] = item;
        }

        int off = items[1] - 1;
        classWriter.pool.putByteArray(b, off, header - off);
        classWriter.items = items2;
        classWriter.threshold = (int) (0.75d * ll);
        classWriter.index = ll;
    }

    /**
     * Copies the bootstrap method data into the given {@link ClassWriter}.
     */
    private void copyBootstrapMethods(ClassWriter classWriter, Item[] items, char[] c) {
        // Finds the "BootstrapMethods" attribute.
        int u = getAttributes();
        boolean found = false;

        for (int i = readUnsignedShort(u); i > 0; --i) {
            String attrName = readUTF8(u + 2, c);

            if ("BootstrapMethods".equals(attrName)) {
                found = true;
                break;
            }

            u += 6 + readInt(u + 4);
        }

        if (!found) {
            return;
        }

        // Copies the bootstrap methods in the class writer.
        int bootstrapMethodCount = readUnsignedShort(u + 8);

        for (int j = 0, v = u + 10; j < bootstrapMethodCount; j++) {
            int position = v - u - 10;
            int hashCode = readConst(readUnsignedShort(v), c).hashCode();

            for (int k = readUnsignedShort(v + 2); k > 0; --k) {
                hashCode ^= readConst(readUnsignedShort(v + 4), c).hashCode();
                v += 2;
            }

            v += 4;
            Item item = new Item(j);
            item.set(position, hashCode & 0x7FFFFFFF);
            int index = item.hashCode % items.length;
            item.next = items[index];
            items[index] = item;
        }

        int attrSize = readInt(u + 4);
        ByteVector bootstrapMethods = new ByteVector(attrSize + 62);
        bootstrapMethods.putByteArray(b, u + 10, attrSize - 2);
        classWriter.bootstrapMethodsCount = bootstrapMethodCount;
        classWriter.bootstrapMethods = bootstrapMethods;
    }

    /**
     * Constructs a new {@link ClassReader} object.
     *
     * @param is an input stream from which to read the class.
     * @throws IOException if a problem occurs during reading.
     */
    public ClassReader(InputStream is) throws IOException {
        this(readClass(is));
    }

    /**
     * Constructs a new {@link ClassReader} object.
     *
     * @param name the binary qualified name of the class to be read.
     * @throws IOException if an exception occurs during reading.
     */
    public ClassReader(String name) throws IOException {
        this(readClass(ClassLoader.getSystemResourceAsStream(name.replace('.', '/') + ".class")));
    }

    /**
     * Reads the bytecode of a class.
     *
     * @param is an input stream from which to read the class.
     * @return the bytecode read from the given input stream.
     * @throws IOException if a problem occurs during reading.
     */
    private static byte[] readClass(InputStream is) throws IOException {
        if (is == null) {
            throw new IOException("Class not found");
        }

        try {
            byte[] b = new byte[is.available()];
            int len = 0;

            while (true) {
                int n = is.read(b, len, b.length - len);

                if (n == -1) {
                    if (len < b.length) {
                        byte[] c = new byte[len];
                        System.arraycopy(b, 0, c, 0, len);
                        b = c;
                    }

                    return b;
                }

                len += n;

                if (len == b.length) {
                    int last = is.read();

                    if (last < 0) {
                        return b;
                    }

                    byte[] c = new byte[b.length + 1000];
                    System.arraycopy(b, 0, c, 0, len);
                    c[len++] = (byte) last;
                    b = c;
                }
            }
        }
        finally {
            is.close();
        }
    }

    /**
     * Makes the given visitor visit the Java class of this {@link ClassReader}.
     * This class is the one specified in the constructor (see {@link #ClassReader(byte[]) ClassReader}).
     *
     * @param classVisitor the visitor that must visit this class.
     * @param flags option flags that can be used to modify the default behavior of this class.
     *              See {@link #SKIP_CODE}, {@link #SKIP_DEBUG}.
     */
    public void accept(ClassVisitor classVisitor, int flags) {
        char[] c = new char[maxStringLength]; // buffer used to read strings
        Context context = new Context(flags, c);

        int u = header; // current offset in the class file
        readClassDeclaration(u, c);
        readInterfaces(u, c);

        // Reads the class attributes.
        signature = null;
        sourceFile = null;
        sourceDebug = null;
        enclosingOwner = null;
        enclosingName = null;
        enclosingDesc = null;
        int anns = 0;
        int ianns = 0;
        int innerClasses = 0;

        attributes = null;
        u = getAttributes();

        for (int i = readUnsignedShort(u); i > 0; --i) {
            String attrName = readUTF8(u + 2, c);

            if ("SourceFile".equals(attrName)) {
                sourceFile = readUTF8(u + 8, c);
            }
            else if ("InnerClasses".equals(attrName)) {
                innerClasses = u + 8;
            }
            else if ("EnclosingMethod".equals(attrName)) {
                readEnclosingMethodInfo(u, c);
            }
            else if ("Signature".equals(attrName)) {
                signature = readUTF8(u + 8, c);
            }
            else if ("RuntimeVisibleAnnotations".equals(attrName)) {
                anns = u + 8;
            }
            else if ("Deprecated".equals(attrName)) {
                access |= Opcodes.ACC_DEPRECATED;
            }
            else if ("Synthetic".equals(attrName)) {
                access |= Opcodes.ACC_SYNTHETIC | ACC_SYNTHETIC_ATTRIBUTE;
            }
            else if ("SourceDebugExtension".equals(attrName)) {
                int len = readInt(u + 4);
                sourceDebug = readUTF(u + 8, len, new char[len]);
            }
            else if ("RuntimeInvisibleAnnotations".equals(attrName)) {
                ianns = u + 8;
            }
            else if ("BootstrapMethods".equals(attrName)) {
                readBootstrapMethods(context, u);
            }
            else {
                readAttribute(u, attrName);
            }

            u += 6 + readInt(u + 4);
        }

        readClass(classVisitor);
        readSourceAndDebugInfo(classVisitor, flags);
        readOuterClass(classVisitor);
        readAnnotations(classVisitor, c, anns, true);
        readAnnotations(classVisitor, c, ianns, false);
        readAttributes(classVisitor);
        readInnerClasses(classVisitor, c, innerClasses);
        readFieldsAndMethods(classVisitor, context);
        classVisitor.visitEnd();
    }

    private void readClassDeclaration(int u, char[] c) {
        access = readUnsignedShort(u);
        name = readClass(u + 2, c);
        superClass = readClass(u + 4, c);
    }

    private void readInterfaces(int u, char[] c) {
        interfaces = new String[readUnsignedShort(u + 6)];
        u += 8;

        for (int i = 0; i < interfaces.length; ++i) {
            interfaces[i] = readClass(u, c);
            u += 2;
        }
    }

    private void readEnclosingMethodInfo(int u, char[] c) {
        enclosingOwner = readClass(u + 8, c);
        int item = readUnsignedShort(u + 10);

        if (item != 0) {
            enclosingName = readUTF8(items[item], c);
            enclosingDesc = readUTF8(items[item] + 2, c);
        }
    }

    private void readClass(ClassVisitor classVisitor) {
        int version = readInt(items[1] - 7);
        classVisitor.visit(version, access, name, signature, superClass, interfaces);
    }

    private void readSourceAndDebugInfo(ClassVisitor classVisitor, int flags) {
        if ((flags & SKIP_DEBUG) == 0 && (sourceFile != null || sourceDebug != null)) {
            classVisitor.visitSource(sourceFile, sourceDebug);
        }
    }

    private void readOuterClass(ClassVisitor classVisitor) {
        if (enclosingOwner != null) {
            classVisitor.visitOuterClass(enclosingOwner, enclosingName, enclosingDesc);
        }
    }

    private void readAnnotations(ClassVisitor classVisitor, char[] c, int anns, boolean visible) {
        if (anns != 0) {
            for (int i = readUnsignedShort(anns), v = anns + 2; i > 0; --i) {
                String desc = readUTF8(v, c);
                AnnotationVisitor av = classVisitor.visitAnnotation(desc, visible);
                v = readAnnotationValues(v + 2, c, true, av);
            }
        }
    }

    private void readAttributes(ClassVisitor classVisitor) {
        Attribute attributes = this.attributes;

        while (attributes != null) {
            Attribute attr = attributes.next;
            attributes.next = null;
            classVisitor.visitAttribute(attributes);
            attributes = attr;
        }
    }

    private void readInnerClasses(ClassVisitor classVisitor, char[] c, int innerClasses) {
        if (innerClasses != 0) {
            int v = innerClasses + 2;

            for (int i = readUnsignedShort(innerClasses); i > 0; --i) {
                String name = readClass(v, c);
                String outerName = readClass(v + 2, c);
                String innerName = readUTF8(v + 4, c);
                int access = readUnsignedShort(v + 6);
                classVisitor.visitInnerClass(name, outerName, innerName, access);
                v += 8;
            }
        }
    }

    private void readFieldsAndMethods(ClassVisitor classVisitor, Context context) {
        int u = header + 10 + 2 * interfaces.length;

        for (int i = readUnsignedShort(u - 2); i > 0; --i) {
            u = readField(classVisitor, context, u);
        }

        u += 2;

        for (int i = readUnsignedShort(u - 2); i > 0; --i) {
            u = readMethod(classVisitor, context, u);
        }
    }

    private void readBootstrapMethods(Context context, int u) {
        int[] bootstrapMethods = new int[readUnsignedShort(u + 8)];

        for (int j = 0, v = u + 10; j < bootstrapMethods.length; j++) {
            bootstrapMethods[j] = v;
            v += 2 + readUnsignedShort(v + 2) << 1;
        }

        context.bootstrapMethods = bootstrapMethods;
    }

    /**
     * Reads a field and makes the given visitor visit it.
     *
     * @param classVisitor the visitor that must visit the field.
     * @param context information about the class being parsed.
     * @param u the start offset of the field in the class file.
     *
     * @return the offset of the first byte following the field in the class.
     */
    private int readField(ClassVisitor classVisitor, Context context, int u) {
        // Reads the field declaration.
        char[] c = context.buffer;
        int access = readUnsignedShort(u);
        String name = readUTF8(u + 2, c);
        String desc = readUTF8(u + 4, c);
        u += 6;

        // Reads the field attributes.
        String signature = null;
        int anns = 0;
        int ianns = 0;
        Object value = null;
        attributes = null;

        for (int i = readUnsignedShort(u); i > 0; --i) {
            String attrName = readUTF8(u + 2, c);

            if ("ConstantValue".equals(attrName)) {
                int item = readUnsignedShort(u + 8);
                value = item == 0 ? null : readConst(item, c);
            }
            else if ("Signature".equals(attrName)) {
                signature = readUTF8(u + 8, c);
            }
            else if ("Deprecated".equals(attrName)) {
                access |= Opcodes.ACC_DEPRECATED;
            }
            else if ("Synthetic".equals(attrName)) {
                access |= Opcodes.ACC_SYNTHETIC | ACC_SYNTHETIC_ATTRIBUTE;
            }
            else if ("RuntimeVisibleAnnotations".equals(attrName)) {
                anns = u + 8;
            }
            else if ("RuntimeInvisibleAnnotations".equals(attrName)) {
                ianns = u + 8;
            }
            else {
                readAttribute(u, attrName);
            }

            u += 6 + readInt(u + 4);
        }

        u += 2;

        FieldVisitor fv = classVisitor.visitField(access, name, desc, signature, value);

        if (fv == null) {
            return u;
        }

        readFieldAnnotations(fv, c, anns, true);
        readFieldAnnotations(fv, c, ianns, false);
        readFieldAttributes(fv);

        fv.visitEnd();
        return u;
    }

    private void readFieldAnnotations(FieldVisitor fv, char[] c, int anns, boolean visible) {
        if (anns != 0) {
            for (int i = readUnsignedShort(anns), v = anns + 2; i > 0; --i) {
                String desc = readUTF8(v, c);
                AnnotationVisitor av = fv.visitAnnotation(desc, visible);
                v = readAnnotationValues(v + 2, c, true, av);
            }
        }
    }

    private void readFieldAttributes(FieldVisitor fv) {
        Attribute attributes = this.attributes;

        while (attributes != null) {
            Attribute attr = attributes.next;
            attributes.next = null;
            fv.visitAttribute(attributes);
            attributes = attr;
        }
    }

    /**
     * Reads a method and makes the given visitor visit it.
     *
     * @param classVisitor the visitor that must visit the method.
     * @param context information about the class being parsed.
     * @param u the start offset of the method in the class file.
     *
     * @return the offset of the first byte following the method in the class.
     */
    private int readMethod(ClassVisitor classVisitor, Context context, int u) {
        u = readMethodDeclaration(context, u);

        int code = 0;
        int exception = 0;
        exceptions = null;
        String signature = null;
        int anns = 0;
        int ianns = 0;
        int annDefault = 0;
        int paramAnns = 0;
        int invParamAnns = 0;
        int firstAttribute = u;
        attributes = null;
        char[] c = context.buffer;

        for (int i = readUnsignedShort(u); i > 0; --i) {
            String attrName = readUTF8(u + 2, c);

            if ("Code".equals(attrName)) {
                if (!context.skipCode()) {
                    code = u + 8;
                }
            }
            else if ("Exceptions".equals(attrName)) {
                exception = readExceptionsInThrowsClause(u, c);
            }
            else if ("Signature".equals(attrName)) {
                signature = readUTF8(u + 8, c);
            }
            else if ("Deprecated".equals(attrName)) {
                context.access |= Opcodes.ACC_DEPRECATED;
            }
            else if ("RuntimeVisibleAnnotations".equals(attrName)) {
                anns = u + 8;
            }
            else if ("AnnotationDefault".equals(attrName)) {
                annDefault = u + 8;
            }
            else if ("Synthetic".equals(attrName)) {
                context.access |= Opcodes.ACC_SYNTHETIC | ACC_SYNTHETIC_ATTRIBUTE;
            }
            else if ("RuntimeInvisibleAnnotations".equals(attrName)) {
                ianns = u + 8;
            }
            else if ("RuntimeVisibleParameterAnnotations".equals(attrName)) {
                paramAnns = u + 8;
            }
            else if ("RuntimeInvisibleParameterAnnotations".equals(attrName)) {
                invParamAnns = u + 8;
            }
            else {
                readAttribute(u, attrName);
            }

            u += 6 + readInt(u + 4);
        }

        u += 2;

        MethodVisitor mv = classVisitor.visitMethod(context.access, context.name, context.desc, signature, exceptions);

        if (mv == null || copyMethodBodyIfApplicable(mv, u, exception, signature, firstAttribute)) {
            return u;
        }

        readAnnotationDefaultValue(mv, c, annDefault);
        readAnnotationValues(mv, c, anns, true);
        readAnnotationValues(mv, c, ianns, false);
        readParameterAnnotations(mv, context, paramAnns, true);
        readParameterAnnotations(mv, context, invParamAnns, false);
        readMethodAttributes(mv);
        readMethodCode(mv, context, code);
        mv.visitEnd();
        return u;
    }

    private int readMethodDeclaration(Context context, int u) {
        char[] c = context.buffer;
        context.access = readUnsignedShort(u);
        context.name = readUTF8(u + 2, c);
        context.desc = readUTF8(u + 4, c);
        return u + 6;
    }

    private int readExceptionsInThrowsClause(int u, char[] c) {
        int n = readUnsignedShort(u + 8);
        exceptions = new String[n];
        int exception = u + 10;

        for (int j = 0; j < n; ++j) {
            exceptions[j] = readClass(exception, c);
            exception += 2;
        }

        return exception;
    }

    private void readAttribute(int u, String attrName) {
        int len = readInt(u + 4);
        Attribute attr = readAttribute(attrName, u + 8, len);

        if (attr != null) {
            attr.next = attributes;
            attributes = attr;
        }
    }

    private void readAnnotationDefaultValue(MethodVisitor mv, char[] c, int annotationDefault) {
        if (annotationDefault != 0) {
            AnnotationVisitor dv = mv.visitAnnotationDefault();
            readAnnotationValue(annotationDefault, c, null, dv);

            if (dv != null) {
                dv.visitEnd();
            }
        }
    }

    /**
     * If the returned MethodVisitor is in fact a MethodWriter, it means there is no method adapter between the reader
     * and the writer.
     * If, in addition, the writer's constant pool was copied from this reader (mw.cw.cr == this), and the signature
     * and exceptions of the method have not been changed, then it is possible to skip all visit events and just copy
     * the original code of the method to the writer (the access, name and descriptor can have been changed, this is not
     * important since they are not copied as is from the reader).
     */
    private boolean copyMethodBodyIfApplicable(
       MethodVisitor mv, int u, int exception, String signature, int firstAttribute
    ) {
        if (mv instanceof MethodWriter) {
            MethodWriter mw = (MethodWriter) mv;

            //noinspection StringEquality
            if (mw.cw.cr == this && signature == mw.signature) {
                boolean sameExceptions = false;

                if (exceptions == null) {
                    sameExceptions = mw.exceptionCount == 0;
                }
                else if (exceptions.length == mw.exceptionCount) {
                    sameExceptions = true;

                    for (int j = exceptions.length - 1; j >= 0; --j) {
                        exception -= 2;

                        if (mw.exceptions[j] != readUnsignedShort(exception)) {
                            sameExceptions = false;
                            break;
                        }
                    }
                }

                if (sameExceptions) {
                    /*
                     * We do not copy directly the code into MethodWriter to save a byte array copy operation.
                     * The real copy will be done in ClassWriter.toByteArray().
                     */
                    mw.classReaderOffset = firstAttribute;
                    mw.classReaderLength = u - firstAttribute;
                    return true;
                }
            }
        }

        return false;
    }

    private void readAnnotationValues(MethodVisitor mv, char[] c, int anns, boolean visible) {
        if (anns != 0) {
            for (int i = readUnsignedShort(anns), v = anns + 2; i > 0; --i) {
                String desc = readUTF8(v, c);
                AnnotationVisitor av = mv.visitAnnotation(desc, visible);
                v = readAnnotationValues(v + 2, c, true, av);
            }
        }
    }

    private void readMethodAttributes(MethodVisitor mv) {
        Attribute attributes = this.attributes;

        while (attributes != null) {
            Attribute attr = attributes.next;
            attributes.next = null;
            mv.visitAttribute(attributes);
            attributes = attr;
        }
    }

    private void readMethodCode(MethodVisitor mv, Context context, int code) {
        if (code != 0) {
            mv.visitCode();
            readCode(mv, context, code);
        }
    }

    /**
     * Reads the bytecode of a method and makes the given visitor visit it.
     *
     * @param mv the visitor that must visit the method's code.
     * @param context information about the class being parsed.
     * @param u the start offset of the code attribute in the class file.
     */
    private void readCode(MethodVisitor mv, Context context, int u) {
        // Reads the header.
        char[] c = context.buffer;
        int maxStack = readUnsignedShort(u);
        int maxLocals = readUnsignedShort(u + 2);
        int codeLength = readInt(u + 4);
        u += 8;

        // Reads the bytecode to find the labels.
        int codeStart = u;
        int codeEnd = u + codeLength;

        context.labels = new Label[codeLength + 2];
        Label[] labels = context.labels;

        u = readAllLabelsInCodeBlock(context, u, codeLength, codeStart, codeEnd);

        // Reads the try catch entries to find the labels, and also visits them.
        u = readTryCatchBlocks(mv, u, c, labels);

        u += 2;

        // Reads the code attributes.
        int varTable = 0;
        int varTypeTable = 0;

        for (int i = readUnsignedShort(u); i > 0; --i) {
            String attrName = readUTF8(u + 2, c);

            if ("LocalVariableTable".equals(attrName)) {
                varTable = readLocalVariableTable(context, u, labels, varTable);
            }
            else if ("LocalVariableTypeTable".equals(attrName)) {
                varTypeTable = u + 8;
            }
            else if ("LineNumberTable".equals(attrName)) {
                readLineNumberTable(context, u, labels);
            }

            u += 6 + readInt(u + 4);
        }

        readBytecodeInstructionsInCodeBlock(mv, context, codeStart, codeEnd, labels);

        if (labels[codeLength] != null) {
            mv.visitLabel(labels[codeLength]);
        }

        if (!context.skipDebug() && varTable != 0) {
            readLocalVariableTables(mv, c, labels, varTable, varTypeTable);
        }

        mv.visitMaxs(maxStack, maxLocals);
    }

    private int readAllLabelsInCodeBlock(Context context, int u, int codeLength, int codeStart, int codeEnd) {
        byte[] b = this.b;
        Label[] labels = context.labels;

        readLabel(codeLength + 1, labels);

        while (u < codeEnd) {
            int offset = u - codeStart;
            int opcode = b[u] & 0xFF;

            switch (TYPE[opcode]) {
                case NOARG_INSN:
                case IMPLVAR_INSN:
                    u += 1;
                    break;
                case LABEL_INSN:
                    readLabel(offset + readShort(u + 1), labels);
                    u += 3;
                    break;
                case LABELW_INSN:
                    readLabel(offset + readInt(u + 1), labels);
                    u += 5;
                    break;
                case WIDE_INSN:
                    opcode = b[u + 1] & 0xFF;

                    if (opcode == Opcodes.IINC) {
                        u += 6;
                    }
                    else {
                        u += 4;
                    }

                    break;
                case TABL_INSN:
                    // skips 0 to 3 padding bytes
                    u = u + 4 - (offset & 3);
                    // reads instruction
                    readLabel(offset + readInt(u), labels);

                    for (int i = readInt(u + 8) - readInt(u + 4) + 1; i > 0; --i) {
                        readLabel(offset + readInt(u + 12), labels);
                        u += 4;
                    }

                    u += 12;
                    break;
                case LOOK_INSN:
                    // skips 0 to 3 padding bytes
                    u = u + 4 - (offset & 3);
                    // reads instruction
                    readLabel(offset + readInt(u), labels);

                    for (int i = readInt(u + 4); i > 0; --i) {
                        readLabel(offset + readInt(u + 12), labels);
                        u += 8;
                    }

                    u += 8;
                    break;
                case VAR_INSN:
                case SBYTE_INSN:
                case LDC_INSN:
                    u += 2;
                    break;
                case SHORT_INSN:
                case LDCW_INSN:
                case FIELDORMETH_INSN:
                case TYPE_INSN:
                case IINC_INSN:
                    u += 3;
                    break;
                case ITFMETH_INSN:
                case INDYMETH_INSN:
                    u += 5;
                    break;
                // case MANA_INSN:
                default:
                    u += 4;
                    break;
            }
        }

        return u;
    }

    private int readTryCatchBlocks(MethodVisitor mv, int u, char[] c, Label[] labels) {
        for (int i = readUnsignedShort(u); i > 0; --i) {
            Label start = readLabel(readUnsignedShort(u + 2), labels);
            Label end = readLabel(readUnsignedShort(u + 4), labels);
            Label handler = readLabel(readUnsignedShort(u + 6), labels);
            String type = readUTF8(items[readUnsignedShort(u + 8)], c);

            mv.visitTryCatchBlock(start, end, handler, type);
            u += 8;
        }

        return u;
    }

    private void readLineNumberTable(Context context, int u, Label[] labels) {
        if (!context.skipDebug()) {
            for (int j = readUnsignedShort(u + 8), v = u; j > 0; --j) {
                int label = readUnsignedShort(v + 10);

                if (labels[label] == null) {
                    readLabel(label, labels).status |= Label.DEBUG;
                }

                labels[label].line = readUnsignedShort(v + 12);
                v += 4;
            }
        }
    }

    private int readLocalVariableTable(Context context, int u, Label[] labels, int varTable) {
        if (!context.skipDebug()) {
            varTable = u + 8;

            for (int j = readUnsignedShort(u + 8), v = u; j > 0; --j) {
                int label = readUnsignedShort(v + 10);

                if (labels[label] == null) {
                    readLabel(label, labels).status |= Label.DEBUG;
                }

                label += readUnsignedShort(v + 12);

                if (labels[label] == null) {
                    readLabel(label, labels).status |= Label.DEBUG;
                }

                v += 10;
            }
        }

        return varTable;
    }

    private void readBytecodeInstructionsInCodeBlock(
       MethodVisitor mv, Context context, int codeStart, int codeEnd, Label[] labels
    ) {
        byte[] b = this.b;
        char[] c = context.buffer;
        int u = codeStart;

        while (u < codeEnd) {
            int offset = u - codeStart;

            // Visits the label and line number for this offset, if any.
            Label l = labels[offset];

            if (l != null) {
                mv.visitLabel(l);

                if (!context.skipDebug() && l.line > 0) {
                    mv.visitLineNumber(l.line, l);
                }
            }

            // Visits the instruction at this offset.
            int opcode = b[u] & 0xFF;

            switch (TYPE[opcode]) {
                case NOARG_INSN:
                    mv.visitInsn(opcode);
                    u += 1;
                    break;
                case IMPLVAR_INSN:
                    if (opcode > Opcodes.ISTORE) {
                        opcode -= 59; // ISTORE_0
                        mv.visitVarInsn(Opcodes.ISTORE + (opcode >> 2), opcode & 0x3);
                    }
                    else {
                        opcode -= 26; // ILOAD_0
                        mv.visitVarInsn(Opcodes.ILOAD + (opcode >> 2), opcode & 0x3);
                    }

                    u += 1;
                    break;
                case LABEL_INSN:
                    mv.visitJumpInsn(opcode, labels[offset + readShort(u + 1)]);
                    u += 3;
                    break;
                case LABELW_INSN:
                    mv.visitJumpInsn(opcode - 33, labels[offset + readInt(u + 1)]);
                    u += 5;
                    break;
                case WIDE_INSN:
                    opcode = b[u + 1] & 0xFF;

                    if (opcode == Opcodes.IINC) {
                        mv.visitIincInsn(readUnsignedShort(u + 2), readShort(u + 4));
                        u += 6;
                    }
                    else {
                        mv.visitVarInsn(opcode, readUnsignedShort(u + 2));
                        u += 4;
                    }

                    break;
                case TABL_INSN: {
                    // Skips 0 to 3 padding bytes.
                    u = u + 4 - (offset & 3);

                    // Reads instruction.
                    int label = offset + readInt(u);
                    int min = readInt(u + 4);
                    int max = readInt(u + 8);
                    Label[] table = new Label[max - min + 1];
                    u += 12;

                    for (int i = 0; i < table.length; ++i) {
                        table[i] = labels[offset + readInt(u)];
                        u += 4;
                    }

                    mv.visitTableSwitchInsn(min, max, labels[label], table);
                    break;
                }
                case LOOK_INSN: {
                    // Skips 0 to 3 padding bytes.
                    u = u + 4 - (offset & 3);

                    // Reads instruction.
                    int label = offset + readInt(u);
                    int len = readInt(u + 4);
                    int[] keys = new int[len];
                    Label[] values = new Label[len];
                    u += 8;

                    for (int i = 0; i < len; ++i) {
                        keys[i] = readInt(u);
                        values[i] = labels[offset + readInt(u + 4)];
                        u += 8;
                    }

                    mv.visitLookupSwitchInsn(labels[label], keys, values);
                    break;
                }
                case VAR_INSN:
                    mv.visitVarInsn(opcode, b[u + 1] & 0xFF);
                    u += 2;
                    break;
                case SBYTE_INSN:
                    mv.visitIntInsn(opcode, b[u + 1]);
                    u += 2;
                    break;
                case SHORT_INSN:
                    mv.visitIntInsn(opcode, readShort(u + 1));
                    u += 3;
                    break;
                case LDC_INSN:
                    mv.visitLdcInsn(readConst(b[u + 1] & 0xFF, c));
                    u += 2;
                    break;
                case LDCW_INSN:
                    mv.visitLdcInsn(readConst(readUnsignedShort(u + 1), c));
                    u += 3;
                    break;
                case FIELDORMETH_INSN:
                case ITFMETH_INSN: {
                    int cpIndex = items[readUnsignedShort(u + 1)];
                    boolean itf = b[cpIndex - 1] == IMETH;
                    String owner = readClass(cpIndex, c);
                    cpIndex = items[readUnsignedShort(cpIndex + 2)];
                    String name = readUTF8(cpIndex, c);
                    String desc = readUTF8(cpIndex + 2, c);

                    if (opcode < Opcodes.INVOKEVIRTUAL) {
                        mv.visitFieldInsn(opcode, owner, name, desc);
                    }
                    else {
                        mv.visitMethodInsn(opcode, owner, name, desc, itf);
                    }

                    if (opcode == Opcodes.INVOKEINTERFACE) {
                        u += 5;
                    }
                    else {
                        u += 3;
                    }

                    break;
                }
                case INDYMETH_INSN: {
                    u = readInvokeDynamicInstruction(mv, context, u);
                    break;
                }
                case TYPE_INSN:
                    mv.visitTypeInsn(opcode, readClass(u + 1, c));
                    u += 3;
                    break;
                case IINC_INSN:
                    mv.visitIincInsn(b[u + 1] & 0xFF, b[u + 2]);
                    u += 3;
                    break;
                // case MANA_INSN:
                default:
                    mv.visitMultiANewArrayInsn(readClass(u + 1, c), b[u + 3] & 0xFF);
                    u += 4;
                    break;
            }
        }
    }

    private int readInvokeDynamicInstruction(MethodVisitor mv, Context context, int u) {
        int cpIndex = items[readUnsignedShort(u + 1)];
        int bsmIndex = context.bootstrapMethods[readUnsignedShort(cpIndex)];
        char[] c = context.buffer;
        Handle bsm = (Handle) readConst(readUnsignedShort(bsmIndex), c);
        int bsmArgCount = readUnsignedShort(bsmIndex + 2);
        Object[] bsmArgs = new Object[bsmArgCount];
        bsmIndex += 4;

        for (int i = 0; i < bsmArgCount; i++) {
            bsmArgs[i] = readConst(readUnsignedShort(bsmIndex), c);
            bsmIndex += 2;
        }

        cpIndex = items[readUnsignedShort(cpIndex + 2)];
        String name = readUTF8(cpIndex, c);
        String desc = readUTF8(cpIndex + 2, c);
        mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        u += 5;

        return u;
    }

    private void readLocalVariableTables(MethodVisitor mv, char[] c, Label[] labels, int varTable, int varTypeTable) {
        int u;
        int[] typeTable = null;

        if (varTypeTable != 0) {
            u = varTypeTable + 2;
            typeTable = new int[readUnsignedShort(varTypeTable) * 3];
            for (int i = typeTable.length; i > 0;) {
                typeTable[--i] = u + 6; // signature
                typeTable[--i] = readUnsignedShort(u + 8); // index
                typeTable[--i] = readUnsignedShort(u); // start
                u += 10;
            }
        }

        u = varTable + 2;

        for (int i = readUnsignedShort(varTable); i > 0; --i) {
            int start = readUnsignedShort(u);
            int length = readUnsignedShort(u + 2);
            int index = readUnsignedShort(u + 8);
            String signature = null;

            if (typeTable != null) {
                for (int j = 0; j < typeTable.length; j += 3) {
                    if (typeTable[j] == start && typeTable[j + 1] == index) {
                        signature = readUTF8(typeTable[j + 2], c);
                        break;
                    }
                }
            }

            String name = readUTF8(u + 4, c);
            String desc = readUTF8(u + 6, c);
            mv.visitLocalVariable(name, desc, signature, labels[start], labels[start + length], index);
            u += 10;
        }
    }

    /**
     * Reads parameter annotations and makes the given visitor visit them.
     *
     * @param mv the visitor that must visit the annotations.
     * @param context information about the class being parsed.
     * @param v start offset in {@link #b b} of the annotations to be read.
     * @param visible <tt>true</tt> if the annotations to be read are visible at runtime.
     */
    private void readParameterAnnotations(MethodVisitor mv, Context context, int v, boolean visible) {
        if (v != 0) {
            int n = b[v++] & 0xFF;
            // workaround for a bug in javac (javac compiler generates a parameter
            // annotation array whose size is equal to the number of parameters in
            // the Java source file, while it should generate an array whose size is
            // equal to the number of parameters in the method descriptor - which
            // includes the synthetic parameters added by the compiler). This work-
            // around supposes that the synthetic parameters are the first ones.
            int synthetics = Type.getArgumentTypes(context.desc).length - n;
            AnnotationVisitor av;
            int i;

            for (i = 0; i < synthetics; ++i) {
                // virtual annotation to detect synthetic parameters in MethodWriter
                av = mv.visitParameterAnnotation(i, "Ljava/lang/Synthetic;", false);

                if (av != null) {
                    av.visitEnd();
                }
            }

            char[] c = context.buffer;

            for (; i < n + synthetics; ++i) {
                int j = readUnsignedShort(v);
                v += 2;

                for (; j > 0; --j) {
                    String desc = readUTF8(v, c);
                    av = mv.visitParameterAnnotation(i, desc, visible);
                    v = readAnnotationValues(v + 2, c, true, av);
                }
            }
        }
    }

    /**
     * Reads the values of an annotation and makes the given visitor visit them.
     *
     * @param v the start offset in {@link #b b} of the values to be read
     *          (including the unsigned short that gives the number of values).
     * @param buf buffer to be used to call {@link #readUTF8 readUTF8},
     *            {@link #readClass(int,char[]) readClass} or {@link #readConst readConst}.
     * @param named if the annotation values are named or not.
     * @param av the visitor that must visit the values.
     *
     * @return the end offset of the annotation values.
     */
    private int readAnnotationValues(int v, char[] buf, boolean named, AnnotationVisitor av) {
        int i = readUnsignedShort(v);
        v += 2;

        if (named) {
            for (; i > 0; --i) {
                v = readAnnotationValue(v + 2, buf, readUTF8(v, buf), av);
            }
        }
        else {
            for (; i > 0; --i) {
                v = readAnnotationValue(v, buf, null, av);
            }
        }

        if (av != null) {
            av.visitEnd();
        }

        return v;
    }

    /**
     * Reads a value of an annotation and makes the given visitor visit it.
     *
     * @param v the start offset in {@link #b b} of the value to be read
     *          (<i>not including the value name constant pool index</i>).
     * @param buf buffer to be used to call {@link #readUTF8 readUTF8},
     *            {@link #readClass(int,char[]) readClass} or {@link #readConst readConst}.
     * @param name the name of the value to be read.
     * @param av the visitor that must visit the value.
     *
     * @return the end offset of the annotation value.
     */
    private int readAnnotationValue(int v, char[] buf, String name, AnnotationVisitor av) {
        int i;

        if (av == null) {
            switch (b[v] & 0xFF) {
                case 'e': // enum_const_value
                    return v + 5;
                case '@': // annotation_value
                    return readAnnotationValues(v + 3, buf, true, null);
                case '[': // array_value
                    return readAnnotationValues(v + 1, buf, false, null);
                default:
                    return v + 3;
            }
        }

        switch (b[v++] & 0xFF) {
            case 'I': // pointer to CONSTANT_Integer
            case 'J': // pointer to CONSTANT_Long
            case 'F': // pointer to CONSTANT_Float
            case 'D': // pointer to CONSTANT_Double
                av.visit(name, readConst(readUnsignedShort(v), buf));
                v += 2;
                break;
            case 'B': // pointer to CONSTANT_Byte
                av.visit(name, (byte) readInt(items[readUnsignedShort(v)]));
                v += 2;
                break;
            case 'Z': // pointer to CONSTANT_Boolean
                av.visit(name,
                   readInt(items[readUnsignedShort(v)]) == 0 ? Boolean.FALSE
                      : Boolean.TRUE);
                v += 2;
                break;
            case 'S': // pointer to CONSTANT_Short
                av.visit(name, (short) readInt(items[readUnsignedShort(v)]));
                v += 2;
                break;
            case 'C': // pointer to CONSTANT_Char
                av.visit(name, (char) readInt(items[readUnsignedShort(v)]));
                v += 2;
                break;
            case 's': // pointer to CONSTANT_Utf8
                av.visit(name, readUTF8(v, buf));
                v += 2;
                break;
            case 'e': // enum_const_value
                av.visitEnum(name, readUTF8(v, buf), readUTF8(v + 2, buf));
                v += 4;
                break;
            case 'c': // class_info
                av.visit(name, Type.getType(readUTF8(v, buf)));
                v += 2;
                break;
            case '@': // annotation_value
                v = readAnnotationValues(v + 2, buf, true, av.visitAnnotation(name, readUTF8(v, buf)));
                break;
            case '[': // array_value
                int size = readUnsignedShort(v);
                v += 2;

                if (size == 0) {
                    return readAnnotationValues(v - 2, buf, false, av.visitArray(name));
                }

                switch (b[v++] & 0xFF) {
                    case 'B':
                        byte[] bv = new byte[size];
                        for (i = 0; i < size; i++) {
                            bv[i] = (byte) readInt(items[readUnsignedShort(v)]);
                            v += 3;
                        }
                        av.visit(name, bv);
                        --v;
                        break;
                    case 'Z':
                        boolean[] zv = new boolean[size];
                        for (i = 0; i < size; i++) {
                            zv[i] = readInt(items[readUnsignedShort(v)]) != 0;
                            v += 3;
                        }
                        av.visit(name, zv);
                        --v;
                        break;
                    case 'S':
                        short[] sv = new short[size];
                        for (i = 0; i < size; i++) {
                            sv[i] = (short) readInt(items[readUnsignedShort(v)]);
                            v += 3;
                        }
                        av.visit(name, sv);
                        --v;
                        break;
                    case 'C':
                        char[] cv = new char[size];
                        for (i = 0; i < size; i++) {
                            cv[i] = (char) readInt(items[readUnsignedShort(v)]);
                            v += 3;
                        }
                        av.visit(name, cv);
                        --v;
                        break;
                    case 'I':
                        int[] iv = new int[size];
                        for (i = 0; i < size; i++) {
                            iv[i] = readInt(items[readUnsignedShort(v)]);
                            v += 3;
                        }
                        av.visit(name, iv);
                        --v;
                        break;
                    case 'J':
                        long[] lv = new long[size];
                        for (i = 0; i < size; i++) {
                            lv[i] = readLong(items[readUnsignedShort(v)]);
                            v += 3;
                        }
                        av.visit(name, lv);
                        --v;
                        break;
                    case 'F':
                        float[] fv = new float[size];
                        for (i = 0; i < size; i++) {
                            fv[i] = Float
                               .intBitsToFloat(readInt(items[readUnsignedShort(v)]));
                            v += 3;
                        }
                        av.visit(name, fv);
                        --v;
                        break;
                    case 'D':
                        double[] dv = new double[size];
                        for (i = 0; i < size; i++) {
                            dv[i] = Double
                               .longBitsToDouble(readLong(items[readUnsignedShort(v)]));
                            v += 3;
                        }
                        av.visit(name, dv);
                        --v;
                        break;
                    default:
                        v = readAnnotationValues(v - 3, buf, false, av.visitArray(name));
                }
        }

        return v;
    }

    /**
     * Returns the label corresponding to the given offset. The default implementation of this method creates a label
     * for the given offset if it has not been already created.
     *
     * @param offset a bytecode offset in a method.
     * @param labels the already created labels, indexed by their offset. If a label already exists for offset this
     *               method must not create a new one. Otherwise it must store the new label in this array.
     *
     * @return a non null Label, which must be equal to labels[offset].
     */
    private Label readLabel(int offset, Label[] labels) {
        if (labels[offset] == null) {
            labels[offset] = new Label();
        }

        return labels[offset];
    }

    /**
     * Returns the start index of the attribute_info structure of this class.
     */
    private int getAttributes() {
        // skips the header
        int u = header + 8 + readUnsignedShort(header + 6) * 2;

        // skips fields and methods
        for (int i = readUnsignedShort(u); i > 0; --i) {
            for (int j = readUnsignedShort(u + 8); j > 0; --j) {
                u += 6 + readInt(u + 12);
            }

            u += 8;
        }

        u += 2;

        for (int i = readUnsignedShort(u); i > 0; --i) {
            for (int j = readUnsignedShort(u + 8); j > 0; --j) {
                u += 6 + readInt(u + 12);
            }

            u += 8;
        }

        // the attribute_info structure starts just after the methods
        return u + 2;
    }

    /**
     * Reads an attribute in {@link #b b}.
     *
     * @param type the type of the attribute.
     * @param off index of the first byte of the attribute's content in {@link #b b}.
     *            The 6 attribute header bytes, containing the type and the length of the attribute, are not taken into
     *            account here (they have already been read).
     * @param len the length of the attribute's content.
     *
     * @return the attribute that has been read.
     */
    private Attribute readAttribute(String type, int off, int len) {
        Attribute attribute = new Attribute(type);
        return attribute.read(this, off, len);
    }

    /**
     * Reads a byte value in {@link #b b}.
     *
     * @param index the start index of the value to be read in {@link #b b}.
     * @return the read value.
     */
    private int readByte(int index) { return b[index] & 0xFF; }

    /**
     * Reads an unsigned short value in {@link #b b}.
     *
     * @param index the start index of the value to be read in {@link #b b}.
     * @return the read value.
     */
    private int readUnsignedShort(int index) {
        byte[] b = this.b;
        return ((b[index] & 0xFF) << 8) | (b[index + 1] & 0xFF);
    }

    /**
     * Reads a signed short value in {@link #b b}.
     *
     * @param index the start index of the value to be read in {@link #b b}.
     * @return the read value.
     */
    private short readShort(int index) {
        byte[] b = this.b;
        return (short) (((b[index] & 0xFF) << 8) | (b[index + 1] & 0xFF));
    }

    /**
     * Reads a signed int value in {@link #b b}.
     *
     * @param index the start index of the value to be read in {@link #b b}.
     * @return the read value.
     */
    private int readInt(int index) {
        byte[] b = this.b;
        return ((b[index] & 0xFF) << 24) | ((b[index + 1] & 0xFF) << 16)
           | ((b[index + 2] & 0xFF) << 8) | (b[index + 3] & 0xFF);
    }

    /**
     * Reads a signed long value in {@link #b b}.
     *
     * @param index the start index of the value to be read in {@link #b b}.
     * @return the read value.
     */
    private long readLong(int index) {
        long l1 = readInt(index);
        long l0 = readInt(index + 4) & 0xFFFFFFFFL;
        return (l1 << 32) | l0;
    }

    /**
     * Reads an UTF8 string constant pool item in {@link #b b}.
     *
     * @param index the start index of an unsigned short value in {@link #b b}, whose value is the index of an UTF8
     *              constant pool item.
     * @param buf buffer to be used to read the item. This buffer must be sufficiently large. It is not automatically
     *            resized.
     * @return the String corresponding to the specified UTF8 item.
     */
    private String readUTF8(int index, char[] buf) {
        int item = readUnsignedShort(index);

        if (index == 0 || item == 0) {
            return null;
        }

        String s = strings[item];

        if (s != null) {
            return s;
        }

        index = items[item];
        return strings[item] = readUTF(index + 2, readUnsignedShort(index), buf);
    }

    /**
     * Reads UTF8 string in {@link #b b}.
     *
     * @param index start offset of the UTF8 string to be read.
     * @param utfLen length of the UTF8 string to be read.
     * @param buf buffer to be used to read the string. This buffer must be sufficiently large.
     *            It is not automatically resized.
     * @return the String corresponding to the specified UTF8 string.
     */
    private String readUTF(int index, int utfLen, char[] buf) {
        int endIndex = index + utfLen;
        byte[] b = this.b;
        int strLen = 0;
        int c;
        int st = 0;
        char cc = 0;
        while (index < endIndex) {
            c = b[index++];
            switch (st) {
                case 0:
                    c = c & 0xFF;
                    if (c < 0x80) { // 0xxxxxxx
                        buf[strLen++] = (char) c;
                    } else if (c < 0xE0 && c > 0xBF) { // 110x xxxx 10xx xxxx
                        cc = (char) (c & 0x1F);
                        st = 1;
                    } else { // 1110 xxxx 10xx xxxx 10xx xxxx
                        cc = (char) (c & 0x0F);
                        st = 2;
                    }
                    break;

                case 1: // byte 2 of 2-byte char or byte 3 of 3-byte char
                    buf[strLen++] = (char) ((cc << 6) | (c & 0x3F));
                    st = 0;
                    break;

                case 2: // byte 2 of 3-byte char
                    cc = (char) ((cc << 6) | (c & 0x3F));
                    st = 1;
                    break;
            }
        }
        return new String(buf, 0, strLen);
    }

    /**
     * Reads a class constant pool item in {@link #b b}.
     *
     * @param index
     *            the start index of an unsigned short value in {@link #b b},
     *            whose value is the index of a class constant pool item.
     * @param buf
     *            buffer to be used to read the item. This buffer must be
     *            sufficiently large. It is not automatically resized.
     * @return the String corresponding to the specified class item.
     */
    private String readClass(int index, char[] buf) {
        // computes the start index of the CONSTANT_Class item in b
        // and reads the CONSTANT_Utf8 item designated by
        // the first two bytes of this CONSTANT_Class item
        return readUTF8(items[readUnsignedShort(index)], buf);
    }

    /**
     * Reads a numeric or string constant pool item in {@link #b b}.
     *
     * @param item the index of a constant pool item.
     * @param buf buffer to be used to read the item. This buffer must be sufficiently large. It is not automatically
     *            resized.
     * @return the {@link Integer}, {@link Float}, {@link Long}, {@link Double}, {@link String}, {@link Type}
     * or {@link Handle} corresponding to the given constant pool item.
     */
    private Object readConst(int item, char[] buf) {
        int index = items[item];
        switch (b[index - 1]) {
            case INT:
                return readInt(index);
            case FLOAT:
                return Float.intBitsToFloat(readInt(index));
            case LONG:
                return readLong(index);
            case DOUBLE:
                return Double.longBitsToDouble(readLong(index));
            case CLASS:
                return Type.getObjectType(readUTF8(index, buf));
            case STR:
                return readUTF8(index, buf);
            case MTYPE:
                return Type.getMethodType(readUTF8(index, buf));
            default: // case ClassWriter.HANDLE_BASE + [1..9]:
                int tag = readByte(index);
                int[] items = this.items;
                int cpIndex = items[readUnsignedShort(index + 1)];
                String owner = readClass(cpIndex, buf);
                cpIndex = items[readUnsignedShort(cpIndex + 2)];
                String name = readUTF8(cpIndex, buf);
                String desc = readUTF8(cpIndex + 2, buf);
                return new Handle(tag, owner, name, desc);
        }
    }

    public int getVersion() {
        return readShort(6);
    }
}
