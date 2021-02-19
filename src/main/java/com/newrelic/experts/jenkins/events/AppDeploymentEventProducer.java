/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.jenkins.events;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.newrelic.experts.client.model.Event;
import com.newrelic.experts.jenkins.EventRecorder;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An event producer for {@code AppBuildEvent}s.
 * 
 * @author sdewitt@newrelic.com
 */
@Singleton
public class AppDeploymentEventProducer {

  private static final String CLASS_NAME = AppDeploymentEventProducer.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

  private EventRecorder recorder;
  private EventHelper eventHelper;
  
  @Inject
  public AppDeploymentEventProducer(
      EventRecorder recorder,
      EventHelper eventHelper
  ) {
    this.recorder = recorder;
    this.eventHelper = eventHelper;
  }
  
  /**
   * Record a deployment event as part of a post build task.
   * 
   * @param appId the New Relic application ID.
   * @param revision the revision value from the post build task.
   * @param changeLog the change log value from the post build task.
   * @param description the description value from the post build task.
   * @param user the user value from the post build task.
   */
  public void recordEvent(
      String appId,
      String revision,
      String changeLog,
      String description,
      String user
  ) {
    final String methodName = "recordEvent";
    final boolean isLoggingTrace = LOGGER.isLoggable(Level.FINE);
    final boolean isLoggingDebug = LOGGER.isLoggable(Level.FINEST);
    
    if (isLoggingTrace) {
      LOGGER.entering(CLASS_NAME, methodName, new Object[] {
          appId
      });
    }

    Event event = new Event("AppDeploymentEvent");
    event.put("appId", appId);
    event.put("revision", revision);
    
    if (changeLog != null) {
      event.put("changelog", changeLog);
    }
    if (description != null) {
      event.put("description", description);
    }
    if (user != null) {
      event.put("user", user);
    }
    
    this.eventHelper.setCommonAttributes(event);
    this.recorder.recordEvent(event);
  
    if (isLoggingDebug) {
      LOGGER.finest(String.format(
          "Added application deployment event for appId %s",
          appId
      ));
    }
    
    if (isLoggingTrace) {
      LOGGER.exiting(CLASS_NAME, methodName);
    }
  }
}
