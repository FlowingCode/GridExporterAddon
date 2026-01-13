/*-
 * #%L
 * Grid Exporter Add-on
 * %%
 * Copyright (C) 2022 - 2026 Flowing Code
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
package com.flowingcode.vaadin.addons.gridexporter.test;

import static org.apache.commons.io.output.NullOutputStream.NULL_OUTPUT_STREAM;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.flowingcode.vaadin.addons.gridexporter.ConfigurableConcurrentStreamResourceWriter;
import com.flowingcode.vaadin.addons.gridexporter.GridExporter;
import com.flowingcode.vaadin.addons.gridexporter.GridExporterConcurrentSettings;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.StreamResourceWriter;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import java.io.IOException;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("serial")
public class ConcurrentExportTests {

  private static final int TEST_TIMEOUT = 10000;

  private static Matcher<Throwable> throwsInterruptedByTimeout() {
    return Matchers.instanceOf(InterruptedByTimeoutException.class);
  }

  private class ConcurrentStreamResourceWriter
      extends ConfigurableConcurrentStreamResourceWriter {

    private boolean interruptedByTimeout;
    private boolean accepted;
    private boolean finished;

    public ConcurrentStreamResourceWriter(StreamResourceWriter delegate) {
      super(delegate);
    }

    @Override
    protected void onTimeout() {
      interruptedByTimeout = true;
    }

    @Override
    protected void onAccept() {
      accepted = true;
    }

    @Override
    protected void onFinish() {
      finished = true;
    }

  }

  private CyclicBarrier barrier;
  private final static List<Thread> threads = new ArrayList<>();
  private final static Lock lock = new ReentrantLock();

  private void initializeCyclicBarrier(int parties) {
    barrier = new CyclicBarrier(parties);
  }

  @Before
  public void before() {
    GridExporterConcurrentSettings.setFailOnUiChange(false);
    barrier = null;
    if (!lock.tryLock()) {
      throw new IllegalStateException(
          this.getClass().getSimpleName() + "test cannot be run in parallel");
    }
    threads.forEach(Thread::interrupt);
    threads.clear();
  }

  @After
  public void after() {
    lock.unlock();
  }

  @SuppressWarnings("unchecked")
  private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
    throw (E) e;
  }

  private interface MockDownload {
    MockDownload withTimeout(long timeout);

    MockDownload withCost(float cost);

    void detach();

    Throwable get() throws InterruptedException;

    MockDownload await() throws InterruptedException;

    MockDownload start();

    boolean wasInterruptedByTimeout();

    boolean isFinished();

    boolean isAccepted();
  }

  private Thread newThread(Runnable target) {
    Thread thread = new Thread(target);
    threads.add(thread);
    return thread;
  }

  private MockDownload newDownload() {

    CyclicBarrier barrier = this.barrier;
    CountDownLatch latch = new CountDownLatch(1);

    ConcurrentStreamResourceWriter writer =
        new ConcurrentStreamResourceWriter((stream, session) -> {
          latch.countDown();
          await(barrier);
        });

    writer.setUi(new UI());
    Exchanger<Throwable> exchanger = new Exchanger<>();

    Thread thread = newThread(() -> {

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
        return;
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

      @Override
      public void detach() {
        writer.setUi(null);
      }

      @Override
      public boolean wasInterruptedByTimeout() {
        return writer.interruptedByTimeout;
      }

      @Override
      public boolean isAccepted() {
        return writer.accepted;
      }

      @Override
      public boolean isFinished() {
        return writer.finished;
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
    assertThat(q2.get(), throwsInterruptedByTimeout());
    assertTrue(q2.wasInterruptedByTimeout());
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
    assertThat(q3.get(), throwsInterruptedByTimeout());
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
    assertThat(q2.get(), throwsInterruptedByTimeout());
    assertThat(q3.get(), throwsInterruptedByTimeout());
  }


  @Test(timeout = TEST_TIMEOUT)
  public void testAcceptFinish() throws InterruptedException {
    ConcurrentStreamResourceWriter.setLimit(2);
    initializeCyclicBarrier(2);
    var q1 = newDownload().await();
    assertTrue("Download has not been accepted", q1.isAccepted());
    assertFalse("Download has finished too early", q1.isFinished());
    var q2 = newDownload().await();
    assertTrue("Download has not been accepted", q2.isAccepted());
    assertThat(q1.get(), nullValue());
    assertThat(q2.get(), nullValue());
    assertTrue("Download has not finished", q1.isFinished());
    assertTrue("Download has not finished", q2.isFinished());
  }

  @Test(timeout = TEST_TIMEOUT)
  public void testFailOnUiClose() throws InterruptedException, BrokenBarrierException {
    GridExporterConcurrentSettings.setFailOnUiChange(true);
    ConcurrentStreamResourceWriter.setLimit(1);

    initializeCyclicBarrier(2);
    CyclicBarrier b1 = barrier;
    var q1 = newDownload().await();
    assertTrue("Download has not been accepted", q1.isAccepted());
    assertFalse("Download has finished too early", q1.isFinished());

    initializeCyclicBarrier(2);
    var q2 = newDownload().withTimeout(TEST_TIMEOUT).start();
    assertTrue("Download has not been accepted", q1.isAccepted());
    assertFalse("Download has finished too early", q1.isFinished());

    // detach while the semaphore is held by q1
    q2.detach();

    // await on b1 so that q1 releases the semaphore
    b1.await();
    assertThat(q1.get(), nullValue());

    // with "FailOnUiChange" the other thread never arrives at the barrier
    assertThat(barrier.getNumberWaiting(), equalTo(0));
    assertThat(q2.get(), instanceOf(IOException.class));

  }

}
