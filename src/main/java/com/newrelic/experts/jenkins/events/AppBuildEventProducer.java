/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.jenkins.events;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.newrelic.experts.client.model.Event;
import com.newrelic.experts.jenkins.JenkinsUtils;
import com.newrelic.experts.jenkins.Messages;
import com.newrelic.experts.jenkins.extensions.EventConfigJobProperty;
import com.newrelic.experts.jenkins.extensions.KeyValuePair;

import hudson.model.AbstractBuild;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;

import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An event producer for {@code AppBuildEvent}s.
 * 
 * @author sdewitt@newrelic.com
 */
@Singleton
public class AppBuildEventProducer {
  /**
   * An enumeration of Jenkins build phases.
   * 
   * @author Scott DeWitt (sdewitt@newrelic.com)
   */
  public enum BuildEventType {
    INITIALIZED,
    STARTED,
    COMPLETED,
    FINALIZED,
    DELETED
  }

  private static final String CLASS_NAME = AppBuildEventProducer.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

  private EventHelper eventHelper;
  
  @Inject
  public AppBuildEventProducer(
      EventHelper eventHelper
  ) {
    this.eventHelper = eventHelper;
  }
  
  /**
   * Record a {@code AppBuildEvent} for the given build {@code eventType}.
   * 
   * @param eventType the build event type.
   * @param build the build currently running.
   * @param listener the task listener or {@code null}.
   */
  public void recordEvent(
      BuildEventType eventType,
      Run<? extends Job<?, ?>, ? extends Run<?, ?>> build,
      TaskListener listener
  ) {
    final String methodName = "recordEvent";
    final boolean isLoggingTrace = LOGGER.isLoggable(Level.FINE);
    final boolean isLoggingDebug = LOGGER.isLoggable(Level.FINEST);
    
    if (isLoggingTrace) {
      LOGGER.entering(CLASS_NAME, methodName, new Object[] { eventType });
    }
    
    Job<?, ?> job = build.getParent();
    
    Event event = new Event("AppBuildEvent");
    
    event.put("jobUrl", job.getUrl());
    event.put("jobName", job.getDisplayName());
    event.put("jobFullName", job.getFullDisplayName());
    event.put("buildId", build.getId());
    event.put("buildUrl", build.getUrl());
    event.put("buildName", build.getDisplayName());
    event.put("buildFullName", build.getFullDisplayName());
    event.put("buildEventType", eventType.toString().toLowerCase());
    event.put("buildQueueId", build.getQueueId());
    event.put("buildMessage", buildBuildMessage(eventType, job, build));
    
    Result result = build.getResult();
    if (result != null) {
      event.put("buildResult", result.toString());
    }
    
    if (eventType == BuildEventType.STARTED) {
      long scheduled = build.getTimeInMillis();
      long started = build.getStartTimeInMillis();
      event.put("buildScheduled", scheduled);
      event.put("buildStarted", started);
      event.put(
          "buildStartDelay",
          (started - scheduled) / 1000
      );
    } else if (eventType == BuildEventType.FINALIZED) {
      event.put("buildDuration", build.getDuration());
      event.put("buildStatusSummary", build.getBuildStatusSummary().message);
    }
    
    @SuppressWarnings("deprecation")
    Node buildAgent = Jenkins.getInstance();
    if (build instanceof AbstractBuild) {
      Node agent = (
          (AbstractBuild<? extends Job<?, ?>, ? extends Run<?, ?>>)build
      ).getBuiltOn();
      if (agent != null) {
        buildAgent = agent;
      }
    }
    event.put("buildAgentName", buildAgent.getDisplayName());
    event.put("buildAgentDesc", buildAgent.getNodeDescription());
    this.eventHelper.setLabels(event, "buildAgentLabels", buildAgent);
    this.eventHelper.setHostname(event, "buildAgentHost", buildAgent);
    setCustomAttributes(event, job, build, listener);
    
    this.eventHelper.recordEvent(event);

    if (isLoggingDebug) {
      LOGGER.finest(String.format(
          "Added application build event with type %s for job %s and build %s",
          eventType,
          job.getDisplayName(),
          build.getDisplayName()
      ));
    }
        
    if (isLoggingTrace) {
      LOGGER.exiting(CLASS_NAME, methodName);
    }
  }
  
  /**
   * Build the string used as the "buildMessage" attribute.
   *  
   * @param eventType the build event type.
   * @param job the Jenkins job.
   * @param build the Jenkins run instance for an active run of the job.
   * @return a localized build message.
   */
  public String buildBuildMessage(
      BuildEventType eventType,
      Job<?, ?> job, 
      Run<? extends Job<?, ?>, ? extends Run<?, ?>> build
  ) {
    // xyz build full display name for job full display name
    String status = eventType.name().toLowerCase();
    status = status.substring(0, 1).toUpperCase() + status.substring(1);
    return Messages.AppBuildEventProducer_BuildMessage(
        status,
        build.getFullDisplayName(),
        job.getFullDisplayName()
    );
  }
  
  /**
   * Set any custom attributes on {@code event} that have been configured in
   * the {@code job} configuration.
   * 
   * @param event the custom application build event being configured.
   * @param job the Jenkins job.
   * @param build the Jenkins run instance for an active run of the job.
   * @param listener the run listener for this run instance.
   */
  public void setCustomAttributes(
      Event event,
      Job<?, ?> job,
      Run<? extends Job<?, ?>, ? extends Run<?, ?>> build,
      TaskListener listener
  ) {
    EventConfigJobProperty prop = job.getProperty(
        EventConfigJobProperty.class
    );
    if (prop != null) {
      List<KeyValuePair> customAttributes = prop.getCustomAttributes();
      if (customAttributes != null && !customAttributes.isEmpty()) {
        for (KeyValuePair pair : customAttributes) {
          if (listener == null) {
            String value = pair.getValue();
            if (!JenkinsUtils.VARIABLE.matcher(value).find()) {
              event.put(pair.getName(), value);
            }
            continue;
          }
          try {
            event.put(
                pair.getName(),
                JenkinsUtils.expandTokens(build, listener, pair.getValue())
            );
          } catch (IOException | InterruptedException exc) {
            LOGGER.log(Level.WARNING, String.format(
                "Could not expand tokens: %s",
                exc.getClass().getName()
            ));
          }
        }
      }
    }
  }
}
