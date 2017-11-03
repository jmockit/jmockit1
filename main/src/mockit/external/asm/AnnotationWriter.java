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

import java.lang.reflect.*;

/**
 * An {@link AnnotationVisitor} that generates annotations in bytecode form.
 * 
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
final class AnnotationWriter extends AnnotationVisitor
{
    /**
     * The class writer to which this annotation must be added.
     */
    private final ClassWriter cw;

    /**
     * The number of values in this annotation.
     */
    private int size;

    /**
     * <tt>true<tt> if values are named, <tt>false</tt> otherwise.
     * Annotation writers used for annotation default and annotation arrays use unnamed values.
     */
    private final boolean named;

    /**
     * The annotation values in bytecode form. This byte vector only contains the values themselves, i.e. the number of
     * values must be stored as a unsigned short just before these bytes.
     */
    private final ByteVector bv;

    /**
     * The byte vector to be used to store the number of values of this
     * annotation. See {@link #bv}.
     */
    private final ByteVector parent;

    /**
     * Where the number of values of this annotation must be stored in {@link #parent}.
     */
    private final int offset;

    /**
     * Next annotation writer. This field is used to store annotation lists.
     */
    AnnotationWriter next;

    /**
     * Previous annotation writer. This field is used to store annotation lists.
     */
    AnnotationWriter prev;

    /**
     * Constructs a new {@link AnnotationWriter}.
     * 
     * @param cw the class writer to which this annotation must be added.
     * @param named <tt>true<tt> if values are named, <tt>false</tt> otherwise.
     * @param bv where the annotation values must be stored.
     * @param parent where the number of annotation values must be stored.
     * @param offset where in <tt>parent</tt> the number of annotation values must be stored.
     */
    AnnotationWriter(ClassWriter cw, boolean named, ByteVector bv, ByteVector parent, int offset) {
        this.cw = cw;
        this.named = named;
        this.bv = bv;
        this.parent = parent;
        this.offset = offset;
    }

    @Override
    public void visit(String name, Object value) {
        ++size;

        if (named) {
            putString(name);
        }

        if (value instanceof String) {
            putString('s', (String) value);
        }
        else if (value instanceof Byte) {
            putInteger('B', (Byte) value);
        }
        else if (value instanceof Boolean) {
            int v = (Boolean) value ? 1 : 0;
            putInteger('Z', v);
        }
        else if (value instanceof Character) {
            putInteger('C', (Character) value);
        }
        else if (value instanceof Short) {
            putInteger('S', (Short) value);
        }
        else if (value instanceof Type) {
            String typeDescriptor = ((Type) value).getDescriptor();
            putString('c', typeDescriptor);
        }
        else if (value instanceof byte[]) {
            readAnnotationValues('B', value);
        }
        else if (value instanceof boolean[]) {
            readAnnotationValues('Z', value);
        }
        else if (value instanceof short[]) {
            readAnnotationValues('S', value);
        }
        else if (value instanceof char[]) {
            readAnnotationValues('C', value);
        }
        else if (value instanceof int[]) {
            readAnnotationValues('I', value);
        }
        else if (value instanceof long[]) {
            readAnnotationValues('J', value);
        }
        else if (value instanceof float[]) {
            readAnnotationValues('F', value);
        }
        else if (value instanceof double[]) {
            readAnnotationValues('D', value);
        }
        else {
            Item item = cw.newConstItem(value);
            char itemType = ".s.IFJDCS".charAt(item.type);
            putItem(itemType, item);
        }
    }

    private void putItem(int b, Item item) {
        bv.put12(b, item.index);
    }

    private void putInteger(int b, int value) {
        int itemIndex = cw.newInteger(value).index;
        bv.put12(b, itemIndex);
    }

    private void putString(int b, String value) {
        int itemIndex = cw.newUTF8(value);
        bv.put12(b, itemIndex);
    }

    private void putString(String value) {
        int itemIndex = cw.newUTF8(value);
        bv.putShort(itemIndex);
    }

    private void putArrayLength(int length) {
        bv.put12('[', length);
    }

    private void readAnnotationValues(char arrayType, Object arrayValue) {
        int length = Array.getLength(arrayValue);
        putArrayLength(length);

        for (int i = 0; i < length; i++) {
            Item item;

            if (arrayType == 'J') {
                long elementValue = Array.getLong(arrayValue, i);
                item = cw.newLong(elementValue);
            }
            else if (arrayType == 'F') {
                float elementValue = Array.getFloat(arrayValue, i);
                item = cw.newFloat(elementValue);
            }
            else if (arrayType == 'D') {
                double elementValue = Array.getDouble(arrayValue, i);
                item = cw.newDouble(elementValue);
            }
            else {
                int value = arrayType == 'Z' ? Array.getBoolean(arrayValue, i) ? 1 : 0 : Array.getInt(arrayValue, i);
                item = cw.newInteger(value);
            }

            putItem(arrayType, item);
        }
    }

    @Override
    public void visitEnum(String name, String desc, String value) {
        ++size;

        if (named) {
            putString(name);
        }

        putString('e', desc);
        putString(value);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String desc) {
        ++size;

        if (named) {
            putString(name);
        }

        // write tag and type, and reserve space for values count
        putString('@', desc);
        bv.putShort(0);

        return new AnnotationWriter(cw, true, bv, bv, bv.length - 2);
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        ++size;

        if (named) {
            putString(name);
        }

        // write tag, and reserve space for array size
        putArrayLength(0);

        return new AnnotationWriter(cw, false, bv, bv, bv.length - 2);
    }

    @Override
    public void visitEnd() {
        if (parent != null) {
            byte[] data = parent.data;
            data[offset] = (byte) (size >>> 8);
            data[offset + 1] = (byte) size;
        }
    }

    // ------------------------------------------------------------------------
    // Utility methods
    // ------------------------------------------------------------------------

    /**
     * Returns the size of this annotation writer list.
     */
    int getSize() {
        int size = 0;
        AnnotationWriter aw = this;

        while (aw != null) {
            size += aw.bv.length;
            aw = aw.next;
        }

        return size;
    }

    /**
     * Puts the annotations of this annotation writer list into the given byte vector.
     * 
     * @param out where the annotations must be put.
     */
    void put(ByteVector out) {
        int n = 0;
        int size = 2;
        AnnotationWriter aw = this;
        AnnotationWriter last = null;

        while (aw != null) {
            ++n;
            size += aw.bv.length;
            aw.visitEnd(); // in case user forgot to call visitEnd
            aw.prev = last;
            last = aw;
            aw = aw.next;
        }

        out.putInt(size);
        out.putShort(n);
        aw = last;

        while (aw != null) {
            out.putByteArray(aw.bv.data, 0, aw.bv.length);
            aw = aw.prev;
        }
    }

    /**
     * Puts the given annotation lists into the given byte vector.
     * 
     * @param anns an array of annotation writer lists.
     * @param off index of the first annotation to be written.
     * @param out where the annotations must be put.
     */
    static void put(AnnotationWriter[] anns, int off, ByteVector out) {
        int size = 1 + 2 * (anns.length - off);

        for (int i = off; i < anns.length; ++i) {
            size += anns[i] == null ? 0 : anns[i].getSize();
        }

        out.putInt(size).putByte(anns.length - off);

        for (int i = off; i < anns.length; ++i) {
            AnnotationWriter aw = anns[i];
            AnnotationWriter last = null;
            int n = 0;

            while (aw != null) {
                ++n;
                aw.visitEnd(); // in case user forgot to call visitEnd
                aw.prev = last;
                last = aw;
                aw = aw.next;
            }

            out.putShort(n);
            aw = last;

            while (aw != null) {
                out.putByteArray(aw.bv.data, 0, aw.bv.length);
                aw = aw.prev;
            }
        }
    }

    /**
     * Puts the given type reference and type path into the given byte vector.
     * LOCAL_VARIABLE and RESOURCE_VARIABLE target types are not supported.
     * 
     * @param typeRef a reference to the annotated type.
     * @param typePath the path to the annotated type argument, wildcard bound, array element type, or static inner type
     *                 within 'typeRef'. May be <tt>null</tt> if the annotation targets 'typeRef' as a whole.
     * @param out where the type reference and type path must be put.
     */
    static void putTarget(int typeRef, TypePath typePath, ByteVector out) {
        switch (typeRef >>> 24) {
        case 0x00: // CLASS_TYPE_PARAMETER
        case 0x01: // METHOD_TYPE_PARAMETER
        case 0x16: // METHOD_FORMAL_PARAMETER
            out.putShort(typeRef >>> 16);
            break;
        case 0x13: // FIELD
        case 0x14: // METHOD_RETURN
        case 0x15: // METHOD_RECEIVER
            out.putByte(typeRef >>> 24);
            break;
        case 0x47: // CAST
        case 0x48: // CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT
        case 0x49: // METHOD_INVOCATION_TYPE_ARGUMENT
        case 0x4A: // CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT
        case 0x4B: // METHOD_REFERENCE_TYPE_ARGUMENT
            out.putInt(typeRef);
            break;
        // case 0x10: // CLASS_EXTENDS
        // case 0x11: // CLASS_TYPE_PARAMETER_BOUND
        // case 0x12: // METHOD_TYPE_PARAMETER_BOUND
        // case 0x17: // THROWS
        // case 0x42: // EXCEPTION_PARAMETER
        // case 0x43: // INSTANCEOF
        // case 0x44: // NEW
        // case 0x45: // CONSTRUCTOR_REFERENCE
        // case 0x46: // METHOD_REFERENCE
        default:
            out.put12(typeRef >>> 24, (typeRef & 0xFFFF00) >> 8);
            break;
        }

        if (typePath == null) {
            out.putByte(0);
        }
        else {
            int length = typePath.b[typePath.offset] * 2 + 1;
            out.putByteArray(typePath.b, typePath.offset, length);
        }
    }
}
