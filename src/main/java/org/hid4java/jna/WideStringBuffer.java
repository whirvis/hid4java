/*
 * the MIT License (MIT)
 *
 * Copyright (c) 2014-2025 Gary Rowe, "Whirvis" Trent Summerlin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * the above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.hid4java.jna;

import com.sun.jna.Structure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Wrapper for a buffer containing {@code wchar_t} elements.
 *
 * @since 0.1.0
 */
public class WideStringBuffer extends Structure implements Structure.ByReference {

    public byte @Nullable [] buffer;

    /**
     * Constructs a zeroed-out buffer containing {@code wchar_t}
     * elements.
     *
     * @param length The number of bytes in the buffer.
     * @throws IllegalArgumentException If {@code len} is negative.
     */
    public WideStringBuffer(int length) {
        this.buffer = new byte[length];
    }

    /**
     * Constructs a wrapper for a buffer containing {@code wchar_t}
     * elements.
     *
     * @param buffer The buffer to wrap around.
     */
    public WideStringBuffer(byte @Nullable [] buffer) {
        this.buffer = buffer;
    }

    @Override
    protected @NotNull List<@NotNull String> getFieldOrder() {
        return Collections.singletonList("buffer");
    }

    @Override
    public int hashCode() {
        /*
         * We check if the buffer is null and return zero if it is,
         * because we don't want to return a hash of the string whose
         * contents are "null".
         */
        if (buffer == null) {
            return 0;
        }

        return this.toString().hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true; /* we equal ourselves */
        } else if (obj == null) {
            return false; /* nothing to compare with */
        } else if (obj.getClass() != this.getClass()) {
            return false; /* child must implement */
        }

        WideStringBuffer that = (WideStringBuffer) obj;

        /*
         * If our buffer is null, then check if the other buffer is also
         * null. We don't want a null buffer to be considered equal to a
         * buffer containing the string "null".
         */
        if (buffer == null) {
            return that.buffer == null;
        }

        return this.toString().equals(that.toString());
    }

    @Override
    public @NotNull String toString() {
        if (buffer == null) {
            return "null";
        }

        StringBuilder str = new StringBuilder();
        for (int i = 0; i < buffer.length && buffer[i] != '\0'; i += 2) {
            int codepoint = (buffer[i] | buffer[i + 1] << 8);
            str.append((char) codepoint);
        }

        return str.toString();
    }

}
