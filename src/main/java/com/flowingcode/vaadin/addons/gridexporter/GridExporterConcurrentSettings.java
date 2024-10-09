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

import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Provides global settings for managing concurrent downloads in {@link GridExporter}.
 * <p>
 * This class allows for setting limits on concurrent downloads, configuring timeouts for acquiring
 * download permits, and adding global listeners for concurrent download timeout events.
 * </p>
 */
public class GridExporterConcurrentSettings {

  private static long concurrentDownloadTimeoutNanos = 0L;

  private static final List<SerializableConsumer<ConcurrentDownloadTimeoutEvent>> globalDownloadTimeoutListeners =
      new CopyOnWriteArrayList<>();

  static List<SerializableConsumer<ConcurrentDownloadTimeoutEvent>> getGlobalDownloadTimeoutListeners() {
    return Collections.unmodifiableList(globalDownloadTimeoutListeners);
  }

  /**
   * Sets the limit for the {@linkplain GridExporter#setConcurrentDownloadCost(float) cost of
   * concurrent downloads}. If all the downloads have a cost of {@link GridExporter#DEFAULT_COST},
   * the limit represents the number of concurrent downloads that are allowed.
   * <p>
   * Finite limits are capped to {@link GridExporter#MAX_COST} (32767). If the limit is
   * {@link Float#POSITIVE_INFINITY POSITIVE_INFINITY}, concurrent downloads will not be limited.
   *
   * @param limit the maximum cost of concurrent downloads allowed
   * @throws IllegalArgumentException if the limit is zero or negative.
   */
  public static void setConcurrentDownloadLimit(float limit) {
    ConcurrentStreamResourceWriter.setLimit(limit);
  }

  /**
   * Returns the limit for the number of concurrent downloads.
   *
   * @return the limit for the number of concurrent downloads, or {@link Float#POSITIVE_INFINITY} if
   *         concurrent downloads are not limited.
   */
  public static float getConcurrentDownloadLimit() {
    return ConcurrentStreamResourceWriter.getLimit();
  }

  /**
   * Configures the behavior of the stream operation when the UI changes during execution.
   *
   * @param failOnUiChange If {@code true}, the operation will throw an {@link IOException} if the
   *        UI changes (e.g., becomes detached) after acquiring the semaphore. If {@code false}, the
   *        operation will proceed regardless of any UI changes.
   */
  public static void setFailOnUiChange(boolean failOnUiChange) {
    ConcurrentStreamResourceWriter.setFailOnUiChange(failOnUiChange);
  }

  /**
   * Sets the timeout for acquiring a permit to start a download when the
   * {@linkplain #setConcurrentDownloadLimit(float) maximum number of concurrent downloads} is
   * reached. If the timeout is less than or equal to zero, the downloads will fail immediately if
   * no enough permits can be acquired.
   *
   * This timeout is crucial for preventing the system from hanging indefinitely while waiting for
   * available resources. If the timeout expires before a permit can be acquired, the download is
   * cancelled.
   *
   * @param timeout the maximum time to wait for a permit
   * @param unit the time unit of the {@code timeout} argument
   */
  public static void setConcurrentDownloadTimeout(long timeout, TimeUnit unit) {
    concurrentDownloadTimeoutNanos = unit.toNanos(timeout);
  }

  public static long getConcurrentDownloadTimeout(TimeUnit unit) {
    return unit.convert(concurrentDownloadTimeoutNanos, TimeUnit.NANOSECONDS);
  }

  /**
   * Adds a global listener for concurrent download timeout events.
   * <p>
   * The listener will be called whenever a concurrent download timeout event occurs.
   * <p>
   * Note that instance-specific listeners take precedence over global listeners. If an instance
   * listener stops the event propagation by calling
   * {@link ConcurrentDownloadTimeoutEvent#stopPropagation() stopPropagation()}, the global
   * listeners will not be notified.
   *
   * @param listener the listener to be added
   * @return a {@link Registration} object that can be used to remove the listener
   */
  public static Registration addGlobalConcurrentDownloadTimeoutEvent(
      SerializableConsumer<ConcurrentDownloadTimeoutEvent> listener) {
    globalDownloadTimeoutListeners.add(0, listener);
    return () -> globalDownloadTimeoutListeners.remove(listener);
  }
}
