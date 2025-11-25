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

import com.vaadin.flow.server.StreamResourceWriter;
import com.vaadin.flow.server.VaadinSession;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.channels.InterruptedByTimeoutException;

/**
 * An implementation of {@link StreamResourceWriter} that controls access to the
 * {@link #accept(OutputStream, VaadinSession) accept} method using a semaphore
 * to manage
 * concurrency.
 *
 * @author Javier Godoy
 */
@SuppressWarnings("serial")
abstract class ConcurrentStreamResourceWriter extends ConcurrentOperationBase implements StreamResourceWriter {

  private final StreamResourceWriter delegate;

  public static void setLimit(float limit) {
    ConcurrentOperationBase.setLimit(limit);
  }

  public static float getLimit() {
    return ConcurrentOperationBase.getLimit();
  }

  /**
   * Constructs a {@code ConcurrentStreamResourceWriter} with the specified
   * delegate. The delegate
   * is a {@link StreamResourceWriter} that performs the actual writing to the
   * stream.
   *
   * @param delegate the delegate {@code InputStreamFactory}
   */
  ConcurrentStreamResourceWriter(StreamResourceWriter delegate) {
    this.delegate = delegate;
  }

  /**
   * Handles {@code stream} (writes data to it) using {@code session} as a
   * context.
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
   * @param stream  data output stream
   * @param session vaadin session
   * @throws IOException                   if an IO error occurred
   * @throws InterruptedIOException        if the current thread is interrupted
   * @throws InterruptedByTimeoutException if the waiting time elapsed before a
   *                                       permit was acquired
   */
  @Override
  public final void accept(OutputStream stream, VaadinSession session) throws IOException {
    runWithSemaphore(session, () -> delegate.accept(stream, session));
  }

}
