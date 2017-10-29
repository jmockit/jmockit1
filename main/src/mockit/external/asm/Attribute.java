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

/**
 * A non standard class, field, method or code attribute.
 *
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
public final class Attribute
{
    /**
     * The type of this attribute.
     */
    private final String type;

    /**
     * The raw value of this attribute, used only for unknown attributes.
     */
    private final byte[] value;

    /**
     * The next attribute in this attribute list. May be <tt>null</tt>.
     */
    Attribute next;

    /**
     * Constructs a new attribute with contents read from a given class reader.
     *
     * @param cr the class that contains the attribute to be read.
     * @param off index of the first byte of the attribute's content in {@link ClassReader#b cr.b}.
     *            The 6 attribute header bytes, containing the type and the length of the attribute, are not taken into
     *            account here.
     * @param len the length of the attribute's content.
     */
    Attribute(String type, ClassReader cr, int off, int len) {
        this.type = type;
        value = new byte[len];
        System.arraycopy(cr.b, off, value, 0, len);
    }

    /**
     * Returns the length of the attribute list that begins with this attribute.
     */
    int getCount() {
        int count = 0;
        Attribute attr = this;

        while (attr != null) {
            count += 1;
            attr = attr.next;
        }

        return count;
    }

    /**
     * Returns the size of all the attributes in this attribute list.
     * This size includes the size of the attribute headers.
     *
     * @param cw the class writer to be used to convert the attributes into byte arrays.
     */
    int getSize(ClassWriter cw) {
        Attribute attr = this;
        int size = 0;

        while (attr != null) {
            cw.newUTF8(attr.type);
            size += attr.value.length + 6;
            attr = attr.next;
        }

        return size;
    }

    /**
     * Writes all the attributes of this attribute list in the given byte vector.
     *
     * @param cw the class writer to be used to convert the attributes into byte arrays.
     * @param out where the attributes must be written.
     */
    void put(ClassWriter cw, ByteVector out) {
        Attribute attr = this;

        while (attr != null) {
            ByteVector b = new ByteVector(attr.value);
            out.putShort(cw.newUTF8(attr.type)).putInt(b.length);
            out.putByteArray(b.data, 0, b.length);
            attr = attr.next;
        }
    }
}
