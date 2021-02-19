/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.experts.jenkins.extensions;

import com.google.inject.Inject;

import com.newrelic.experts.jenkins.EventRecorder;
import hudson.Extension;
import hudson.model.PeriodicWork;
import jenkins.model.Jenkins;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Jenkins {@link PeriodicWork} object that adds JenkinsMaster events for
 * event collection, by default running once every minute.
 *
 * @author Dan Alvizu (alvizu@gmail.com)
 */
@Extension
public class JenkinsMasterEventWork
    extends PeriodicWork {

  private static final String CLASS_NAME = JenkinsMasterEventWork.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

  private final EventRecorder recorder;

  /**
   * Create a new {@link JenkinsMasterEventWork}.
   * <p>
   * This public no-argument constructors is required in order for SezPoz
   * to work properly.  If you call it directly it will thrown an
   * {@link UnsupportedOperationException}.
   * </p>
   */
  public JenkinsMasterEventWork() {
    throw new UnsupportedOperationException(
      "Public no-argument constructor is required but not supported."
    );
  }

  /**
   * Create a new {@link JenkinsMasterEventWork} using constructor based DI
   * to pass in dependencies.
   *
   * @param recorder the {@link EventRecorder} to send events too.
   */
  @Inject
  public JenkinsMasterEventWork(
      EventRecorder recorder
  ) {
    this.recorder = recorder;
  }

  @Override
  public long getRecurrencePeriod() {
    return 60000;
  }

  @Override
  protected void doRun() throws Exception {

    final String methodName = "recordBuildEvent";
    final boolean isLoggingTrace = LOGGER.isLoggable(Level.FINE);

    if (isLoggingTrace) {
      LOGGER.entering(CLASS_NAME, methodName);
    }

    boolean isQuietDownMode = Jenkins.getInstance().isQuietingDown();
    long agentCount = Arrays.asList(Jenkins.getInstance().getComputers())
        .stream()
        .filter(computer -> computer.isOnline() && computer.isAcceptingTasks())
        .peek(computer -> {
          if (isLoggingTrace) {
            LOGGER.fine("Found computer" + computer.getName());
          }
        })
        .count();
    this.recorder.recordJenkinsMasterEvent(isQuietDownMode, agentCount);

    if (isLoggingTrace) {
      LOGGER.exiting(CLASS_NAME, methodName);
    }
  }

}
