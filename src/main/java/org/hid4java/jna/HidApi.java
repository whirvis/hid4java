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

import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * JNA proxy to access the HID API library.
 *
 * @since 0.0.1
 */
public class HidApi {

    private static final int WSTR_DEFAULT_LEN = 512;

    /**
     * Enables use of the LibUSB implementation of the HID API library when
     * running on a Linux platform.
     * <p>
     * The default is raw HID, which enables Bluetooth devices but requires
     * udev rules.
     */
    public static boolean useLibUsbVariantOnLinux = false;

    /**
     * Determines if all devices should be opened in exclusive mode when
     * using the Darwin implementation of the HID API.
     *
     * @see DarwinHidApiLibrary#hid_darwin_set_open_exclusive(int)
     */
    public static boolean darwinOpenDevicesNonExclusive = false;

    /**
     * Enables HID traffic logging assist debugging. This will show all
     * bytes (including the report ID) that were are or received via HID
     * API buffers.
     * <p>
     * This may present a security issue if left enabled in production,
     * although a packet sniffer would see the same data.
     */
    public static boolean logTraffic = false;

    private static final @NotNull Lock
            HID_API_LOCK = new ReentrantLock();

    private static @Nullable HidApiLibrary hidApi;

    /**
     * Open the first HID device with the given vendor ID, product ID and
     * optionally serial number.
     *
     * @param vendorId     The vendor ID.
     * @param productId    The product ID.
     * @param serialNumber The serial number, {@code null} for wildcard.
     * @return The device structure, or {@code null} if not found.
     * @throws IllegalArgumentException If the vendor ID or product ID do
     *                                  not fit inside an unsigned short.
     * @throws IllegalStateException    If the HID API is not initialized.
     */
    public static @Nullable HidDeviceStructure open(
            @Range(from = 0x0000, to = 0xFFFF) int vendorId,
            @Range(from = 0x0000, to = 0xFFFF) int productId,
            @Nullable String serialNumber) {
        requireUnsignedShort("vendorId", vendorId);
        requireUnsignedShort("productId", productId);

        Pointer ptr;
        HID_API_LOCK.lock();
        try {
            HidApiLibrary hidApi = requireInit();
            ptr = hidApi.hid_open(
                    (short) vendorId,
                    (short) productId,
                    serialNumber == null ? null : new WString(serialNumber)
            );
        } finally {
            HID_API_LOCK.unlock();
        }

        if (ptr == null) {
            return null;
        }

        return new HidDeviceStructure(ptr);
    }

    /**
     * Initialises the HID API library. This must be called before any
     * other API calls.
     * <p>
     * If the library is currently initialized, it will be re-initialized
     * with the current parameters.
     */
    public static void init() {
        HID_API_LOCK.lock();
        try {
            exit();

            if (useLibUsbVariantOnLinux && Platform.isLinux()) {
                hidApi = LibUsbHidApiLibrary.INSTANCE;
            } else if (Platform.isMac()) {
                hidApi = DarwinHidApiLibrary.INSTANCE;
            } else {
                hidApi = HidRawHidApiLibrary.INSTANCE;
            }

            hidApi.hid_init();

            if (hidApi instanceof DarwinHidApiLibrary) {
                DarwinHidApiLibrary darwin = (DarwinHidApiLibrary) hidApi;
                int nonExclusive = darwinOpenDevicesNonExclusive ? 0 : 1;
                darwin.hid_darwin_set_open_exclusive(nonExclusive);
            }
        } finally {
            HID_API_LOCK.unlock();
        }
    }

    /**
     * De-initializes the HID API library.
     */
    public static void exit() {
        HID_API_LOCK.lock();
        try {
            if (hidApi != null) {
                hidApi.hid_exit();
                hidApi = null;
            }
        } finally {
            HID_API_LOCK.unlock();
        }
    }

