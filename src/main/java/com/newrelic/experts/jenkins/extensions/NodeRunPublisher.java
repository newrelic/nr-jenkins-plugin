package com.newrelic.experts.jenkins.extensions;

import com.google.inject.Inject;

import com.newrelic.experts.jenkins.EventRecorder;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.PeriodicWork;
import jenkins.model.Jenkins;

import java.util.concurrent.TimeUnit;

@Extension
public class NodeRunPublisher extends PeriodicWork {

  private final EventRecorder recorder;

  /**
   * Create a new {@link EventRunListener}.
   * <p>
   * This public no-argument constructors is required in order for SezPoz
   * to work properly.  If you call it directly it will thrown an
   * {@link UnsupportedOperationException}.
   * </p>
   */
  public NodeRunPublisher() {
    throw new UnsupportedOperationException(
      "Public no-argument constructor is required but not supported."
    );
  }

  @Inject
  public NodeRunPublisher(EventRecorder eventRecorder) {
    this.recorder = eventRecorder;
  }

  @Override
  public long getRecurrencePeriod() {
    return TimeUnit.MINUTES.toMillis(1);
  }

  @Override
  protected void doRun() throws Exception {
    Jenkins jenkins = Jenkins.getInstance();
    Computer[] computers = jenkins.getComputers();

    for (Computer computer : computers) {
      this.recorder.recordNodeEvent(computer);
    }
  }
}
