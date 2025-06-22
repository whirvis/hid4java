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
package org.hid4java.event;

import org.hid4java.HidDevice;
import org.hid4java.HidServicesListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

/**
 * Container for {@link HidServicesListener} that can be used to
 * fire events.
 *
 * @since 0.0.1
 */
public class HidServicesListenerList {

    private final List<HidServicesListener> listeners;
    private final ReadWriteLock listenersLock;
    private final ExecutorService executorService;

    /**
     * Constructs a new {@code HidServicesListenerList}.
     */
    public HidServicesListenerList() {
        this.listeners = new ArrayList<>();
        this.listenersLock = new ReentrantReadWriteLock();
        this.executorService = createEventThreadPool(3);
    }

    /**
     * Returns an unmodifiable view of the listeners.
     *
     * @return An unmodifiable view of the listeners.
     */
    public final @NotNull List<HidServicesListener> getListeners() {
        return Collections.unmodifiableList(listeners);
    }

    /**
     * Adds a listener.
     *
     * @param listener The listener to add.
     * @throws NullPointerException If {@code listener} is {@code null}.
     */
    public final void add(@NotNull HidServicesListener listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        listenersLock.writeLock().lock();
        try {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        } finally {
            listenersLock.writeLock().unlock();
        }
    }

    /**
     * Removes a listener.
     *
     * @param listener The listener to remove.
     */
    public final void remove(@Nullable HidServicesListener listener) {
        if (listener == null) {
            return; /* don't bother with obtaining a lock */
        }
        listenersLock.writeLock().lock();
        try {
            listeners.remove(listener);
        } finally {
            listenersLock.writeLock().unlock();
        }
    }

    /**
     * Removes all listeners.
     */
    public final void clear() {
        listenersLock.writeLock().lock();
        try {
            listeners.clear();
        } finally {
            listenersLock.writeLock().unlock();
        }
    }

    private @NotNull List<HidServicesListener> copyListeners() {
        listenersLock.readLock().lock();
        try {
            List<HidServicesListener> copy = new ArrayList<>();
            Collections.copy(listeners, copy);
            return copy;
        } finally {
            listenersLock.readLock().unlock();
        }
    }

    private void fireHidEvent(
            @NotNull HidServicesEvent event,
            @NotNull BiConsumer<@NotNull HidServicesListener,
                    @NotNull HidServicesEvent> callback
    ) {
        /*
         * We use a copy to make sure no concurrent modifications occur while
         * processing an event. Even if we obtain a lock to process the events,
         * a listener removing itself (or adding another listener) could result
         * in a deadlock.
         */
        List<HidServicesListener> listeners = this.copyListeners();

        for (HidServicesListener listener : listeners) {
            try {
                callback.accept(listener, event);
            } catch (Throwable cause) {
                listener.hidListenerException(event, cause);
            }
        }
    }

    /**
     * Fires the HID device attached event.
     *
     * @param hidDevice The device that was attached.
     * @throws NullPointerException If {@code hidDevice} is {@code null}.
     * @see HidServicesListener#hidDeviceAttached(HidServicesEvent)
     */
    public void fireHidDeviceAttached(@NotNull HidDevice hidDevice) {
        Objects.requireNonNull(hidDevice, "hidDevice cannot be null");
        executorService.submit(() -> {
            HidServicesEvent event = new HidServicesEvent(hidDevice);
            this.fireHidEvent(event, HidServicesListener::hidDeviceAttached);
        });
    }

    /**
     * Fires the HID device detached event.
     *
     * @param hidDevice The device that was detached.
     * @throws NullPointerException If {@code hidDevice} is {@code null}.
     * @see HidServicesListener#hidDeviceDetached(HidServicesEvent)
     */
    public void fireHidDeviceDetached(@NotNull HidDevice hidDevice) {
        Objects.requireNonNull(hidDevice, "hidDevice cannot be null");
        executorService.submit(() -> {
            HidServicesEvent event = new HidServicesEvent(hidDevice);
            this.fireHidEvent(event, HidServicesListener::hidDeviceDetached);
        });
    }

    /**
     * Fires the HID failure event.
     *
     * @param hidDevice The device that caused the error, {@code null}
     *                  if the device is unknown.
     * @see HidServicesListener#hidFailure(HidServicesEvent)
     */
    public void fireHidFailure(@Nullable HidDevice hidDevice) {
        /* TODO: why is this unused, where would it be called? */
        executorService.submit(() -> {
            HidServicesEvent event = new HidServicesEvent(hidDevice);
            this.fireHidEvent(event, HidServicesListener::hidFailure);
        });
    }

    /**
     * Fires the HID data received event.
     *
     * @param hidDevice The device that sent data input.
     * @param data      The buffer containing the received data.
     * @throws NullPointerException If {@code hidDevice} or {@code data}
     *                              are {@code null}.
     * @see HidServicesListener#hidDataReceived(HidServicesEvent)
     */
    public void fireHidDataReceived(
            @NotNull HidDevice hidDevice, byte @NotNull [] data) {
        Objects.requireNonNull(hidDevice, "hidDevice cannot be null");
        Objects.requireNonNull(data, "data cannot be null");
        executorService.submit(() -> {
            HidServicesEvent event = new HidServicesEvent(hidDevice, data);
            this.fireHidEvent(event, HidServicesListener::hidDataReceived);
        });
    }

    private static @NotNull Thread
    createEventThread(@NotNull Runnable runnable) {
        Thread eventThread = Executors
                .defaultThreadFactory()
                .newThread(runnable);

        eventThread.setName("hid4java Event Worker");
        eventThread.setDaemon(true);

        return eventThread;
    }

    @SuppressWarnings("SameParameterValue")
    private static @NotNull ExecutorService
    createEventThreadPool(int threadCount) {
        return Executors.newFixedThreadPool(
                threadCount,
                HidServicesListenerList::createEventThread
        );
    }

}
