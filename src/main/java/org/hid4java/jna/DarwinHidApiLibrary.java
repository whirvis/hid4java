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

import com.sun.jna.Native;
import com.sun.jna.WString;
import org.jetbrains.annotations.NotNull;

/**
 * JNA proxy to the Darwin HID API library.
 *
 * @since 0.8.0
 */
public interface DarwinHidApiLibrary extends HidRawHidApiLibrary {

    @NotNull DarwinHidApiLibrary INSTANCE = Native.load(
            "hidapi", DarwinHidApiLibrary.class);

    /**
     * Changes the behavior of opening HID devices.
     * <p>
     * By default, all HID devices are opened in exclusive mode.
     * <p>
     * <b>Note:</b> Calling this function before {@link #hid_init()}
     * or after {@link #hid_exit()} has no effect.
     *
     * @param openExclusive Zero to have all further devices opened in
     *                      non-exclusive mode. Any other value will have
     *                      them opened in exclusive mode.
     * @see #hid_open(short, short, WString)
     * @see #hid_open_path(String)
     */
    void hid_darwin_set_open_exclusive(int openExclusive);

}
