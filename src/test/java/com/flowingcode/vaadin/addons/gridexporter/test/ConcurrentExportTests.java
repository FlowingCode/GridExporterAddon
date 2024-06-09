package com.flowingcode.vaadin.addons.gridexporter.test;

import static org.apache.commons.io.output.NullOutputStream.NULL_OUTPUT_STREAM;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import com.flowingcode.vaadin.addons.gridexporter.ConfigurableConcurrentStreamResourceWriter;
import com.flowingcode.vaadin.addons.gridexporter.GridExporter;
import com.vaadin.flow.server.StreamResourceWriter;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import java.io.IOException;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("serial")
public class ConcurrentExportTests {

  private static final int TEST_TIMEOUT = 5000;

  private static Matcher<Throwable> interruptedByTimeout() {
    return Matchers.instanceOf(InterruptedByTimeoutException.class);
  }

  private class ConcurrentStreamResourceWriter
      extends ConfigurableConcurrentStreamResourceWriter {

    public ConcurrentStreamResourceWriter(StreamResourceWriter delegate) {
      super(delegate);
    }

  }

  private CyclicBarrier barrier;

  private void initializeCyclicBarrier(int parties) {
    barrier = new CyclicBarrier(parties);
  }

  @Before
  public void before() {
    barrier = null;
  }

