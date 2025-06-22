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
package org.hid4java.event;

import org.hid4java.HidDevice;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

/**
 * Describes an event relating to an HID device.
 *
 * @since 0.0.1
 */
public class HidServicesEvent {

    private final @Nullable HidDevice device;
    private final byte @Nullable [] data;

    /**
     * Constructs a new {@code HidServicesEvent}.
     *
     * @param device The device that triggered the event.
     */
    public HidServicesEvent(@Nullable HidDevice device) {
        this.device = device;
        this.data = null;
    }

    /**
     * Constructs a new {@code HidServicesEvent}.
     *
     * @param device The device that triggered the event.
     * @param data   The data received from this event.
     * @since 0.8.0
     */
    public HidServicesEvent(
            @Nullable HidDevice device,
            byte @Nullable [] data) {
        this.device = device;
        this.data = copyOfOrNull(data);
    }

    /**
     * Returns the device that triggered the event.
     *
     * @return The device that triggered the event.
     */
    public final @Nullable HidDevice getDevice() {
        return this.device;
    }

    /**
     * Returns a copy of the received data.
     * <p>
     * <b>Note:</b> This may be multiple packets of data.
     *
     * @return A copy of the received data.
     */
    public final byte @Nullable [] getDataReceived() {
        /*
         * Although we made a copy in the constructor, we don't want
         * callers of this method to modify it for the next invocations
         * of this method.
         */
        return copyOfOrNull(data);
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

        HidServicesEvent that = (HidServicesEvent) obj;
        return Objects.equals(device, that.device)
                && Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(device, Arrays.hashCode(data));
    }

    @Override
    public @NotNull String toString() {
        return this.getClass().getSimpleName() + "[" +
                "device=" + device + "," +
                "data=" + Arrays.toString(data) +
                "]";
    }

    private static byte @Nullable []
    copyOfOrNull(byte @Nullable [] data) {
        return data != null
                ? Arrays.copyOf(data, data.length)
                : null;
    }

}