    /**
     * Opens an HID device by its path name.
     *
     * @param path The device path (e.g. {@code "0003:0002:00"})
     * @return The device, or {@code null} if not found.
     * @throws NullPointerException  If {@code path} is {@code null}.
     * @throws IllegalStateException If the HID API is not initialized.
     */
    public static @Nullable HidDeviceStructure open(
            @NotNull String path) {
        Objects.requireNonNull(path, "path cannot be null");

        Pointer ptr;
        HID_API_LOCK.lock();
        try {
            HidApiLibrary hidApi = requireInit();
            ptr = hidApi.hid_open_path(path);
        } finally {
            HID_API_LOCK.unlock();
        }

        if (ptr == null) {
            return null;
        }

        return new HidDeviceStructure(ptr);
    }

    /**
     * Closes an HID device.
     *
     * @param device The device to close.
     * @throws IllegalStateException If the HID API is not initialized.
     */
    public static void close(@Nullable HidDeviceStructure device) {
        if (device == null) {
            return; /* don't bother with obtaining a lock */
        }

        HID_API_LOCK.lock();
        try {
            HidApiLibrary hidApi = requireInit();
            hidApi.hid_close(device.ptr);
        } finally {
            HID_API_LOCK.unlock();
        }
    }

    /**
     * Enumerates the attached HID devices.
     * <p>
     * You can iterate through each device like so:
     * <pre>
     * HidDeviceInfoStructure deviceInfo =
     *     HidApi.enumerateDevices(vendorId, productId);
     * while(deviceInfo != null) {
     *     &sol;* process device info *&sol;
     *     deviceInfo = deviceInfo.next;
     * }
     * </pre>
     *
     * @param vendorId  The vendor ID, {@code 0} for wildcard.
     * @param productId The product ID, {@code 0} for wildcard.
     * @return The info of the first matching device, {@code null} if no
     * devices were found.
     * @throws IllegalArgumentException If the vendor ID or product ID do
     *                                  not fit inside an unsigned short.
     * @throws IllegalStateException    If the HID API is not initialized.
     */
    public static @Nullable HidDeviceInfoStructure enumerateDevices(
            @Range(from = 0x0000, to = 0xFFFF) int vendorId,
            @Range(from = 0x0000, to = 0xFFFF) int productId) {
        requireUnsignedShort("vendorId", vendorId);
        requireUnsignedShort("productId", productId);

        HID_API_LOCK.lock();
        try {
            HidApiLibrary hidApi = requireInit();
            return hidApi.hid_enumerate(
                    (short) vendorId, (short) productId);
        } finally {
            HID_API_LOCK.unlock();
        }
    }

    /**
     * Frees an enumerable HID device list.
     *
     * @param list The list to free.
     */
    public static void freeEnumeration(
            @Nullable HidDeviceInfoStructure list) {
        if (list == null) {
            return; /* don't bother with obtaining a lock */
        }

        HID_API_LOCK.lock();
        try {
            HidApiLibrary hidApi = requireInit();
            hidApi.hid_free_enumeration(list.getPointer());
        } finally {
            HID_API_LOCK.unlock();
        }
    }

    /**
     * Returns a string describing the last error that occurred for an
     * HID device.
     *
     * @param device The HID device structure.
     * @return The message of the last error that occurred for the device,
     * {@code null} if no errors have occurred.
     * @throws NullPointerException  If {@code device} is {@code null}.
     * @throws IllegalStateException If the HID API is not initialized.
     */
    public static @Nullable String getLastErrorMessage(
            @NotNull HidDeviceStructure device) {
        Objects.requireNonNull(device, "device cannot be null");

        Pointer ptr;

        HID_API_LOCK.lock();
        try {
            HidApiLibrary hidApi = requireInit();
            ptr = hidApi.hid_error(device.ptr);
        } finally {
            HID_API_LOCK.unlock();
        }

        if (ptr == null) {
            return null; /* no error message */
        }

        byte[] strBytes = ptr.getByteArray(0, WSTR_DEFAULT_LEN);
        WideStringBuffer wStr = new WideStringBuffer(strBytes);
        return wStr.toString();
    }

