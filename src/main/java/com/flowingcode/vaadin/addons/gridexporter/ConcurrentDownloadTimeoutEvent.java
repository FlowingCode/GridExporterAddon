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

import java.util.EventObject;
import java.util.Objects;

/**
 * An event that is fired when a concurrent download timeout occurs in the {@link GridExporter}.
 * <p>
 * This event allows the handler to determine whether the event propagation should be stopped,
 * preventing other listeners from processing the event.
 * </p>
 *
 * @see GridExporterConcurrentSettings#setConcurrentDownloadTimeout(long,
 *      java.util.concurrent.TimeUnit)
 */
@SuppressWarnings("serial")
public class ConcurrentDownloadTimeoutEvent extends EventObject {

  private boolean propagationStopped;

  /**
   * Constructs a new ConcurrentDownloadTimeoutEvent.
   *
   * @param source the {@link GridExporter} that is the source of this event
   * @throws IllegalArgumentException if source is null
   */
  public ConcurrentDownloadTimeoutEvent(GridExporter<?> source) {
    super(Objects.requireNonNull(source));
  }

  /**
   * Returns the source of this event.
   *
   * @return the {@code GridExporter} that is the source of this event
   */
  @Override
  public GridExporter<?> getSource() {
    return (GridExporter<?>) super.getSource();
  }

  /**
   * Stops the propagation of this event. When propagation is stopped, other listeners will not be
   * notified of this event.
   */
  public void stopPropagation() {
    propagationStopped = true;
  }

  /**
   * Checks if the propagation of this event has been stopped.
   *
   * @return {@code true} if the propagation has been stopped, {@code false} otherwise
   * @see #stopPropagation()
   */
  public boolean isPropagationStopped() {
    return propagationStopped;
  }
}
