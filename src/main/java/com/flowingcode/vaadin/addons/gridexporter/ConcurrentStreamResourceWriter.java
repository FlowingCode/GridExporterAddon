package com.flowingcode.vaadin.addons.gridexporter;

import com.vaadin.flow.server.StreamResourceWriter;
import com.vaadin.flow.server.VaadinSession;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

/**
 * An implementation of {@link StreamResourceWriter} that controls access to the
 * {@link #accept(OutputStream, VaadinSession) accept} method using a semaphore to manage
 * concurrency.
 *
 * @author Javier Godoy
 */
@SuppressWarnings("serial")
abstract class ConcurrentStreamResourceWriter implements StreamResourceWriter {

  public static final float MAX_COST = 0x7FFF;

  public static final float MIN_COST = 1.0f / 0x10000;

  public static final float DEFAULT_COST = 1.0f;

  private static final ConfigurableSemaphore semaphore = new ConfigurableSemaphore();

  private static volatile boolean enabled;

  private final StreamResourceWriter delegate;

  private static final class ConfigurableSemaphore extends Semaphore {

    private int maxPermits;

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
   * Sets the limit for the cost of concurrent downloads.
   * <p>
   * Finite limits are capped to {@link #MAX_COST} (32767). If the limit is
   * {@link Float#POSITIVE_INFINITY POSITIVE_INFINITY}, the semaphore will not be used for
   * controlling concurrent downloads.
   *
   * @param limit the maximum cost of concurrent downloads allowed
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

  /**
   * Returns the limit for the number of concurrent downloads.
   *
   * @return the limit for the number of concurrent downloads, or {@link Float#POSITIVE_INFINITY} if
   *         the semaphore is disabled.
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

  private static int costToPermits(float cost, int maxPermits) {
    // restrict limit to 0x7fff to ensure the cost can be represented
    // using fixed-point arithmetic with 16 fractional digits and 15 integral digits
    cost = Math.min(cost, MAX_COST);
    // Determine the number of permits required based on the cost, capping at maxPermits.
    // If the cost is zero or negative, no permits are needed.
    // Any positive cost, no matter how small, will require at least one permit.
    return cost <= 0 ? 0 : Math.max(Math.min((int) (cost * 0x10000), maxPermits), 1);
  }

  /**
   * Constructs a {@code ConcurrentStreamResourceWriter} with the specified delegate. The delegate
   * is a {@link StreamResourceWriter} that performs the actual writing to the stream.
   *
   * @param delegate the delegate {@code InputStreamFactory}
   */
  ConcurrentStreamResourceWriter(StreamResourceWriter delegate) {
    this.delegate = delegate;
  }

  /**
   * Sets the timeout for acquiring a permit to start a download when there are not enough permits
   * available in the semaphore.
   *
   * @see GridExporter#setConcurrentDownloadTimeout(long, TimeUnit)
   * @return the timeout in nanoseconds.
   */
  public abstract long getTimeout();

  /**
   * Returns the cost of this download.
   *
   * Note that the method is not called under the session lock. It means that if implementation
   * requires access to the application/session data then the session has to be locked explicitly.
   *
   * @param session vaadin session
   * @see GridExporter#setConcurrentDownloadCost(float)
   */
  public float getCost(VaadinSession session) {
    return DEFAULT_COST;
  }

  /**
   * Handles {@code stream} (writes data to it) using {@code session} as a context.
   * <p>
   * Note that the method is not called under the session lock. It means that if implementation
   * requires access to the application/session data then the session has to be locked explicitly.
   * <p>
   * If a semaphore has been set, it controls access to this method, enforcing a timeout. A permit
   * will be acquired from the semaphore, if one becomes available within the given waiting time and
   * the current thread has not been {@linkplain Thread#interrupt interrupted}.
   *
   * @param stream data output stream
   * @param session vaadin session
   * @throws IOException if an IO error occurred
   * @throws InterruptedIOException if the current thread is interrupted
   * @throws InterruptedByTimeoutException if the waiting time elapsed before a permit was acquired
   */
  @Override
  public final void accept(OutputStream stream, VaadinSession session) throws IOException {

    if (!enabled) {
      delegate.accept(stream, session);
    } else {

      try {

        int permits;
        float cost = getCost(session);
        synchronized (semaphore) {
          permits = costToPermits(cost, semaphore.maxPermits);
        }

        if (semaphore.tryAcquire(permits, getTimeout(), TimeUnit.NANOSECONDS)) {
          try {
            delegate.accept(stream, session);
          } finally {
            semaphore.release(permits);
          }
        } else {
          throw new InterruptedByTimeoutException();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw (IOException) new InterruptedIOException().initCause(e);
      }
    }
  }

}