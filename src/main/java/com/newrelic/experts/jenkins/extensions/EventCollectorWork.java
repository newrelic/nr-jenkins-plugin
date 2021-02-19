/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.jenkins.extensions;

import com.google.inject.Inject;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.newrelic.experts.client.api.NewRelicClient;
import com.newrelic.experts.client.api.NewRelicClientException;
import com.newrelic.experts.client.model.Event;
import com.newrelic.experts.jenkins.EventRecorder;
import com.newrelic.experts.jenkins.JenkinsUtils;

import hudson.Extension;
import hudson.model.PeriodicWork;
import hudson.util.Secret;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Jenkins {@link PeriodicWork} object that implements an event harvest
 * cycle, by default running once every minute.
 * 
 * @author Scott DeWitt (sdewitt@newrelic.com)
 */
@Extension
public class EventCollectorWork extends PeriodicWork {
 
  private static final String CLASS_NAME = EventCollectorWork.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

  private NewRelicGlobalConfiguration nrjConfig;
  private EventRecorder recorder;
  private NewRelicClient client;
  
  /**
   * Create a new {@link EventCollectorWork}.
   * <p>
   * This public no-argument constructors is required in order for SezPoz
   * to work properly.  If you call it directly it will thrown an
   * {@link UnsupportedOperationException}.
   * </p>
   */
  public EventCollectorWork() {
    throw new UnsupportedOperationException(
      "Public no-argument constructor is required but not supported."
    );
  }  
  
  /**
   * Create a new {@link EventCollectorWork} using constructor based DI
   * to pass in dependencies.
   * 
   * @param nrjConfig the {@link NewRelicGlobalConfiguration} to use.
   * @param recorder the {@link EventRecorder} to send events too.
   * @param client the {@link NewRelicClient} to use.
   */
  @Inject
  public EventCollectorWork(
      NewRelicGlobalConfiguration nrjConfig,
      EventRecorder recorder,
      NewRelicClient client
  ) {
    this.nrjConfig = nrjConfig;
    this.recorder = recorder;
    this.client = client;
  }
  
  @Override
  public long getRecurrencePeriod() {
    return this.nrjConfig.getEventHarvestInterval() * 1000;
  } 
  
  @Override
  protected void doRun() throws Exception {
    final String methodName = "doRun";
    final boolean isLoggingTrace = LOGGER.isLoggable(Level.FINE);
    final boolean isLoggingDebug = LOGGER.isLoggable(Level.FINEST);

    if (isLoggingTrace) {
      LOGGER.entering(CLASS_NAME, methodName);
    }

    Event[] events = this.recorder.popEvents();

    if (events.length == 0) {
      if (isLoggingDebug) {
        LOGGER.logp(Level.FINE, CLASS_NAME, methodName,
            "RETURN EARLY No events to send."
        );
      }
      return;
    }
    
    StandardUsernamePasswordCredentials credentialsId =
        this.nrjConfig.getInsightsInsertCredentials();
    if (credentialsId == null) {
      if (isLoggingTrace) {
        LOGGER.logp(Level.FINE, CLASS_NAME, methodName,
            "RETURN EARLY No credentials."
        );
      }
      return;
    }

    try {
      this.client.recordEvents(
          JenkinsUtils.createClientConnectionConfig(this.client),
          credentialsId.getUsername(),
          Secret.toString(credentialsId.getPassword()),
          events
      );
    } catch (NewRelicClientException nrce) {
      LOGGER.log(Level.SEVERE, "Failed to post events");
    }
    
    if (isLoggingTrace) {
      LOGGER.exiting(CLASS_NAME, methodName);
    }
  }

}
