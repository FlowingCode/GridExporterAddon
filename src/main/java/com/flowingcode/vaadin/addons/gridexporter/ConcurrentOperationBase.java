/*-
 * #%L
 * Grid Exporter Add-on
 * %%
 * Copyright (C) 2022 - 2024 Flowing Code
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.flowingcode.vaadin.addons.gridexporter;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

/**
 * Base class containing shared semaphore logic for concurrent download/upload
 * control.
 * This class is used by both ConcurrentStreamResourceWriter and
 * ConcurrentDownloadHandler
 * to avoid code duplication.
 *
 * @author Javier Godoy
 */
@SuppressWarnings("serial")
abstract class ConcurrentOperationBase {

    public static final float MAX_COST = 0x7FFF;
    public static final float MIN_COST = 1.0f / 0x10000;
    public static final float DEFAULT_COST = 1.0f;

    static final ConfigurableSemaphore semaphore = new ConfigurableSemaphore();
    static volatile boolean enabled;
    static volatile boolean failOnUiChange;

    static final class ConfigurableSemaphore extends Semaphore {

        int maxPermits; // package-private for access from subclasses

        ConfigurableSemaphore() {
            super(0);
        }

        synchronized void setPermits(int permits) {
            if (permits < 0) {
                throw new IllegalArgumentException();
            }
            int delta = permits - maxPermits;
            if (delta > 0) {
                super.release(delta);
            } else if (delta < 0) {
                super.reducePermits(-delta);
            }
            maxPermits = permits;
        }

        @Override
        public String toString() {
            IntFunction<String> str = permits -> {
                float f = permits / (float) 0x10000;
                return f == Math.floor(f) ? String.format("%.0f", f) : Float.toString(f);
            };
            return "Semaphore[" + str.apply(availablePermits()) + "/" + str.apply(maxPermits) + "]";
        }
    }

    /**
     * Sets the limit for the cost of concurrent operations.
     * <p>
     * Finite limits are capped to {@link #MAX_COST} (32767). If the limit is
     * {@link Float#POSITIVE_INFINITY POSITIVE_INFINITY}, the semaphore will not be
     * used for
     * controlling concurrent operations.
     *
     * @param limit the maximum cost of concurrent operations allowed
     * @throws IllegalArgumentException if the limit is zero or negative.
     */
    public static void setLimit(float limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException();
        }
        if (Float.isInfinite(limit)) {
            enabled = false;
            return;
        }

        synchronized (semaphore) {
            enabled = true;
            semaphore.setPermits(costToPermits(limit, Integer.MAX_VALUE));
        }
    }

    static void setFailOnUiChange(boolean failOnUiChange) {
        ConcurrentOperationBase.failOnUiChange = failOnUiChange;
    }

    /**
     * Returns the limit for the number of concurrent operations.
     *
     * @return the limit for the number of concurrent operations, or
     *         {@link Float#POSITIVE_INFINITY}
     *         if the semaphore is disabled.
     */
    public static float getLimit() {
        if (enabled) {
            synchronized (semaphore) {
                return (float) semaphore.maxPermits / 0x10000;
            }
        } else {
            return Float.POSITIVE_INFINITY;
        }
    }

    static int costToPermits(float cost, int maxPermits) {
        // restrict limit to 0x7fff to ensure the cost can be represented
        // using fixed-point arithmetic with 16 fractional digits and 15 integral digits
        cost = Math.min(cost, MAX_COST);
        // Determine the number of permits required based on the cost, capping at
        // maxPermits.
        // If the cost is zero or negative, no permits are needed.
        // Any positive cost, no matter how small, will require at least one permit.
        return cost <= 0 ? 0 : Math.max(Math.min((int) (cost * 0x10000), maxPermits), 1);
    }

    /**
     * Sets the timeout for acquiring a permit to start a download when there are
     * not enough permits
     * available in the semaphore.
     *
     * @return the timeout in nanoseconds.
     */
    public abstract long getTimeout();

    /**
     * Returns the cost of this download.
     *
     * Note that the method is not called under the session lock. It means that if
     * implementation
     * requires access to the application/session data then the session has to be
     * locked explicitly.
     *
     * @param session vaadin session
     */
    public float getCost(VaadinSession session) {
        return DEFAULT_COST;
    }

    /**
     * Returns the UI associated with the current download.
     * <p>
     * This method is used to ensure that the UI is still attached to the current
     * session when a
     * download is initiated. Implementations should return the appropriate UI
     * instance.
     * </p>
     *
     * @return the {@link UI} instance associated with the current download, or
     *         {@code null} if no UI
     *         is available.
     */
    protected abstract UI getUI();

    private UI getAttachedUI() {
        return Optional.ofNullable(getUI()).filter(UI::isAttached).orElse(null);
    }

    /**
     * Callback method that is invoked when a timeout occurs while trying to acquire
     * a permit for
     * starting a download.
     * <p>
     * Implementations can use this method to perform any necessary actions in
     * response to the
     * timeout, such as logging a warning or notifying the user.
     * </p>
     */
    protected abstract void onTimeout();

    /**
     * Callback method that is invoked when a download is accepted.
     * <p>
     * This method is called at the start of the download process.
     * Subclasses should implement this method to perform any necessary actions
     * before the download
     * begins.
     */
    protected abstract void onAccept();

    /**
     * Callback method that is invoked when a download finishes.
     * <p>
     * This method is called at the end of the download process.
     * Subclasses should implement this method to perform any necessary actions
     * after the download
     * completes.
     */
    protected abstract void onFinish();

    @FunctionalInterface
    protected interface RunnableWithIOException {
        void run() throws IOException;
    }

    protected void runWithSemaphore(VaadinSession session, RunnableWithIOException task)
            throws IOException {
        onAccept();
        try {
            if (!enabled) {
                task.run();
            } else {
                try {
                    int permits;
                    float cost = getCost(session);
                    synchronized (semaphore) {
                        permits = costToPermits(cost, semaphore.maxPermits);
                    }

                    UI ui = failOnUiChange ? getAttachedUI() : null;

                    if (semaphore.tryAcquire(permits, getTimeout(), TimeUnit.NANOSECONDS)) {
                        try {
                            if (ui != null && getAttachedUI() != ui) {
                                throw new IOException("Detached UI");
                            }
                            task.run();
                        } finally {
                            semaphore.release(permits);
                        }
                    } else {
                        onTimeout();
                        throw new InterruptedByTimeoutException();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw (IOException) new InterruptedIOException().initCause(e);
                }
            }
        } finally {
            onFinish();
        }
    }
}