    /**
     * Returns the manufacturer of an HID device.
     *
     * @param device The HID device structure.
     * @return The manufacturer of the device, {@code null} on error.
     * @throws NullPointerException  If {@code device} is {@code null}.
     * @throws IllegalStateException If the HID API is not initialized.
     */
    public static @Nullable String getManufacturer(
            @NotNull HidDeviceStructure device) {
        Objects.requireNonNull(device, "device cannot be null");

        HID_API_LOCK.lock();
        try {
            HidApiLibrary hidApi = requireInit();
            WideStringBuffer wStr =
                    new WideStringBuffer(WSTR_DEFAULT_LEN);
            int result = hidApi.hid_get_manufacturer_string(
                    device.ptr, wStr, WSTR_DEFAULT_LEN);
            if (result == -1) {
                return null; /* error occurred, no data */
            }
            return wStr.toString();
        } finally {
            HID_API_LOCK.unlock();
        }
    }

    /**
     * Returns the product ID of an HID device.
     *
     * @param device The HID device structure.
     * @return The product ID of the device, {@code null} on error.
     * @throws NullPointerException  If {@code device} is {@code null}.
     * @throws IllegalStateException If the HID API is not initialized.
     */
    public static @Nullable String getProductId(
            @NotNull HidDeviceStructure device) {
        Objects.requireNonNull(device, "device cannot be null");

        HID_API_LOCK.lock();
        try {
            HidApiLibrary hidApi = requireInit();
            WideStringBuffer wStr =
                    new WideStringBuffer(WSTR_DEFAULT_LEN);
            int result = hidApi.hid_get_product_string(
                    device.ptr, wStr, WSTR_DEFAULT_LEN);
            if (result == -1) {
                return null; /* error occurred, no data */
            }
            return wStr.toString();
        } finally {
            HID_API_LOCK.unlock();
        }
    }

    /**
     * Returns the serial number of an HID device.
     *
     * @param device The HID device structure.
     * @return The serial number of the device, {@code null} on error.
     * @throws NullPointerException  If {@code device} is {@code null}.
     * @throws IllegalStateException If the HID API is not initialized.
     */
    public static @Nullable String getSerialNumber(
            @NotNull HidDeviceStructure device) {
        Objects.requireNonNull(device, "device cannot be null");

        HID_API_LOCK.lock();
        try {
            HidApiLibrary hidApi = requireInit();
            WideStringBuffer wStr =
                    new WideStringBuffer(WSTR_DEFAULT_LEN);
            int result = hidApi.hid_get_serial_number_string(
                    device.ptr, wStr, WSTR_DEFAULT_LEN);
            if (result == -1) {
                return null; /* error occurred, no data */
            }
            return wStr.toString();
        } finally {
            HID_API_LOCK.unlock();
        }
    }

    /**
     * Sets an HID device to be non-blocking.
     * <p>
     * In non-blocking mode, calls to {@code hid_read()} will immediately
     * return with a value of zero if there is no data to be read. In
     * blocking mode, {@code hid_read()} will block the current thread
     * until there is data to read before returning the number of bytes
     * read.
     * <p>
     * Non-blocking I/O can be turned on and off at any time.
     *
     * @param device      The HID device structure.
     * @param nonBlocking {@code true} to enable non-blocking,
     *                    {@code false} to disable non-blocking.
     * @return {@code 0} on success, {@code -1} on error.
     * @throws NullPointerException If {@code device} is {@code null}.
     */
    @Range(from = -1, to = Integer.MAX_VALUE)
    public static int setNonBlocking(
            @NotNull HidDeviceStructure device,
            boolean nonBlocking) {
        Objects.requireNonNull(device, "device cannot be null");

        HID_API_LOCK.lock();
        try {
            HidApiLibrary hidApi = requireInit();
            return hidApi.hid_set_nonblocking(
                    device.ptr, nonBlocking ? 1 : 0);
        } finally {
            HID_API_LOCK.unlock();
        }
    }

