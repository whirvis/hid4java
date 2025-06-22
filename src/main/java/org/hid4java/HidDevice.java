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

import org.hid4java.jna.HidApi;
import org.hid4java.jna.HidDeviceInfoStructure;
import org.hid4java.jna.HidDeviceStructure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Represents an HID device.
 *
 * @since 0.0.1
 */
@SuppressWarnings("unused")
public class HidDevice implements Closeable {

    /* TODO: add locks for getters, setters, read, write, etc. */

    private static final int INPUT_REPORT_LENGTH = 64;

    private final @NotNull HidDeviceManager manager;
    private @Nullable HidDeviceStructure device;

    private final @NotNull String path;
    private final int vendorId;
    private final int productId;
    private @Nullable String serialNumber;
    private final int releaseNumber;
    private @Nullable String manufacturer;
    private @Nullable String product;
    private final int usagePage;
    private final int usage;
    private final int interfaceNumber;

    private final boolean autoDataRead;
    private final long dataReadIntervalMs;

    /*
     * We use a Thread instead of Executor here, since it may be stopped,
     * paused, and/or restarted frequently. Executors are more heavyweight
     * in this regard.
     */
    private @Nullable Thread dataReadThread;

    HidDevice(@NotNull HidDeviceInfoStructure info,
              @NotNull HidDeviceManager manager,
              @NotNull HidServicesSpecification specs) {
        this.manager = manager;
        this.device = null;

        /*
         * Cache these, because the structure this information comes from
         * will inevitably be freed. If we try to read the information from
         * it then, it could cause the whole JVM to crash!
         */
        this.path = info.path;
        this.vendorId = info.vendor_id & 0xFFFF;
        this.productId = info.product_id & 0xFFFF;
        if (info.serial_number != null) {
            this.serialNumber = info.serial_number.toString();
        }
        this.releaseNumber = info.release_number;
        if (info.manufacturer_string != null) {
            this.manufacturer = info.manufacturer_string.toString();
        }
        if (info.product_string != null) {
            this.product = info.product_string.toString();
        }
        this.usagePage = info.usage_page;
        this.usage = info.usage;
        this.interfaceNumber = info.interface_number;

        this.autoDataRead = specs.isAutoDataRead();
        this.dataReadIntervalMs = specs.getDataReadIntervalMs();
        this.dataReadThread = null;
    }

    /**
     * Returns the device path.
     *
     * @return The device path.
     * @since 0.1.0
     */
    public @NotNull String getPath() {
        return this.path;
    }

    /**
     * Returns the vendor ID.
     *
     * @return The vendor ID.
     * @since 0.1.0
     */
    public int getVendorId() {
        return this.vendorId;
    }

    /**
     * Returns the product ID.
     *
     * @return The product ID.
     * @since 0.1.0
     */
    public int getProductId() {
        return this.productId;
    }

    /**
     * Returns the device's serial number.
     *
     * @return The device's serial number.
     * @since 0.1.0
     */
    public @Nullable String getSerialNumber() {
        return this.serialNumber;
    }

    /**
     * Returns the device's release number.
     *
     * @return The device's release number.
     * @since 0.1.0
     */
    public int getReleaseNumber() {
        return this.releaseNumber;
    }

    /**
     * Returns the device manufacturer.
     *
     * @return The device manufacturer.
     * @since 0.1.0
     */
    public @Nullable String getManufacturer() {
        return this.manufacturer;
    }

    /**
     * Return the device product.
     *
     * @return The device product.
     * @since 0.1.0
     */
    public @Nullable String getProduct() {
        return this.product;
    }

    /**
     * Returns the usage page.
     *
     * @return The usage page.
     * @since 0.1.0
     */
    public int getUsagePage() {
        return this.usagePage;
    }

    /**
     * Returns the usage information.
     *
     * @return The usage information.
     * @since 0.1.0
     */
    public int getUsage() {
        return this.usage;
    }

    /**
     * Returns the interface number.
     *
     * @return The interface number.
     * @since 0.1.0
     */
    public int getInterfaceNumber() {
        return this.interfaceNumber;
    }

