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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manager which provides access to the underlying HID API library, device
 * attach/detach events, and device data reading (if configured).
 *
 * @since 0.0.1
 */
class HidDeviceManager {

    private final @NotNull HidServicesListenerList listeners;
    private final @NotNull HidServicesSpecification specs;
    private final @NotNull Map<String, HidDevice> attachedDevices;
    private final @NotNull Lock managerLock;

    private @Nullable Thread scanThread;

    HidDeviceManager(
            @NotNull HidServicesListenerList listeners,
            @NotNull HidServicesSpecification specs) {
        this.listeners = listeners;
        this.specs = specs;
        this.attachedDevices = new HashMap<>();
        this.managerLock = new ReentrantLock();

        /* attempt to initialize immediately */
        try {
            HidApi.init();
        } catch (Exception e) {
            throw new HidException("Unable to initialize HID API", e);
        }
    }

    void onDeviceDataReceived(
            HidDevice hidDevice, byte[] dataReceived) {
        if (dataReceived.length == 0) {
            return; /* don't bother with obtaining a lock */
        }
        managerLock.lock();
        try {
            listeners.fireHidDataReceived(hidDevice, dataReceived);
        } finally {
            managerLock.unlock();
        }
    }

    void onDeviceWrite() {
        managerLock.lock();
        try {
            if (!this.isScanning() || specs.getScanMode() !=
                    ScanMode.SCAN_AT_FIXED_INTERVAL_WITH_PAUSE_AFTER_WRITE) {
                return; /* nothing to do */
            }

            /* ensure we have a new scan executor service available */
            this.stopScanThread();
            this.configureScanThread();
        } finally {
            managerLock.unlock();
        }
    }

    /**
     * Returns all currently attached HID devices.
     *
     * @return All currently attached HID devices.
     */
    public @NotNull List<HidDevice> getAttachedHidDevices() {
        HidDeviceInfoStructure root;
        try {
            root = HidApi.enumerateDevices(0x0000, 0x0000);
        } catch (Throwable e) {
            this.stop(); /* something serious has happened */
            throw new HidException("Unable to initialize HID API", e);
        }

        /* just quit if no devices were found */
        if (root == null) {
            return Collections.emptyList();
        }

        List<HidDevice> devices = new ArrayList<>();
        HidDeviceInfoStructure current = root;
        do {
            devices.add(new HidDevice(current, this, specs));
            current = current.next;
        } while (current != null);
        HidApi.freeEnumeration(root);

        return devices;
    }

    /**
     * Returns if the scan thread is running.
     *
     * @return {@code true} if the scan thread is running,
     * {@code false} otherwise.
     */
    public boolean isScanning() {
        managerLock.lock();
        try {
            return scanThread != null && scanThread.isAlive();
        } finally {
            managerLock.unlock();
        }
    }

    private void scanNoLock() {
        List<String> nowDetached = new ArrayList<>();
        List<HidDevice> currentlyAttached = this.getAttachedHidDevices();

        /* find all new currently attached devices */
        for (HidDevice device : currentlyAttached) {
            String path = device.getPath();
            if (!attachedDevices.containsKey(path)) {
                attachedDevices.put(path, device);
                listeners.fireHidDeviceAttached(device);
            }
        }

        /* find all devices that have been detached */
        for (Map.Entry<String, HidDevice> entry : attachedDevices.entrySet()) {
            HidDevice device = entry.getValue();
            if (!currentlyAttached.contains(device)) {
                nowDetached.add(device.getPath());
                listeners.fireHidDeviceDetached(device);
            }
        }

        if (!nowDetached.isEmpty()) {
            nowDetached.forEach(attachedDevices.keySet()::remove);
        }
    }

    /**
     * Scans for newly connected devices and removes devices that are no
     * longer connected.
     * <p>
     * This will fire device attach/detach events as appropriate.
     */
    public void scan() {
        managerLock.lock();
        try {
            this.scanNoLock();
        } finally {
            managerLock.unlock();
        }
    }


    private void configureScanThread() {
        managerLock.lock();
        try {
            if (this.isScanning()) {
                this.stopScanThread();
            }

            Thread scanThread = new ScanThread(this);
            scanThread.setDaemon(true);
            scanThread.setName("hid4java device scanner");
            scanThread.start();

            this.scanThread = scanThread;
        } finally {
            managerLock.unlock();
        }
    }

    private void stopScanThread() {
        managerLock.lock();
        try {
            if (!this.isScanning()) {
                return; /* nothing to do */
            }

            /*
             * We must wait up to 50ms for the scan thread to terminate
             * in order to avoid spurious return values from isScanning().
             * See hid4java issue #125.
             */

            //noinspection DataFlowIssue
            scanThread.interrupt();
            try {
                scanThread.join(50);
            } catch (InterruptedException e) {
                /* ignore and continue */
            }
        } finally {
            managerLock.unlock();
        }
    }

    /**
     * Starts the manager.
     * <p>
     * If the manager has already been started, it will immediately return
     * without doing anything. Otherwise, this will perform a one-off scan of
     * all devices. Then, if the scan interval is zero, it will stop there or
     * start the scanning daemon thread at the required interval.
     *
     * @throws HidException If an HID error occurs.
     */
    public void start() {
        managerLock.lock();
        try {
            if (this.isScanning()) {
                return; /* manager already started */
            }

            /* perform initial scan to populate attached devices */
            this.scan();
            this.configureScanThread();
        } finally {
            managerLock.unlock();
        }
    }

    /**
     * Stops the scan thread and closes all attached devices.
     *
     * @throws HidException If an HID error occurs.
     */
    public void stop() {
        managerLock.lock();
        try {
            this.stopScanThread();
            attachedDevices.values().forEach(HidDevice::close);
            attachedDevices.clear();
        } finally {
            managerLock.unlock();
        }
    }

    private static class ScanThread extends Thread {

        private final HidDeviceManager manager;
        private final Runnable scanRunnable;

        public ScanThread(HidDeviceManager manager) {
            this.manager = manager;
            this.scanRunnable = this.getScanRunnable();
        }

        private void doNotScan() {
            /* do nothing */
        }

        @SuppressWarnings("BusyWait")
        private void scanAtFixedInterval() {
            long scanInterval = manager.specs.getScanIntervalMs();
            while (!this.isInterrupted()) {
                try {
                    Thread.sleep(scanInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                manager.scan();
            }
        }

        @SuppressWarnings("BusyWait")
        private void scanAtFixedIntervalWithPauseAfterWrite() {
            long scanInterval = manager.specs.getScanIntervalMs();
            long pauseInterval = manager.specs.getPauseIntervalMs();

            /* provide an initial pause */
            try {
                Thread.sleep(pauseInterval);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            /* switch to continuous scanning */
            while (!this.isInterrupted()) {
                try {
                    Thread.sleep(scanInterval);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                manager.scan();
            }
        }

        private Runnable getScanRunnable() {
            ScanMode scanMode = manager.specs.getScanMode();
            switch (scanMode) {
                case NO_SCAN:
                    return this::doNotScan;
                case SCAN_AT_FIXED_INTERVAL:
                    return this::scanAtFixedInterval;
                case SCAN_AT_FIXED_INTERVAL_WITH_PAUSE_AFTER_WRITE:
                    return this::scanAtFixedIntervalWithPauseAfterWrite;
                default:
                    String message = "Unexpected scan mode " + scanMode;
                    throw new HidException(message);
            }
        }

        @Override
        public void run() {
            scanRunnable.run();
        }

    }

}
