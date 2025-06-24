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

import org.hid4java.event.HidServicesListenerList;
import org.hid4java.jna.HidApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * JNA bridge class to the HID API.
 * <p>
 * This class requires the HID API to be present on the classpath
 * or the system library search path.
 *
 * @since 0.0.1
 */
public class HidServices {

    private final HidServicesListenerList listeners;
    private final HidDeviceManager manager;
    private final Lock servicesLock;

    /**
     * Constructs a new {@code HidServices}.
     *
     * @param specs The configuration for HID services.
     * @throws NullPointerException If {@code specs} are {@code null}.
     * @throws HidException         If an HID error occurs.
     */
    public HidServices(@NotNull HidServicesSpecification specs) {
        Objects.requireNonNull(specs, "specs cannot be null");

        this.listeners = new HidServicesListenerList();
        this.manager = new HidDeviceManager(listeners, specs);
        this.servicesLock = new ReentrantLock();

        /* ensures proper shutdown when application exits */
        if (specs.isAutoShutdown()) {
            Runtime runtime = Runtime.getRuntime();
            runtime.addShutdownHook(new Thread(this::shutdown));
        }

        if (specs.isAutoStart()) {
            manager.start();
        }
    }

    /**
     * Constructs a new {@code HidServices} with the default
     * configuration.
     *
     * @throws HidException If an HID error occurs.
     */
    public HidServices() {
        this(new HidServicesSpecification());
    }

    /**
     * Adds a listener.
     *
     * @param listener The listener to add.
     * @throws NullPointerException If {@code listener} is {@code null}.
     */
    public void addHidServicesListener(
            @NotNull HidServicesListener listener) {
        Objects.requireNonNull(listener, "listener cannot be null");

        servicesLock.lock();
        try {
            listeners.add(listener);
        } finally {
            servicesLock.unlock();
        }
    }

    /**
     * Removes a listener.
     *
     * @param listener The listener to remove.
     */
    public void removeHidServicesListener(
            @Nullable HidServicesListener listener) {
        if (listener == null) {
            return; /* don't bother with obtaining a lock */
        }

        servicesLock.lock();
        try {
            listeners.remove(listener);
        } finally {
            servicesLock.unlock();
        }
    }

    /**
     * Returns a list of all currently attached HID devices.
     *
     * @return A list of all currently attached HID devices.
     */
    public @NotNull List<HidDevice> getAttachedHidDevices() {
        servicesLock.lock();
        try {
            return manager.getAttachedHidDevices();
        } finally {
            servicesLock.unlock();
        }
    }

    /**
     * Manually scans for HID device connection changes.
     * <p>
     * This will trigger listener events as required.
     *
     * @throws HidException If an HID error occurs.
     */
    public void scan() {
        servicesLock.lock();
        try {
            manager.scan();
        } finally {
            servicesLock.unlock();
        }
    }

    /**
     * Starts all threads (enumeration, data read, etc.) as configured.
     *
     * @throws HidException If an HID error occurs.
     */
    public void start() {
        servicesLock.lock();
        try {
            manager.start();
        } finally {
            servicesLock.unlock();
        }
    }

    /**
     * Stops all threads (enumeration, data read, etc.), closes all devices,
     * and clears all listeners.
     *
     * @throws HidException If an HID error occurs.
     */
    public void stop() {
        servicesLock.lock();
        try {
            manager.stop();
            listeners.clear();
        } finally {
            servicesLock.unlock();
        }
    }

    /**
     * Stops all device threads and shuts down the {@link HidApi}.
     *
     * @throws HidException If an HID error occurs.
     */
    public void shutdown() {
        servicesLock.lock();
        try {
            this.stop();
            HidApi.exit();
        } finally {
            servicesLock.unlock();
        }
    }

    /**
     * Finds and opens the first attached HID device that matches a given
     * vendor ID, product ID, and serial number.
     *
     * @param vendorId     The vendor ID, {@code 0} for wildcard.
     * @param productId    The product ID, {@code 0} for wildcard.
     * @param serialNumber The serial number, {@code null} for wildcard.
     * @return The first matching device, {@code null} if no such device
     * could be found.
     */
    public @Nullable HidDevice openHidDevice(
            @Range(from = 0x0000, to = 0xFFFF) int vendorId,
            @Range(from = 0x0000, to = 0xFFFF) int productId,
            @Nullable String serialNumber) {
        List<HidDevice> devices = manager.getAttachedHidDevices();
        for (HidDevice device : devices) {
            if (!device.matches(vendorId, productId, serialNumber)) {
                continue; /* not the device we're looking for */
            }
            device.open();
            return device;
        }
        return null;
    }

    /**
     * Returns the current version of the underlying HID API library.
     *
     * @return The current version in "major.minor.patch" format.
     * @throws IllegalStateException If the HID API is not initialized.
     */
    public static @NotNull String getNativeVersion() {
        return HidApi.getVersion();
    }

}
