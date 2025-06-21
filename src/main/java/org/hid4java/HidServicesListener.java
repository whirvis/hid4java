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
package org.hid4java;

import org.hid4java.event.HidServicesEvent;
import org.jetbrains.annotations.NotNull;

import java.util.EventListener;

/**
 * Interface to listen for HID events.
 *
 * @since 0.0.1
 */
public interface HidServicesListener extends EventListener {

    /**
     * Called when an HID device is attached.
     * <p>
     * <b>Default behavior:</b> No-op.
     *
     * @param event The event.
     */
    default void hidDeviceAttached(@NotNull HidServicesEvent event) {
        /* no-op by default */
    }

    /**
     * Called when an HID device is detached.
     * <p>
     * <b>Default behavior:</b> No-op.
     *
     * @param event The event.
     */
    default void hidDeviceDetached(@NotNull HidServicesEvent event) {
        /* no-op by default */
    }

    /**
     * Called when an HID failure occurs (enumeration, data transfer, etc.)
     * <p>
     * <b>Default behavior:</b> No-op.
     *
     * @param event The event.
     */
    default void hidFailure(@NotNull HidServicesEvent event) {
        /* no-op by default */
    }

    /**
     * Called when an HID input data buffer is populated.
     * <p>
     * <b>Default behavior:</b> No-op.
     *
     * @param event The event.
     */
    default void hidDataReceived(@NotNull HidServicesEvent event) {
        /* no-op by default */
    }

    /**
     * Called when an uncaught exception is thrown in a callback.
     * To prevent a stack overflow, any exceptions that occur in this
     * callback will only have their stack trace printed.
     * <p>
     * <b>Default behavior:</b> Prints the stack trace.
     *
     * @param event The event.
     * @param cause The uncaught exception.
     */
    @SuppressWarnings({"unused", "CallToPrintStackTrace"})
    default void hidListenerException(
            @NotNull HidServicesEvent event,
            @NotNull Throwable cause
    ) {
        /* TODO: more robust logging */
        cause.printStackTrace();
    }

}
