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

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import org.hid4java.HidDevice;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

/**
 * JNA proxy to the HID API library.
 *
 * @since 0.1.0
 */
public interface HidApiLibrary extends Library {

    /**
     * Initializes the HID API library.
     * <p>
     * Calling this function is not strictly necessary. It will be called
     * by {@code hid_enumerate()} or {@code hid_open_*()} if needed.
     * <p>
     * However, this function should be called at the start of execution
     * if any HID API handles could be opened by different threads.
     */
    void hid_init();

    /**
     * Closes the HID API library.
     * <p>
     * This function frees all static data associated with HID API. It
     * should be called at the end of execution to avoid memory leaks.
     */
    void hid_exit();

    /**
     * Opens the first HID device with the given vendor ID, product ID, and
     * serial number. If no serial number is specified, the first device with
     * the specified vendor ID and product ID is opened.
     *
     * @param vendor_id     The vendor ID.
     * @param product_id    The product ID.
     * @param serial_number The serial number, {@code null} for any.
     * @return A pointer to an {@link HidDevice} on success, {@code null}
     * on failure.
     */
    @Nullable Pointer hid_open(
            @Range(from = 0x0000, to = 0xFFFF) short vendor_id,
            @Range(from = 0x0000, to = 0xFFFF) short product_id,
            @Nullable WString serial_number
    );

    /**
     * Closes an HID device.
     *
     * @param device The device handle.
     */
    void hid_close(@NotNull Pointer device);

    /**
     * Returns a pointer to a string describing the last error
     * which occurred for a device.
     *
     * @param device The device handle.
     * @return A pointer to a string containing the last error,
     * {@code null} if none has occurred.
     */
    @Nullable Pointer hid_error(@NotNull Pointer device);

    /**
     * Reads an input report from an HID device.
     * <p>
     * Input reports are returned to the host through the INTERRUPT
     * IN endpoint. The first byte will contain the report number if
     * the device uses numbered reports.
     *
     * @param device The device handle.
     * @param buffer A buffer to write the read data into.
     * @param length The number of bytes to read. <b>For devices with
     *               multiple reports, make sure to read an extra byte
     *               for the report number.</b>
     * @return The number of bytes read, {@code -1} on error. If there is
     * no data to be read and the handle is in non-blocking mode, {@code 0}
     * is returned immediately.
     */
    @Range(from = -1L, to = Integer.MAX_VALUE)
    int hid_read(
            @NotNull Pointer device,
            @NotNull WideStringBuffer.ByReference buffer,
            @Range(from = 0L, to = Integer.MAX_VALUE) int length
    );

    /**
     * Reads an input report from an HID device with a timeout.
     * <p>
     * Input reports are returned to the host through the INTERRUPT
     * IN endpoint. The first byte will contain the report number if
     * the device uses numbered reports.
     *
     * @param device  The device handle.
     * @param buffer  A buffer to write the read data into.
     * @param length  The number of bytes to read. <b>For devices with
     *                multiple reports, make sure to read an extra byte
     *                for the report number.</b>
     * @param timeout The timeout in milliseconds, or {@code -1} to
     *                wait indefinitely.
     * @return The number of bytes read, {@code -1} on error. If there is
     * no data to be read within the timeout, {@code 0} is returned.
     */
    @Range(from = -1L, to = Integer.MAX_VALUE)
    int hid_read_timeout(
            @NotNull Pointer device,
            @NotNull WideStringBuffer.ByReference buffer,
            @Range(from = 0L, to = Integer.MAX_VALUE) int length,
            @Range(from = -1L, to = Integer.MAX_VALUE) int timeout
    );

    /**
     * Writes an output report to an HID device.
     * <p>
     * The first byte of data must contain the report ID. For devices that
     * only support a single report, use {@code 0x00}. The remaining bytes
     * should contain the actual report data.
     * <p>
     * This function will send the data to the first OUT endpoint, if one
     * exists. If it does not, it will send the data through the control
     * endpoint (endpoint 0).
     *
     * @param device The device handle.
     * @param data   The data to send.
     * @param length The number of bytes to send,
     *               <b>including the report ID.</b>
     * @return The number of bytes written, {@code -1} on error.
     */
    @Range(from = -1L, to = Integer.MAX_VALUE)
    int hid_write(
            @NotNull Pointer device,
            @NotNull WideStringBuffer.ByReference data,
            @Range(from = 0L, to = Integer.MAX_VALUE) int length
    );

    /**
     * Gets a feature report from an HID device.
     * <p>
     * <b>Note:</b> Set the first byte to the ID of the report to read.
     * Upon return, the first byte will still contain the report ID, and
     * the report data will start at {@code data[1]}.
     *
     * @param device The device handle.
     * @param buffer A buffer to write the data into.
     * @param length The number of bytes to read,
     *               <b>including the report ID.</b>
     * @return The number of bytes read, {@code -1} on error.
     */
    @Range(from = -1L, to = Integer.MAX_VALUE)
    int hid_get_feature_report(
            @NotNull Pointer device,
            @NotNull WideStringBuffer.ByReference buffer,
            @Range(from = 0L, to = Integer.MAX_VALUE) int length
    );

