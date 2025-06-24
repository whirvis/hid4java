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
import com.sun.jna.WString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Represents an {@code HidDeviceInfo} structure.
 *
 * @since 0.1.0
 */
public class HidDeviceInfoStructure
        extends Structure
        implements Structure.ByReference {

    /**
     * The USB path to the device.
     */
    public String path;

    /**
     * The device's vendor ID.
     */
    public short vendor_id;

    /**
     * The device's product ID.
     */
    public short product_id;

    /**
     * The device's serial number.
     */
    public WString serial_number;

    /**
     * The device's release number.
     */
    public short release_number;

    /**
     * The device's manufacturer string.
     */
    public WString manufacturer_string;

    /**
     * Usage Page for this device/interface (Windows/Mac only).
     */
    public WString product_string;

    /**
     * Usage for this device/interface (Windows/Mac only).
     */
    public short usage_page;

    /**
     * The device's usage number.
     */
    public short usage;

    /**
     * The device's interface number.
     */
    public int interface_number;

    /**
     * Reference to the next device, if any.
     */
    public @Nullable HidDeviceInfoStructure next;

    @Override
    protected @NotNull List<@NotNull String> getFieldOrder() {
        return Arrays.asList(
                "path",
                "vendor_id",
                "product_id",
                "serial_number",
                "release_number",
                "manufacturer_string",
                "product_string",
                "usage_page",
                "usage",
                "interface_number",
                "next"
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                path,
                vendor_id,
                product_id,
                serial_number,
                release_number,
                manufacturer_string,
                product_id,
                usage_page,
                usage,
                interface_number
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true; /* we equal ourselves */
        } else if (obj == null) {
            return false; /* nothing to compare with */
        } else if (obj.getClass() != this.getClass()) {
            return false; /* child must implement */
        }

        HidDeviceInfoStructure that = (HidDeviceInfoStructure) obj;

        return Objects.equals(path, that.path)
                && Objects.equals(vendor_id, that.vendor_id)
                && Objects.equals(product_id, that.product_id)
                && Objects.equals(serial_number, that.serial_number)
                && Objects.equals(release_number, that.release_number)
                && Objects.equals(manufacturer_string, that.manufacturer_string)
                && Objects.equals(product_string, that.product_string)
                && Objects.equals(usage_page, that.usage_page)
                && Objects.equals(usage, that.usage)
                && Objects.equals(interface_number, that.interface_number);
    }

    @Override
    public @NotNull String toString() {
        return "HID device\n" +
                String.format("\tpath: %s\n", path) +
                String.format("\tvendor_id: 0x%04x\n", vendor_id) +
                String.format("\tproduct_id: 0x%04x\n", product_id) +
                String.format("\tserial_number: %s\n", serial_number) +
                String.format("\trelease_number: %d\n", release_number) +
                String.format("\tmanufacturer_string: %s\n", manufacturer_string) +
                String.format("\tproduct_string: %s\n", product_string) +
                String.format("\tusage_page: %d\n", usage_page) +
                String.format("\tusage: %d\n", usage) +
                String.format("\tinterface_number: %d", interface_number);
    }

}
