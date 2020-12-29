package com.newrelic.experts.jenkins.extensions;

import com.google.inject.Inject;

import com.newrelic.experts.jenkins.EventRecorder;

import hudson.Extension;
import hudson.model.PeriodicWork;
import hudson.model.Queue;

import java.util.concurrent.TimeUnit;

@Extension
public class QueueRunPublisher extends PeriodicWork {

  private EventRecorder recorder;

  private final Queue queue = Queue.getInstance();

  /**
   * Create a new {@link EventRunListener}.
   * <p>
   * This public no-argument constructors is required in order for SezPoz
   * to work properly.  If you call it directly it will thrown an
   * {@link UnsupportedOperationException}.
   * </p>
   */
  public QueueRunPublisher() {
    throw new UnsupportedOperationException(
      "Public no-argument constructor is required but not supported."
    );
  }

  @Inject
  public QueueRunPublisher(EventRecorder eventRecorder) {
    this.recorder = eventRecorder;
  }

  @Override
  public long getRecurrencePeriod() {
    return TimeUnit.MINUTES.toMillis(1);
  }

  @Override
  protected void doRun() throws Exception {
    this.recorder.recordQueueEvent(queue);
  }
}