    /* TODO: use locks instead of synchronized */
    private synchronized void startDataReadThread() {
        if (dataReadThread != null) {
            return; /* thread is already running */
        }

        /* TODO: Won't the thread do this? */
        this.dataRead();

        Thread dataReadThread = new DataReadThread(this);
        dataReadThread.setDaemon(true);
        dataReadThread.setName("hid4java data reader");
        dataReadThread.start();

        this.dataReadThread = dataReadThread;
    }

    /* TODO: use locks instead of synchronized */
    private synchronized void stopDataReadThread() {
        if (dataReadThread != null) {
            dataReadThread.interrupt();
        }
    }

    /* TODO: use locks instead of synchronized */
    private synchronized void dataRead() {
        byte[] data = this.readAll(100);
        manager.afterDeviceDataRead(this, data);
    }

    private void requireOpen() {
        if (!this.isOpen()) {
            throw new IllegalStateException("HID device must be open");
        }
    }

    /**
     * Returns if the device is currently open.
     *
     * @return {@code true} if the device is currently open,
     * {@code false} otherwise.
     * @since 0.1.0
     */
    public boolean isOpen() {
        return this.device != null;
    }

    /**
     * Opens the device and obtains a device structure.
     *
     * @return {@code true} if the device was successfully opened,
     * {@code false} otherwise.
     * @since 0.1.0
     */
    public boolean open() {
        HidDeviceStructure hidDeviceStructure = HidApi.open(path);
        if (hidDeviceStructure == null) {
            return false;
        }

        this.device = hidDeviceStructure;
        if (autoDataRead) {
            this.startDataReadThread();
        }

        return true;
    }

    /**
     * Sets the device to be non-blocking.
     * <p>
     * In non-blocking mode, calls to {@code hid_read()} will immediately
     * return with a value of zero if there is no data to be read. In
     * blocking mode, {@code hid_read()} will block the current thread
     * until there is data to read before returning the number of bytes
     * read.
     * <p>
     * Non-blocking I/O can be turned on and off at any time.
     *
     * @param nonBlocking {@code true} to enable non-blocking,
     *                    {@code false} to disable non-blocking.
     * @return {@code true} on success, {@code false} on failure.
     * @throws IllegalStateException If the device is not open.
     * @since 0.1.0
     */
    public boolean setNonBlocking(boolean nonBlocking) {
        this.requireOpen();
        return HidApi.setNonBlocking(device, nonBlocking);
    }

    /**
     * Reads an input report from the device.
     * <p>
     * Input reports are returned to the host through the INTERRUPT
     * IN endpoint. The first byte will contain the report number if
     * the device uses numbered reports.
     *
     * @param buffer A buffer to write the read data into.
     * @return The number of bytes read, {@code -1} on error. If there is
     * no data to be read and the handle is in non-blocking mode, {@code 0}
     * is returned immediately.
     * @throws NullPointerException  If {@code buffer} is {@code null}.
     * @throws IllegalStateException If the device is not open.
     * @since 0.1.0
     */
    @Range(from = -1L, to = Integer.MAX_VALUE)
    public int read(byte @NotNull [] buffer) {
        Objects.requireNonNull(buffer, "buffer cannot be null");
        this.requireOpen();
        return HidApi.read(device, buffer);
    }

    /**
     * Reads an input report from the device with a timeout.
     * <p>
     * Input reports are returned to the host through the INTERRUPT
     * IN endpoint. The first byte will contain the report number if
     * the device uses numbered reports.
     *
     * @param buffer    A buffer to write the read data into.
     * @param timeoutMs The timeout in milliseconds, or {@code -1} to
     *                  wait indefinitely.
     * @return The number of bytes read, {@code -1} on error. If there is
     * no data to be read within the timeout, {@code 0} is returned.
     * @throws NullPointerException  If {@code buffer} is {@code null}.
     * @throws IllegalStateException If the device is not open.
     * @since 0.1.0
     */
    @Range(from = -1L, to = Integer.MAX_VALUE)
    public int read(
            byte @NotNull [] buffer,
            @Range(from = -1L, to = Long.MAX_VALUE) long timeoutMs) {
        Objects.requireNonNull(buffer, "buffer cannot be null");
        this.requireOpen();
        /* TODO: handle down-casting from long to int */
        return HidApi.read(device, buffer, (int) timeoutMs);
    }

