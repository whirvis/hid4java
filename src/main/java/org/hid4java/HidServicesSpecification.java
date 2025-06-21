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

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;

/**
 * Flexible configuration for HID services.
 *
 * @since 0.5.0
 */
public class HidServicesSpecification {

    private @NotNull ScanMode scanMode;
    private @NotNull Duration scanInterval;
    private @NotNull Duration pauseInterval;
    private boolean autoStart;
    private boolean autoShutdown;
    private boolean autoDataRead;
    private @NotNull Duration dataReadInterval;

    /**
     * Constructs a new {@code HidServicesSpecification} with the default
     * parameters.
     */
    public HidServicesSpecification() {
        this.scanMode = ScanMode.SCAN_AT_FIXED_INTERVAL;
        this.scanInterval = Duration.ofMillis(500L);
        this.pauseInterval = Duration.ofMillis(5000L);
        this.autoStart = true;
        this.autoShutdown = true;
        this.autoDataRead = false;
        this.dataReadInterval = Duration.ofMillis(500L);
    }

    /**
     * Returns the scan mode.
     *
     * @return The scan mode.
     */
    public @NotNull ScanMode getScanMode() {
        return this.scanMode;
    }

    /**
     * Sets the scan mode.
     *
     * @param scanMode The scan mode to use.
     * @return This configuration object.
     * @throws NullPointerException If {@code scanMode} is {@code null}.
     */
    public @NotNull HidServicesSpecification setScanMode(
            @NotNull ScanMode scanMode) {
        Objects.requireNonNull(scanMode, "scanMode cannot be null");
        this.scanMode = scanMode;
        return this;
    }

    /**
     * Returns the scan interval between device enumerations.
     *
     * @return The scan interval.
     */
    public @NotNull Duration getScanInterval() {
        return this.scanInterval;
    }

    /**
     * Returns the scan interval between device enumerations.
     *
     * @return The scan interval in milliseconds.
     */
    public long getScanIntervalMs() {
        return scanInterval.toMillis();
    }

    /**
     * Sets the scan interval between device enumerations.
     *
     * @param scanInterval The scan interval.
     * @return This configuration object.
     * @throws NullPointerException     If {@code scanInterval} is {@code null}.
     * @throws IllegalArgumentException If {@code scanInterval} is negative.
     */
    public @NotNull HidServicesSpecification setScanInterval(
            @NotNull Duration scanInterval) {
        requireNonNegativeDuration(scanInterval, "scanInterval");
        this.scanInterval = scanInterval;
        return this;
    }

    /**
     * Sets the scan interval between device enumerations.
     *
     * @param scanIntervalMs The scan interval in milliseconds.
     * @return This configuration object.
     * @throws IllegalArgumentException If {@code scanIntervalMs} is negative.
     */
    public @NotNull HidServicesSpecification setScanIntervalMs(
            long scanIntervalMs) {
        Duration scanInterval = Duration.ofMillis(scanIntervalMs);
        return this.setScanInterval(scanInterval);
    }

    /**
     * Returns the interval where device enumeration is paused.
     *
     * @return The pause interval.
     */
    public @NotNull Duration getPauseInterval() {
        return this.pauseInterval;
    }

    /**
     * Returns the interval where device enumeration is paused.
     *
     * @return The pause interval in milliseconds.
     */
    public long getPauseIntervalMs() {
        return pauseInterval.toMillis();
    }

    /**
     * Sets the interval where device enumeration is paused.
     * <p>
     * <b>Note:</b> This will have no effect if the scan mode does not
     * support pausing.
     *
     * @param pauseInterval The pause interval in milliseconds.
     * @return This configuration object.
     * @throws NullPointerException     If {@code pauseInterval} is {@code null}.
     * @throws IllegalArgumentException If {@code pauseInterval} is negative.
     */
    public @NotNull HidServicesSpecification setPauseInterval(
            @NotNull Duration pauseInterval) {
        requireNonNegativeDuration(pauseInterval, "pauseInterval");
        this.pauseInterval = pauseInterval;
        return this;
    }