    /**
     * Reads an input report from an HID device.
     * <p>
     * Input reports are returned to the host through the INTERRUPT
     * IN endpoint. The first byte will contain the report number if
     * the device uses numbered reports.
     *
     * @param device The device handle.
     * @param buffer A buffer to write the read data into.
     * @return The number of bytes read, {@code -1} on error. If there is
     * no data to be read and the handle is in non-blocking mode, {@code 0}
     * is returned immediately.
     * @throws NullPointerException If {@code device} or {@code buffer}
     *                              are {@code null}.
     */
    @Range(from = -1, to = Integer.MAX_VALUE)
    public static int read(
            @NotNull HidDeviceStructure device,
            byte @NotNull [] buffer) {
        Objects.requireNonNull(device, "device cannot be null");
        Objects.requireNonNull(buffer, "buffer cannot be null");

        HID_API_LOCK.lock();
        try {
            HidApiLibrary hidApi = requireInit();

            WideStringBuffer wStr = new WideStringBuffer(buffer);
            int bytesRead = hidApi.hid_read(
                    device.ptr, wStr, buffer.length);
            logTraffic(wStr, false);

            return bytesRead;
        } finally {
            HID_API_LOCK.unlock();
        }
    }

    @Range(from = -1, to = Integer.MAX_VALUE)
    private static int read(
            @NotNull HidApiLibrary hidApi,
            @NotNull HidDeviceStructure device,
            byte @NotNull [] buffer,
            @Range(from = -1L, to = Integer.MAX_VALUE) int timeoutMs) {
        WideStringBuffer wStr = new WideStringBuffer(buffer);
        int bytesRead = hidApi.hid_read_timeout(device.ptr,
                wStr, buffer.length, timeoutMs);
        logTraffic(wStr, false);
        return bytesRead;
    }

    /**
     * Reads an input report from an HID device with a timeout.
     * <p>
     * Input reports are returned to the host through the INTERRUPT
     * IN endpoint. The first byte will contain the report number if
     * the device uses numbered reports.
     *
     * @param device    The device handle.
     * @param buffer    A buffer to write the read data into.
     * @param timeoutMs The timeout in milliseconds, or {@code -1} to
     *                  wait indefinitely.
     * @return The number of bytes read, {@code -1} on error. If there is
     * no data to be read within the timeout, {@code 0} is returned.
     * @throws NullPointerException If {@code device} or {@code buffer}
     *                              are {@code null}.
     */
    @Range(from = -1, to = Integer.MAX_VALUE)
    public static int read(
            @NotNull HidDeviceStructure device,
            byte @NotNull [] buffer,
            @Range(from = -1L, to = Long.MAX_VALUE) long timeoutMs) {
        Objects.requireNonNull(device, "device cannot be null");
        Objects.requireNonNull(buffer, "buffer cannot be null");

        HID_API_LOCK.lock();
        try {
            HidApiLibrary hidApi = requireInit();

            /* wait indefinitely or no extra logic needed */
            if (timeoutMs <= Integer.MAX_VALUE) {
                return read(hidApi, device, buffer, (int) timeoutMs);
            }

            long remainingTimeoutMs = timeoutMs;
            while (remainingTimeoutMs > 0) {
                int timeoutMsChunk = (int) Math.min(
                        Integer.MAX_VALUE, remainingTimeoutMs);

                int bytesRead = read(
                        hidApi, device, buffer, timeoutMsChunk);
                if (bytesRead < 0) {
                    return bytesRead; /* error occurred */
                } else if (bytesRead > 0) {
                    return bytesRead; /* data received */
                }

                remainingTimeoutMs -= timeoutMsChunk;
            }

            return 0; /* no data received within timeout */
        } finally {
            HID_API_LOCK.unlock();
        }
    }

