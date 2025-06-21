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

/**
 * Determines how HID devices will be scanned.
 *
 * @see HidServicesSpecification#setScanMode(ScanMode)
 */
public enum ScanMode {

    /**
     * Equivalent to a scan interval of zero.
     */
    NO_SCAN,

    /**
     * Trigger continuous scan at a given interval.
     */
    SCAN_AT_FIXED_INTERVAL,

    /**
     * Trigger continuous scan at a given interval; but pause after each
     * write operation to give the device time to process data (so it does
     * not need to respond to further enumeration requests).
     * <p>
     * This can be a useful strategy for handling devices with constrained
     * processing power and/or limited USB stacks.
     * <p>
     * <b>Note:</b> This will affect the time to generate a device attach
     * or detach event, since scanning will be paused.
     */
    SCAN_AT_FIXED_INTERVAL_WITH_PAUSE_AFTER_WRITE,

}