    /**
     * Sets the interval where device enumeration is paused.
     * <p>
     * <b>Note:</b> This will have no effect if the scan mode does not
     * support pausing.
     *
     * @param pauseIntervalMs The pause interval in milliseconds.
     * @return This configuration object.
     * @throws IllegalArgumentException If {@code pauseIntervalMs} is negative.
     */
    public @NotNull HidServicesSpecification setPauseIntervalMs(
            long pauseIntervalMs) {
        Duration pauseInterval = Duration.ofMillis(pauseIntervalMs);
        return this.setPauseInterval(pauseInterval);
    }

    /**
     * Returns if HID services will start before any listeners are
     * registered.
     *
     * @return {@code true} if services will start automatically,
     * {@code false} otherwise.
     */
    public boolean isAutoStart() {
        return this.autoStart;
    }

    /**
     * Sets whether HID services should start before any listeners are
     * registered (default value is backwards compatible with 0.6.0 and
     * below).
     *
     * @param autoStart {@code true} if services should start before any
     *                  listeners are registered, {@code false} otherwise.
     * @return This configuration object.
     */
    public @NotNull HidServicesSpecification setAutoStart(
            boolean autoStart) {
        this.autoStart = autoStart;
        return this;
    }

    /**
     * Returns whether the shutdown hook will also close the HID API.
     *
     * @return {@code true} if the shutdown hook will also close the HID
     * API, {@code false} otherwise.
     */
    public boolean isAutoShutdown() {
        return this.autoShutdown;
    }

    /**
     * Sets whether the shutdown hook should also close the HID API
     * automatically (automatic behavior is recommended).
     *
     * @param autoShutdown {@code true} if the shutdown hook should also
     *                     close the HID API, {@code false} otherwise.
     * @return This configuration object.
     */
    public @NotNull HidServicesSpecification setAutoShutdown(
            boolean autoShutdown) {
        this.autoShutdown = autoShutdown;
        return this;
    }

    /**
     * Returns if HID device input buffers will be automatically checked
     * and read from.
     *
     * @return {@code true} if device input buffers will be automatically
     * checked and read, {@code false} otherwise.
     */
    public boolean isAutoDataRead() {
        return this.autoDataRead;
    }

    /**
     * Sets whether HID device input buffers should be automatically
     * checked and read from.
     *
     * @param autoDataRead {@code true} if device input buffers should be
     *                     automatically checked and read, {@code false}
     *                     otherwise.
     * @return This configuration object.
     * @since 0.8.0
     */
    public @NotNull HidServicesSpecification setAutoDataRead(
            boolean autoDataRead) {
        this.autoDataRead = autoDataRead;
        return this;
    }

    /**
     * Returns the interval between attempts to read device input buffers.
     *
     * @return The data read interval.
     */
    public @NotNull Duration getDataReadInterval() {
        return this.dataReadInterval;
    }

    /**
     * Returns the interval between attempts to read device input buffers.
     *
     * @return The data read interval in milliseconds.
     */
    public long getDataReadIntervalMs() {
        return dataReadInterval.toMillis();
    }

    /**
     * Sets the interval between attempts to read device input buffers.
     *
     * @param dataReadInterval The data read interval.
     * @return This configuration object.
     * @throws NullPointerException     If {@code dataReadInterval} is {@code null}.
     * @throws IllegalArgumentException If {@code dataReadInterval} is negative.
     */
    public @NotNull HidServicesSpecification setDataReadInterval(
            @NotNull Duration dataReadInterval) {
        requireNonNegativeDuration(dataReadInterval, "dataReadInterval");
        this.dataReadInterval = dataReadInterval;
        return this;
    }

    /**
     * Sets the interval between attempts to read device input buffers.
     *
     * @param dataReadIntervalMs The data read interval in milliseconds.
     * @return This configuration object.
     * @throws IllegalArgumentException If {@code dataReadIntervalMs} is negative.
     */
    public @NotNull HidServicesSpecification setDataReadIntervalMs(
            long dataReadIntervalMs) {
        Duration dataReadInterval = Duration.ofMillis(dataReadIntervalMs);
        return this.setDataReadInterval(dataReadInterval);
    }

    private static void
    requireNonNegativeDuration(Duration duration, String name) {
        Objects.requireNonNull(duration, name + " cannot be null");
        if (duration.isNegative()) {
            String message = name + " cannot be negative";
            throw new IllegalArgumentException(message);
        }
    }

}