    /**
     * Gets a feature report from an HID device.
     *
     * @param device   The device handle.
     * @param buffer   A buffer to write the data into.
     * @param reportId The ID of the report to read.
     * @return The number of bytes read, {@code -1} on error.
     * @throws NullPointerException If {@code device} or {@code buffer}
     *                              are {@code null}.
     */
    @Range(from = -1, to = Integer.MAX_VALUE)
    public static int getFeatureReport(
            @NotNull HidDeviceStructure device,
            byte @NotNull [] buffer,
            byte reportId) {
        Objects.requireNonNull(device, "device cannot be null");
        Objects.requireNonNull(buffer, "buffer cannot be null");

        HID_API_LOCK.lock();
        try {
            HidApiLibrary hidApi = requireInit();

            WideStringBuffer report = new WideStringBuffer(
                    buffer.length + 1);
            report.buffer[0] = reportId;
            int bytesRead = hidApi.hid_get_feature_report(
                    device.ptr, report, buffer.length + 1);
            if (bytesRead == -1) {
                return bytesRead; /* error occurred, no data */
            }

            System.arraycopy(report.buffer, 1, buffer, 0, buffer.length);
            logTraffic(report, false);

            return bytesRead;
        } finally {
            HID_API_LOCK.unlock();
        }

    }

    /**
     * Sends a feature report to an HID device.
     * <p>
     * Feature reports are sent over the control endpoint as a
     * {@code set_report} transfer.
     *
     * @param device   The device handle.
     * @param data     The data to send.
     * @param reportId The ID of the report to send.
     * @return The number of bytes written, {@code -1} on error.
     * @throws NullPointerException If {@code device} or {@code data}
     *                              are {@code null}.
     */
    @Range(from = -1, to = Integer.MAX_VALUE)
    public static int sendFeatureReport(
            @NotNull HidDeviceStructure device,
            byte @NotNull [] data,
            byte reportId) {
        Objects.requireNonNull(device, "device cannot be null");
        Objects.requireNonNull(data, "data cannot be null");

        HID_API_LOCK.lock();
        try {
            HidApiLibrary hidApi = requireInit();

            WideStringBuffer report = new WideStringBuffer(data.length + 1);
            report.buffer[0] = reportId;
            System.arraycopy(data, 0, report.buffer, 1, data.length);

            logTraffic(report, true);

            return hidApi.hid_send_feature_report(
                    device.ptr, report, report.buffer.length);
        } finally {
            HID_API_LOCK.unlock();
        }
    }

    /**
     * Writes an output report to an HID device.
     * <p>
     * This function will send the data to the first OUT endpoint, if one
     * exists. If it does not, it will send the data through the control
     * endpoint (endpoint 0).
     *
     * @param device   The device handle.
     * @param data     The data to send.
     * @param length   The number of bytes to send.
     * @param reportId The ID of the report to send. For devices that
     *                 only support a single report, use {@code 0x00}.
     * @return The number of bytes written, {@code -1} on error.
     * @throws NullPointerException If {@code device} or {@code data}
     *                              are {@code null}.
     */
    @Range(from = -1, to = Integer.MAX_VALUE)
    public static int write(
            @NotNull HidDeviceStructure device,
            byte @NotNull [] data,
            @Range(from = 0, to = Integer.MAX_VALUE) int length,
            byte reportId) {
        Objects.requireNonNull(device, "device cannot be null");
        Objects.requireNonNull(data, "data cannot be null");
        if (length >= data.length) {
            String message = "length out of bounds for data";
            throw new IllegalArgumentException(message);
        }

        HID_API_LOCK.lock();
        try {
            HidApiLibrary hidApi = requireInit();

            WideStringBuffer report = new WideStringBuffer(length + 1);
            report.buffer[0] = reportId;
            System.arraycopy(data, 0, report.buffer, 1, length);

            logTraffic(report, true);

            return hidApi.hid_write(
                    device.ptr, report, report.buffer.length);
        } finally {
            HID_API_LOCK.unlock();
        }
    }

