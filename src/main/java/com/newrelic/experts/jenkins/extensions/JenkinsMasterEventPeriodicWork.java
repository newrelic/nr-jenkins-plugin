/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.jenkins.extensions;

import com.google.inject.Inject;

import com.newrelic.experts.client.model.JenkinsMasterEvent;
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
public class JenkinsMasterEventPeriodicWork
    extends PeriodicWork {

  private static final String CLASS_NAME = JenkinsMasterEventPeriodicWork.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

  private final EventRecorder recorder;

  /**
   * Create a new {@link JenkinsMasterEventPeriodicWork}.
   * <p>
   * This public no-argument constructors is required in order for SezPoz
   * to work properly.  If you call it directly it will thrown an
   * {@link UnsupportedOperationException}.
   * </p>
   */
  public JenkinsMasterEventPeriodicWork() {
    throw new UnsupportedOperationException(
      "Public no-argument constructor is required but not supported."
    );
  }

  /**
   * Create a new {@link JenkinsMasterEventPeriodicWork} using constructor based DI
   * to pass in dependencies.
   *
   * @param recorder the {@link EventRecorder} to send events too.
   */
  @Inject
  public JenkinsMasterEventPeriodicWork(
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
    synchronized (this.recorder) {

      final String methodName = "recordBuildEvent";
      final boolean isLoggingTrace = LOGGER.isLoggable(Level.FINE);

      if (isLoggingTrace)
      {
        LOGGER.entering(CLASS_NAME, methodName);
      }

      JenkinsMasterEvent jenkinsMasterEvent = new JenkinsMasterEvent();
      jenkinsMasterEvent.setQuietDownMode(Jenkins.getInstance().isQuietingDown());
      long count = Arrays.asList(Jenkins.getInstance().getComputers())
          .stream()
          .filter(computer -> computer.isOnline() && computer.isAcceptingTasks())
          .peek(computer -> {
              if (isLoggingTrace) {
                LOGGER.fine("Found computer" + computer.getName());
              }
          })
          .count();
      jenkinsMasterEvent.setAgentConnectedCount((int) count);
      this.recorder.recordJenkinsMasterEvent(jenkinsMasterEvent);

      if (isLoggingTrace)
      {
        LOGGER.exiting(CLASS_NAME, methodName);
      }
    }
  }

}