    /**
     * Reads an input report from the device with a timeout.
     * <p>
     * Input reports are returned to the host through the INTERRUPT
     * IN endpoint. The first byte will contain the report number if
     * the device uses numbered reports.
     *
     * @param amountToRead The number of bytes to read.
     * @param timeoutMs    The timeout in milliseconds, or {@code -1} to
     *                     wait indefinitely.
     * @return The data read from the device.
     * @throws IllegalArgumentException If {@code amountToRead} is negative.
     * @throws IllegalStateException    If the device is not open.
     * @since 0.1.0
     */
    public byte @NotNull [] read(
            @Range(from = 0L, to = Integer.MAX_VALUE) int amountToRead,
            @Range(from = -1L, to = Integer.MAX_VALUE) long timeoutMs) {
        this.requireOpen();
        byte[] buffer = new byte[amountToRead];
        /* TODO: handle down-casting from long to int */
        int read = HidApi.read(device, buffer, (int) timeoutMs);
        return shorten(buffer, read);
    }

    /**
     * Reads an input report from the device.
     * <p>
     * Input reports are returned to the host through the INTERRUPT
     * IN endpoint. The first byte will contain the report number if
     * the device uses numbered reports.
     *
     * @param amountToRead The number of bytes to read.
     * @return The data read from the device.
     * @throws IllegalArgumentException If {@code amountToRead} is negative.
     * @throws IllegalStateException    If the device is not open.
     * @since 0.1.0
     */
    public byte @NotNull [] read(
            @Range(from = 0L, to = Integer.MAX_VALUE) int amountToRead) {
        this.requireOpen();
        byte[] buffer = new byte[amountToRead];
        int read = HidApi.read(device, buffer);
        return shorten(buffer, read);
    }

    /**
     * Reads an input report of 64 bytes from the device with a 1000ms
     * timeout.
     * <p>
     * Input reports are returned to the host through the INTERRUPT
     * IN endpoint. The first byte will contain the report number if
     * the device uses numbered reports.
     *
     * @return The data read from the device.
     * @throws IllegalStateException If the device is not open.
     * @since 0.1.0
     */
    public byte @NotNull [] read() {
        /* TODO: this feels like a waste of memory, especially in a loop */
        return this.read(INPUT_REPORT_LENGTH, 1000L);
    }

    /**
     * Reads as many input reports from the device as possible with
     * a timeout.
     * <p>
     * <b>Note:</b> The timeout is used for {@code hid_read()}.
     * The first read call that exceeds the timeout will have the
     * function return.
     *
     * @param timeoutMs The timeout in milliseconds, or {@code -1}
     *                  to wait indefinitely.
     * @return The data read from the device.
     * @throws IllegalStateException If the device is not open.
     * @since 0.8.0
     */
    public byte @NotNull [] readAll(
            @Range(from = -1L, to = Long.MAX_VALUE) long timeoutMs) {
        this.requireOpen();

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] buffer = new byte[INPUT_REPORT_LENGTH];
        while (true) {
            int bytesRead = read(buffer, timeoutMs);
            if (bytesRead == 0) {
                break; /* no more data */
            } else if (bytesRead == -1) {
                /* TODO: handle this! */
                break; /* error occurred */
            }

            try {
                output.write(buffer);
            } catch (IOException e) {
                /* this should never happen */
                throw new RuntimeException(e);
            }
        }