    /**
     * Gets an indexed string from an HID device.
     *
     * @param device The device handle.
     * @param index  The index of the string to get.
     * @return The string at the given index, {@code null} on error.
     */
    public static @Nullable String getIndexedString(
            @Nullable HidDeviceStructure device,
            @Range(from = 0, to = Integer.MAX_VALUE) int index) {
        if (device == null) {
            // TODO: error message here?
            return null; /* don't bother with obtaining a lock */
        }

        HID_API_LOCK.lock();
        try {
            HidApiLibrary hidApi = requireInit();

            WideStringBuffer wStr = new WideStringBuffer(WSTR_DEFAULT_LEN);
            int result = hidApi.hid_get_indexed_string(
                    device.ptr, index, wStr, WSTR_DEFAULT_LEN);
            if (result == -1) {
                return null; /* error occurred, no data */
            }

            return wStr.toString();
        } finally {
            HID_API_LOCK.unlock();
        }
    }

    /**
     * Gets the report descriptor from an HID device.
     *
     * @param device The device handle.
     * @param buffer A buffer to write the data into.
     * @param length The buffer length in multiples of {@code wchar_t}.
     * @return {@code 0} on success, {@code -1} on error.
     * @throws NullPointerException If {@code device} or {@code buffer}
     *                              are {@code null}.
     */
    @Range(from = -1, to = Integer.MAX_VALUE)
    public static int getReportDescriptor(
            @NotNull HidDeviceStructure device,
            byte @NotNull [] buffer,
            @Range(from = 0, to = Integer.MAX_VALUE) int length) {
        Objects.requireNonNull(device, "device cannot be null");
        Objects.requireNonNull(buffer, "buffer cannot be null");
        if (length >= buffer.length) {
            String message = "length out of bounds for buffer";
            throw new IllegalArgumentException(message);
        }

        HID_API_LOCK.lock();
        try {
            HidApiLibrary hidApi = requireInit();
            return hidApi.hid_get_report_descriptor(
                    device.ptr, buffer, length);
        } finally {
            HID_API_LOCK.unlock();
        }
    }

    /**
     * Returns the current version of the HID API library.
     *
     * @return The current version in "major.minor.patch" format.
     * @throws IllegalStateException If the HID API is not initialized.
     */
    public static @NotNull String getVersion() {
        HID_API_LOCK.lock();
        try {
            HidApiLibrary hidApi = requireInit();
            return hidApi.hid_version_str();
        } finally {
            HID_API_LOCK.unlock();
        }
    }

    private static @NotNull HidApiLibrary requireInit() {
        HID_API_LOCK.lock();
        try {
            if (hidApi == null) {
                String message = "HID API must be initialized";
                throw new IllegalStateException(message);
            }
            return hidApi;
        } finally {
            HID_API_LOCK.unlock();
        }
    }

    private static void requireUnsignedShort(String name, int value) {
        if (value < 0x0000 || value > 0xFFFF) {
            String message = name + " must fit inside an unsigned short";
            throw new IllegalArgumentException(message);
        }
    }

    private static void logTraffic(
            @Nullable WideStringBuffer buffer,
            boolean isWrite) {
        if (!HidApi.logTraffic) {
            return; /* don't bother with obtaining a lock */
        } else if (buffer == null || buffer.size() <= 0) {
            return; /* don't bother with obtaining a lock */
        }

        /* TODO: use proper logging API */

        System.out.print(isWrite ? ">" : "<");
        System.out.printf(" [%d bytes]:", buffer.buffer.length);
        for (int i = 0; i < buffer.buffer.length; i++) {
            System.out.printf(" %02x", buffer.buffer[i]);
        }
        System.out.println();
    }

}
