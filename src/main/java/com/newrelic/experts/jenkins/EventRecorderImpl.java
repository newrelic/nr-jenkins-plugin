/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.jenkins;

import com.newrelic.experts.client.model.Event;
import com.newrelic.experts.jenkins.extensions.EventConfigJobProperty;
import com.newrelic.experts.jenkins.extensions.KeyValuePair;

import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.labels.LabelAtom;
import hudson.util.TagCloud;
import hudson.util.VersionNumber;

import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.support.steps.ExecutorStepExecution;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of the {@link EventRecorder} that utilizes a simple
 * {@link java.util.LinkedList} to store the events.
 * 
 * @author Scott DeWitt (sdewitt@newrelic.com)
 */
public class EventRecorderImpl implements EventRecorder {

  private static final String CLASS_NAME = EventRecorderImpl.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

  private List<Event> events;
  
  public EventRecorderImpl() {
    this.events = new LinkedList<Event>();
  }
  
  private void setLabels(Event event, String attributeName, Node node) {
    List<String> labels = new ArrayList<String>();
    
    for (TagCloud<LabelAtom>.Entry atom : node.getLabelCloud()) {
      labels.add(atom.item.getDisplayName());
    }
    
    event.put(attributeName, String.join("|", labels));
  }
  
  private void setHostname(Event event, String attributeName, Node node) {
    Computer computer = node.toComputer();
    if (computer != null) {
      try {
        event.put(attributeName, computer.getHostName());
      } catch (IOException | InterruptedException exc) {
        LOGGER.log(Level.WARNING, String.format(
            "Could not get node hostname: %s",
            exc.getMessage()
        ), exc);
      }
    }
  }
  
  private void internalRecordBuildEvent(
      BuildEventType eventType,
      Run<? extends Job<?, ?>, ? extends Run<?, ?>> build,
      TaskListener listener
  ) {
    final String methodName = "internalRecordBuildEvent";
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
    setLabels(event, "buildAgentLabels", buildAgent);
    setHostname(event, "buildAgentHost", buildAgent);
    
    Jenkins jenkins = Jenkins.getInstance();
    VersionNumber ver = Jenkins.getVersion();
    event.put("provider", "Jenkins");
    event.put("providerVersion", (ver != null ? ver.toString() : "unknown"));
    setLabels(event, "jenkinsMasterLabels", jenkins);
    setHostname(event, "jenkinsMasterHost", jenkins);
    setCustomAttributes(event, job, build, listener);
    
    this.events.add(event);
  
    if (isLoggingDebug) {
      LOGGER.finest(String.format(
          "Added event with type %s for job %s and build %s",
          eventType,
          job.getDisplayName(),
          build.getDisplayName()
      ));
    }
    
    if (isLoggingTrace) {
      LOGGER.exiting(CLASS_NAME, methodName);
    }
  }
  
  private void internalRecordQueueEvent(
    Queue queue
  ) {
    final Queue.Item[] items = queue.getItems();
    final String methodName = "internalRecordQueueEvent";
    final boolean isLoggingTrace = LOGGER.isLoggable(Level.FINE);
    final boolean isLoggingDebug = LOGGER.isLoggable(Level.FINEST);

    if (isLoggingTrace) {
      LOGGER.entering(CLASS_NAME, methodName, new Object[] { queue });
    }

    int blocked = 0;
    int pending = 0;
    int stuck = 0;

    for (Queue.Item item : items) {
      String jobName = "unknown";
      Queue.Task task = item.task;

      if (task instanceof FreeStyleProject) {
        jobName = task.getFullDisplayName();
      } else if (task instanceof ExecutorStepExecution.PlaceholderTask) {
        Run<?, ?> run = ((ExecutorStepExecution.PlaceholderTask) task).runForDisplay();
        if (run != null) {
          jobName = run.getParent().getFullName();
        } else {
          jobName = "unknown";
        }
      }

      Node buildAgent = Jenkins.getInstance();

      Event event = new Event("AppQueueItemEvent");
      event.put("jobName", jobName);
      event.put("taskUrl", task.getUrl());
      event.put("isStuck", item.isStuck());
      event.put("isBuildable", item.isBuildable());
      event.put("isBlocked", item.isBlocked());
      event.put("isPending", queue.isPending(task));
      event.put("message", item.getWhy());
      event.put("since", item.getInQueueSince());
      event.put("queuedAgentName", buildAgent.getDisplayName());
      event.put("queueAgentDesc", buildAgent.getNodeDescription());
      event.put("labels", buildAgent.getLabelString());
      setLabels(event, "queuedAgentLabels", buildAgent);
      setHostname(event, "queueAgentHost", buildAgent);

      Jenkins jenkins = Jenkins.getInstance();
      setLabels(event, "jenkinsMasterLabels", jenkins);
      setHostname(event, "jenkinsMasterHost", jenkins);

      if (item.isStuck()) {
        stuck++;
      }

      if (item.isBlocked()) {
        blocked++;
      }
      if (queue.isPending(task)) {
        pending++;
      }

      this.events.add(event);
    }

    Event eventQueue = new Event("AppQueueEvent");
    eventQueue.put("stuckItems", stuck);
    eventQueue.put("buildableItems", queue.countBuildableItems());
    eventQueue.put("blockedItems", blocked);
    eventQueue.put("pendingItems", pending);

    this.events.add(eventQueue);

    if (isLoggingDebug) {
      LOGGER.finest(String.format("Added queue %s", queue));
    }

    if (isLoggingTrace) {
      LOGGER.exiting(CLASS_NAME, methodName);
    }
  }
  
  @Override
  public void recordBuildEvent(
      BuildEventType eventType,
      Run<? extends Job<?, ?>, ? extends Run<?, ?>> build,
      TaskListener listener
  ) {
    this.internalRecordBuildEvent(eventType, build, listener);
  }
  
  @Override
  public void recordBuildEvent(
      BuildEventType eventType,
      Run<? extends Job<?, ?>, ? extends Run<?, ?>> build
  ) {
    this.internalRecordBuildEvent(eventType, build, null);
  }

  @Override
  public void recordAppDeploymentEvent(
      String appId,
      String revision,
      String changeLog,
      String description,
      String user
  ) {
    final String methodName = "recordBuildEvent";
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
    
    this.events.add(event);
  
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

  @Override
  public void recordQueueEvent(Queue queue) {
    this.internalRecordQueueEvent(queue);
  }

  @Override
  public void recordNodeEvent(Computer computer) {
    Event event = new Event("AppNodeEvent");
    System.out.println("Entra");
    try {
      event.put("hostname", computer.getHostName());
      event.put("nodeUrl", computer.getUrl());
      event.put("idle", computer.countIdle());
      event.put("busy", computer.countBusy());
      System.out.println(event.toString());

    } catch (IOException exception) {
      exception.printStackTrace();
    } catch (InterruptedException exceptionI) {
      exceptionI.printStackTrace();
    }
    this.events.add(event);
  }

  @Override
  public Event[] popEvents() {
    Event[] eventAry = new Event[this.events.size()];
    eventAry = this.events.toArray(eventAry);
    this.events.clear();
    return eventAry;
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
    return Messages.EventRecorderImpl_BuildMessage(
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
