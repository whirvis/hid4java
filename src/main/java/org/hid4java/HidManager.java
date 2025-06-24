/*
 * The MIT License (MIT)
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
 * The above copyright notice and this permission notice shall be included in all
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Factory for configured HID services.
 *
 * @since 0.0.1
 */
public class HidManager {

    private static final @NotNull Lock
            SERVICES_LOCK = new ReentrantLock();

    private static @Nullable HidServices hidServices;

    /**
     * Returns the HID services.
     * <p>
     * If the HID services have not yet been initialized, this method will
     * initialize them with the default parameters.
     *
     * @return The HID services.
     * @see #getHidServices(HidServicesSpecification)
     */
    public static @NotNull HidServices getHidServices() {
        SERVICES_LOCK.lock();
        try {
            if (hidServices == null) {
                hidServices = new HidServices();
            }
            return hidServices;
        } finally {
            SERVICES_LOCK.unlock();
        }
    }

    /**
     * Initializes the HID services with the specified parameters.
     *
     * @param specs The parameters for configuring HID services.
     * @return A single instance of the HID services using the specified
     * parameters.
     * @throws NullPointerException  If {@code specs} are {@code null}.
     * @throws IllegalStateException If this method has already been called
     *                               (use {@link #getHidServices()}).
     * @since 0.5.0
     */
    public static @NotNull HidServices getHidServices(
            @NotNull HidServicesSpecification specs) {
        Objects.requireNonNull(specs, "specs cannot be null");
        SERVICES_LOCK.lock();
        try {
            if (hidServices != null) {
                String message = "HID services already initialized";
                throw new IllegalStateException(message);
            }
            hidServices = new HidServices(specs);
            return hidServices;
        } finally {
            SERVICES_LOCK.unlock();
        }
    }

}
