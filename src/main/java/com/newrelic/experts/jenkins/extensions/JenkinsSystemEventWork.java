/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.jenkins.extensions;

import com.google.inject.Inject;

import com.newrelic.experts.client.api.NewRelicClient;
import com.newrelic.experts.jenkins.EventRecorder;
import com.newrelic.experts.jenkins.events.JenkinsSystemEventProducer;

import hudson.Extension;
import hudson.model.PeriodicWork;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Jenkins {@link PeriodicWork} object that produces {@code JenkinsSystemEvent}s.
 * <p>
 * Events are produced every 15 seconds by default.
 * </p>
 * 
 * @author Scott DeWitt (sdewitt@newrelic.com)
 */
@Extension
public class JenkinsSystemEventWork extends PeriodicWork {
 
  private static final String CLASS_NAME = JenkinsSystemEventWork.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

  private NewRelicGlobalConfiguration nrjConfig;
  private JenkinsSystemEventProducer producer;
  
  /**
   * Create a new {@link JenkinsSystemEventWork}.
   * <p>
   * This public no-argument constructors is required in order for SezPoz
   * to work properly.  If you call it directly it will thrown an
   * {@link UnsupportedOperationException}.
   * </p>
   */
  public JenkinsSystemEventWork() {
    throw new UnsupportedOperationException(
      "Public no-argument constructor is required but not supported."
    );
  }  
  
  /**
   * Create a new {@link JenkinsSystemEventWork} using constructor based DI
   * to pass in dependencies.
   * 
   * @param nrjConfig the {@link NewRelicGlobalConfiguration} to use.
   * @param producer the {@link JenkinsSystemEventProducer} to use to sample system metrics.
   */
  @Inject
  public JenkinsSystemEventWork(
      NewRelicGlobalConfiguration nrjConfig,
      JenkinsSystemEventProducer producer
  ) {
    this.nrjConfig = nrjConfig;
    this.producer = producer;
  }
  
  @Override
  public long getRecurrencePeriod() {
    return this.nrjConfig.getSystemSampleInterval() * 1000;
  } 
  
  @Override
  protected void doRun() throws Exception {
    final String methodName = "doRun";
    final boolean isLoggingTrace = LOGGER.isLoggable(Level.FINE);

    if (isLoggingTrace) {
      LOGGER.entering(CLASS_NAME, methodName);
    }

    this.producer.recordEvent();
    
    if (isLoggingTrace) {
      LOGGER.exiting(CLASS_NAME, methodName);
    }
  }

}