    /**
     * Sends a feature report to an HID device.
     * <p>
     * Feature reports are sent over the control endpoint as a
     * {@code set_report} transfer.
     * <p>
     * The first byte of data must contain the report ID. For devices that
     * only support a single report, use {@code 0x00}. The remaining bytes
     * should contain the actual report data.
     *
     * @param device The device handle.
     * @param data   The data to send.
     * @param length The number of bytes to send,
     *               <b>including the report ID.</b>
     * @return The number of bytes written, {@code -1} on error.
     */
    @Range(from = -1L, to = Integer.MAX_VALUE)
    int hid_send_feature_report(
            @NotNull Pointer device,
            @NotNull WideStringBuffer.ByReference data,
            @Range(from = 0L, to = Integer.MAX_VALUE) int length
    );

    /**
     * Gets an indexed string from an HID device.
     *
     * @param device The device handle.
     * @param index  The index of the string to get.
     * @param str    A wide string buffer to write the data into.
     * @param length The buffer length in multiples of {@code wchar_t}.
     * @return {@code 0} on success, {@code -1} on error.
     */
    @Range(from = -1L, to = 0L)
    int hid_get_indexed_string(
            @NotNull Pointer device,
            @Range(from = 0L, to = Integer.MAX_VALUE) int index,
            @NotNull WideStringBuffer.ByReference str,
            @Range(from = 0L, to = Integer.MAX_VALUE) int length
    );

    /**
     * Gets the report descriptor from an HID device.
     *
     * @param device The device handle.
     * @param buffer A buffer to write the data into.
     * @param length The buffer length in multiples of {@code wchar_t}.
     * @return {@code 0} on success, {@code -1} on error.
     */
    @Range(from = -1L, to = 0L)
    int hid_get_report_descriptor(
            @NotNull Pointer device,
            byte @NotNull [] buffer,
            @Range(from = 0L, to = Integer.MAX_VALUE) int length
    );

    /**
     * Gets the manufacturer string from an HID device.
     *
     * @param device The device handle.
     * @param str    A wide string buffer to write the data into.
     * @param length The buffer length in multiples of {@code wchar_t}.
     * @return {@code 0} on success, {@code -1} on error.
     */
    @Range(from = -1L, to = 0L)
    int hid_get_manufacturer_string(
            @NotNull Pointer device,
            @NotNull WideStringBuffer.ByReference str,
            @Range(from = 0L, to = Integer.MAX_VALUE) int length
    );

    /**
     * Gets the product number string from an HID device.
     *
     * @param device The device handle.
     * @param str    A wide string buffer to write the data into.
     * @param length The buffer length in multiples of {@code wchar_t}.
     * @return {@code 0} on success, {@code -1} on error.
     */
    @Range(from = -1L, to = 0L)
    int hid_get_product_string(
            @NotNull Pointer device,
            @NotNull WideStringBuffer.ByReference str,
            @Range(from = 0L, to = Integer.MAX_VALUE) int length
    );

    /**
     * Gets the serial number string from an HID device.
     *
     * @param device The device handle.
     * @param str    A wide string buffer to write the data into.
     * @param length The buffer length in multiples of {@code wchar_t}.
     * @return {@code 0} on success, {@code -1} on error.
     */
    @Range(from = -1L, to = 0L)
    int hid_get_serial_number_string(
            @NotNull Pointer device,
            @NotNull WideStringBuffer.ByReference str,
            @Range(from = 0L, to = Integer.MAX_VALUE) int length
    );

    /**
     * Sets an HID device handle to be non-blocking.
     * <p>
     * In non-blocking mode, calls to {@code hid_read()} will immediately
     * return with a value of zero if there is no data to be read. In
     * blocking mode, {@code hid_read()} will block the current thread
     * until there is data to read before returning the number of bytes
     * read.
     * <p>
     * Non-blocking I/O can be turned on and off at any time.
     *
     * @param device       The device handle.
     * @param non_blocking {@code 0} to disable non-blocking,
     *                     {@code 1} to enable non-blocking.
     * @return {@code 0} on success, {@code -1} on error.
     */
    @Range(from = -1L, to = 0L)
    int hid_set_nonblocking(
            @NotNull Pointer device,
            @Range(from = 0L, to = 1L) int non_blocking
    );

    /**
     * Enumerates the current HID Devices.
     * <p>
     * This function returns a linked list of all HID devices currently
     * attached to the system that match the given vendor ID and product ID.
     * <p>
     * If the vendor ID is zero, then any vendor will match. If the product
     * ID is zero, then any product will match. If both are set to zero, then
     * all HID devices will be returned.
     *
     * @param vendor_id  The vendor ID.
     * @param product_id The product ID.
     * @return A linked list of all discovered matching devices.
     */
    @Nullable HidDeviceInfoStructure hid_enumerate(
            @Range(from = 0x0000, to = 0xFFFF) short vendor_id,
            @Range(from = 0x0000, to = 0xFFFF) short product_id
    );

    /**
     * Frees an enumeration.
     *
     * @param device The device information pointer.
     */
    void hid_free_enumeration(@NotNull Pointer device);

    /**
     * Opens an HID device by its path name.
     * <p>
     * The path name can be determined by calling {@code hid_enumerate()}.
     * A platform specific path name (such as "/dev/hidraw0" on Linux) can
     * also be used.
     *
     * @param path The path name.
     * @return A pointer to the device on success, {@code null}
     * on failure.
     */
    @Nullable Pointer hid_open_path(@NotNull String path);

    /**
     * Returns the current version of the HID API library.
     *
     * @return The current version in "major.minor.patch" format.
     */
    @NotNull String hid_version_str();

}