  @SuppressWarnings("unchecked")
  private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
    throw (E) e;
  }

  private interface MockDownload {
    MockDownload withTimeout(long timeout);

    MockDownload withCost(float cost);

    Throwable get() throws InterruptedException;

    MockDownload await() throws InterruptedException;

    MockDownload start();
  }

  private MockDownload newDownload() {

    CountDownLatch latch = new CountDownLatch(1);

    ConcurrentStreamResourceWriter writer =
        new ConcurrentStreamResourceWriter((stream, session) -> {
          latch.countDown();
          await(barrier);
        });

    Exchanger<Throwable> exchanger = new Exchanger<>();

    Thread thread = new Thread(() -> {

      Throwable throwable = null;
      try {
        writer.accept(NULL_OUTPUT_STREAM, createSession());
      } catch (Throwable t) {
        throwable = t;
      }

      latch.countDown();
      try {
        exchanger.exchange(throwable);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });

    return new MockDownload() {
      @Override
      public Throwable get() throws InterruptedException {
        if (thread.getState() == Thread.State.NEW) {
          throw new IllegalStateException("Download has not started");
        }
        return exchanger.exchange(null);
      }

      @Override
      public MockDownload start() {
        if (thread.getState() == Thread.State.NEW) {
          thread.start();
        }

        try {
          latch.await(1, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
          sneakyThrow(e);
        }

        return this;
      }

      @Override
      public MockDownload await() throws InterruptedException {
        if (thread.getState() == Thread.State.NEW) {
          thread.start();
        }
        latch.await();
        return this;
      }

      @Override
      public MockDownload withTimeout(long timeout) {
        writer.setTimeout(TimeUnit.MILLISECONDS.toNanos(timeout));
        return this;
      }

      @Override
      public MockDownload withCost(float cost) {
        writer.setCost(cost);
        return this;
      }
    };
  }

  private static void await(CyclicBarrier barrier) {
    try {
      barrier.await();
    } catch (Exception e) {
      sneakyThrow(e);
    }
  }

  private VaadinSession createSession() {
    Lock lock = new ReentrantLock();
    VaadinService service = new VaadinServletService(null, null);
    return new VaadinSession(service) {
      @Override
      public Lock getLockInstance() {
        return lock;
      }
    };
  }

  @Test
  public void testSetLimit() throws IOException {

    float[] costs = new float[] {0.5f, 1, 2, ConcurrentStreamResourceWriter.MIN_COST,
        2 * ConcurrentStreamResourceWriter.MIN_COST, ConcurrentStreamResourceWriter.MAX_COST,
        Float.POSITIVE_INFINITY};

    // increment permits
    for (float cost : costs) {
      ConcurrentStreamResourceWriter.setLimit(cost);
      Assert.assertEquals(cost, ConcurrentStreamResourceWriter.getLimit(), 0);
    }

    // shrink permits
    for (int i = costs.length; i-- > 0;) {
      ConcurrentStreamResourceWriter.setLimit(costs[i]);
      Assert.assertEquals(costs[i], ConcurrentStreamResourceWriter.getLimit(), 0);
    }

    //finite costs are capped to MAX_COST
    ConcurrentStreamResourceWriter.setLimit(0x10000);
    Assert.assertEquals(GridExporter.MAX_COST, ConcurrentStreamResourceWriter.getLimit(), 0);

    //Any positive cost, no matter how small, will require at least one permit.
    ConcurrentStreamResourceWriter.setLimit(Float.MIN_NORMAL);
    Assert.assertEquals(GridExporter.MIN_COST, ConcurrentStreamResourceWriter.getLimit(), 0);

    ConcurrentStreamResourceWriter.setLimit(0.99f / 0x10000);
    Assert.assertEquals(GridExporter.MIN_COST, ConcurrentStreamResourceWriter.getLimit(), 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetLimitWithZero() {
    ConcurrentStreamResourceWriter.setLimit(0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetLimitWithNegative() {
    ConcurrentStreamResourceWriter.setLimit(-1);
  }

  @Test(timeout = TEST_TIMEOUT)
  public void testUnlimitedDownloads()
      throws InterruptedException {
    ConcurrentStreamResourceWriter.setLimit(Float.POSITIVE_INFINITY);
    initializeCyclicBarrier(2);

    var q1 = newDownload().await();
    var q2 = newDownload().await();

    assertThat(q1.get(), nullValue());
    assertThat(q2.get(), nullValue());
  }

  @Test(timeout = TEST_TIMEOUT)
  public void testConcurrentSuccess()
      throws InterruptedException {
    ConcurrentStreamResourceWriter.setLimit(2);
    initializeCyclicBarrier(2);

    var q1 = newDownload().await();
    var q2 = newDownload().await();

    assertThat(q1.get(), nullValue());
    assertThat(q2.get(), nullValue());
  }

  @Test(timeout = TEST_TIMEOUT)
  public void testInterruptedByTimeout1()
      throws InterruptedException {
    ConcurrentStreamResourceWriter.setLimit(1);
    initializeCyclicBarrier(2);

    var q1 = newDownload().await();
    var q2 = newDownload().start();
    assertThat(barrier.getNumberWaiting(), equalTo(1));
    await(barrier);

    assertThat(q1.get(), nullValue());
    assertThat(q2.get(), interruptedByTimeout());
  }


  @Test(timeout = TEST_TIMEOUT)
  public void testInterruptedByTimeout2()
      throws InterruptedException {
    ConcurrentStreamResourceWriter.setLimit(2);
    initializeCyclicBarrier(3);

    var q1 = newDownload().await();
    var q2 = newDownload().await();
    var q3 = newDownload().withCost(2).start();
    assertThat(barrier.getNumberWaiting(), equalTo(2));
    await(barrier);

    assertThat(q1.get(), nullValue());
    assertThat(q2.get(), nullValue());
    assertThat(q3.get(), interruptedByTimeout());
  }

  @Test(timeout = TEST_TIMEOUT)
  public void testInterruptedByTimeout3()
      throws InterruptedException {
    ConcurrentStreamResourceWriter.setLimit(2);
    initializeCyclicBarrier(2);

    var q1 = newDownload().withCost(2).await();
    var q2 = newDownload().start();
    var q3 = newDownload().start();
    assertThat(barrier.getNumberWaiting(), equalTo(1));
    await(barrier);

    assertThat(q1.get(), nullValue());
    assertThat(q2.get(), interruptedByTimeout());
    assertThat(q3.get(), interruptedByTimeout());
  }

}