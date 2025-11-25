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

import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.server.VaadinSession;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.InterruptedByTimeoutException;

/**
 * An implementation of {@link DownloadHandler} that controls access to the
 * {@link #handleDownloadRequest(DownloadEvent) handleDownloadRequest} method
 * using a semaphore to
 * manage concurrency.
 *
 * @author Javier Godoy
 */
@SuppressWarnings("serial")
abstract class ConcurrentDownloadHandler extends ConcurrentOperationBase implements DownloadHandler {

    private final DownloadHandler delegate;

    /**
     * Constructs a {@code ConcurrentDownloadHandler} with the specified delegate.
     * The delegate is a
     * {@link DownloadHandler} that performs the actual download handling.
     *
     * @param delegate the delegate {@code DownloadHandler}
     */
    ConcurrentDownloadHandler(DownloadHandler delegate) {
        this.delegate = delegate;
    }

    /**
     * Handles the download request using the provided {@link DownloadEvent}.
     * <p>
     * Note that the method is not called under the session lock. It means that if
     * implementation
     * requires access to the application/session data then the session has to be
     * locked explicitly.
     * <p>
     * If a semaphore has been set, it controls access to this method, enforcing a
     * timeout. A permit
     * will be acquired from the semaphore, if one becomes available within the
     * given waiting time and
     * the current thread has not been {@linkplain Thread#interrupt interrupted}.
     *
     * @param event the download event containing the output stream and session
     * @throws IOException                   if an IO error occurred
     * @throws InterruptedIOException        if the current thread is interrupted
     * @throws InterruptedByTimeoutException if the waiting time elapsed before a
     *                                       permit was acquired
     */
    @Override
    public final void handleDownloadRequest(DownloadEvent event) throws IOException {
        runWithSemaphore(event.getSession(), () -> delegate.handleDownloadRequest(event));
    }

}