        return output.toByteArray();
    }

    /**
     * Writes an output report to the device.
     * <p>
     * This will send the data to the first OUT endpoint, if one exists.
     * If it does not, it will send the data through the control endpoint
     * (endpoint 0).
     *
     * @param data         The data to send.
     * @param packetLength The number of bytes to send, <b>not including
     *                     the report ID.</b>
     * @param reportId     The report ID. For devices that only support
     *                     a single report, use {@code 0x00}.
     * @param applyPadding {@code true} if padding should be applied to
     *                     the data, {@code false} otherwise.
     * @return The number of bytes written, {@code -1} on error.
     * @throws NullPointerException     If {@code buffer} is {@code null}.
     * @throws IllegalArgumentException If {@code packetLength} is negative.
     * @throws IllegalStateException    If the device is not open.
     * @since 0.8.0
     */
    public int write(
            byte @NotNull [] data,
            @Range(from = 0, to = Integer.MAX_VALUE) int packetLength,
            @Range(from = 0x00, to = 0xFF) byte reportId,
            boolean applyPadding) {
        Objects.requireNonNull(data, "data cannot be null");

        //noinspection ConstantValue
        if (packetLength < 0) {
            String message = "packetLength cannot be negative";
            throw new IllegalArgumentException(message);
        }

        this.requireOpen();

        byte[] packet;
        if (applyPadding) {
            packet = Arrays.copyOf(data, packetLength + 1);
        } else {
            packet = data;
        }

        int result = HidApi.write(device, packet, packetLength, reportId);
        manager.afterDeviceWrite();
        return result;
    }

    /**
     * Writes an output report to the device.
     * <p>
     * This will send the data to the first OUT endpoint, if one exists.
     * If it does not, it will send the data through the control endpoint
     * (endpoint 0).
     *
     * @param data         The data to send.
     * @param packetLength The number of bytes to send, <b>not including
     *                     the report ID.</b>
     * @param reportId     The report ID. For devices that only support
     *                     a single report, use {@code 0x00}.
     * @return The number of bytes written, {@code -1} on error.
     * @throws NullPointerException     If {@code buffer} is {@code null}.
     * @throws IllegalArgumentException If {@code packetLength} is negative.
     * @throws IllegalStateException    If the device is not open.
     * @since 0.1.0
     */
    public int write(
            byte @NotNull [] data,
            @Range(from = 0, to = Integer.MAX_VALUE) int packetLength,
            @Range(from = 0x00, to = 0xFF) byte reportId) {
        return this.write(data, packetLength, reportId, false);
    }

    /**
     * Gets a feature report from the device.
     *
     * @param buffer   A buffer to write the data into.
     * @param reportId The report ID. For devices that only support
     *                 a single report, use {@code 0x00}.
     * @return The number of bytes read, {@code -1} on error.
     * @throws NullPointerException  If {@code buffer} is {@code null}.
     * @throws IllegalStateException If the device is not open.
     * @since 0.1.0
     */
    @Range(from = -1L, to = Integer.MAX_VALUE)
    public int getFeatureReport(
            byte @NotNull [] buffer,
            @Range(from = 0x00, to = 0xFF) byte reportId) {
        Objects.requireNonNull(buffer, "buffer cannot be null");
        this.requireOpen();
        return HidApi.getFeatureReport(device, buffer, reportId);
    }

    /**
     * Sends a feature report to the device.
     * <p>
     * Feature reports are sent over the control endpoint as a
     * {@code set_report} transfer.
     *
     * @param data     The data to send.
     * @param reportId The report ID. For devices that only support
     *                 a single report, use {@code 0x00}.
     * @return The number of bytes written, {@code -1} on error.
     * @throws NullPointerException  If {@code data} is {@code null}.
     * @throws IllegalStateException If the device is not open.
     * @since 0.1.0
     */
    @Range(from = -1L, to = Integer.MAX_VALUE)
    public int sendFeatureReport(
            byte @NotNull [] data,
            @Range(from = 0x00, to = 0xFF) byte reportId) {
        Objects.requireNonNull(data, "data cannot be null");
        this.requireOpen();
        return HidApi.sendFeatureReport(device, data, reportId);
    }

    /**
     * Gets an indexed string from the device.
     *
     * @param index The index of the string to get.
     * @return The string at the requested index, {@code null} if an error
     * occurred or the string does not exist.
     * @throws IllegalStateException If the device is not open.
     * @since 0.1.0
     */
    public @Nullable String getIndexedString(
            @Range(from = 0L, to = Integer.MAX_VALUE) int index) {
        this.requireOpen();
        return HidApi.getIndexedString(device, index);
    }

    /**
     * Gets the report descriptor from the device.
     *
     * @param buffer A buffer to write the data into.
     * @return {@code true} on success, {@code false} on failure.
     * @throws NullPointerException  If {@code buffer} is {@code null}.
     * @throws IllegalStateException If the device is not open.
     * @since HID API 0.14.0
     */
    public boolean getReportDescriptor(byte @NotNull [] buffer) {
        Objects.requireNonNull(buffer, "buffer cannot be null");
        this.requireOpen();
        int result = HidApi.getReportDescriptor(device, buffer, buffer.length);
        return result == 0; /* 0 == success, -1 == failure */
    }

    /**
     * Returns the last error message from the HID API for this device.
     *
     * @return The last error message, {@code null} if none has occurred.
     * @since 0.1.0
     */
    public @Nullable String getLastErrorMessage() {
        return HidApi.getLastErrorMessage(device);
    }

    /**
     * Returns if the device matches a vendor ID, product ID, and
     * serial number.
     *
     * @param vendorId     The vendor ID, {@code 0} for wildcard.
     * @param productId    The product ID, {@code 0} for wildcard.
     * @param serialNumber The serial number, {@code null} for wildcard.
     * @return {@code true} if the device matches the given vendor ID,
     * product ID, and serial number, {@code false} otherwise.
     * @since 0.1.0
     */
    public boolean matches(
            @Range(from = 0x0000, to = 0xFFFF) int vendorId,
            @Range(from = 0x0000, to = 0xFFFF) int productId,
            @Nullable String serialNumber) {
        return (vendorId == 0 || vendorId == this.vendorId)
                && (productId == 0 || productId == this.productId)
                && (serialNumber == null || serialNumber.equals(this.serialNumber));
    }

    /**
     * Returns if the device is currently closed.
     *
     * @return {@code true} if the device is currently closed,
     * {@code false} otherwise.
     * @since 0.8.0
     */
    public boolean isClosed() {
        return this.device == null;
    }

    @Override
    public void close() {
        if (this.isClosed()) {
            return; /* nothing to do */
        }

        this.stopDataReadThread();
        HidApi.close(device);
        this.device = null;
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

        HidDevice that = (HidDevice) obj;
        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public @NotNull String toString() {
        return this.getClass().getSimpleName() + " ["
                + String.format("path=%s,", path)
                + String.format("vendorId=0x%04x,", vendorId)
                + String.format("productId=0x%04x,", productId)
                + String.format("serialNumber=%s,", serialNumber)
                + String.format("releaseNumber=%d,", releaseNumber)
                + String.format("manufacturer=%s,", manufacturer)
                + String.format("product=%s,", product)
                + String.format("usagePage=%d,", usagePage)
                + String.format("usage=%d,", usage)
                + String.format("interfaceNumber=%d,", interfaceNumber)
                + "]";
    }

    private static byte @NotNull [] shorten(
            byte @NotNull [] arr,
            @Range(from = 0, to = Integer.MAX_VALUE) int length) {
        byte[] dataRead = new byte[length];
        System.arraycopy(arr, 0, dataRead, 0, length);
        return dataRead;
    }

    private static class DataReadThread extends Thread {

        private final @NotNull HidDevice device;

        private DataReadThread(@NotNull HidDevice device) {
            this.device = device;
        }

        @SuppressWarnings("BusyWait")
        @Override
        public void run() {
            while (!this.isInterrupted()) {
                try {
                    device.dataRead();
                    Thread.sleep(device.dataReadIntervalMs);
                } catch (InterruptedException e) {
                    this.interrupt();
                }
            }
        }

    }

}
