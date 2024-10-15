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
