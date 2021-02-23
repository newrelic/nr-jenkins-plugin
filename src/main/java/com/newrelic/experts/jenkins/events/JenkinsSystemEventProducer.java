/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.jenkins.events;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.newrelic.experts.client.model.Event;

import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Node;
import hudson.model.Queue;

import jenkins.model.Jenkins;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An event producer for {@code JenkinsSystemEvent}s.
 * <p>
 * The node an queue stats portion of this code were inspired by
 * <a href="https://github.com/jenkinsci/metrics-plugin/blob/master/src/main/java/jenkins/metrics/impl/JenkinsMetricProviderImpl.java">JenkinsMetricsProviderImpl.java</a>.
 * </p>
 * 
 * @author sdewitt@newrelic.com
 * @see <a href="https://github.com/jenkinsci/metrics-plugin/blob/master/src/main/java/jenkins/metrics/impl/JenkinsMetricProviderImpl.java">JenkinsMetricsProviderImpl.java</a>
 */
@Singleton
public class JenkinsSystemEventProducer {

  private static final String CLASS_NAME = JenkinsSystemEventProducer.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

  private EventHelper eventHelper;
  
  @Inject
  public JenkinsSystemEventProducer(
      EventHelper eventHelper
  ) {
    this.eventHelper = eventHelper;
  }
  
  /**
   * Record a {@code JenkinsSystemEvent}.
   */
  public void recordEvent() {
    final String methodName = "recordEvent";
    final boolean isLoggingTrace = LOGGER.isLoggable(Level.FINE);
    final boolean isLoggingDebug = LOGGER.isLoggable(Level.FINEST);
    
    if (isLoggingTrace) {
      LOGGER.entering(CLASS_NAME, methodName);
    }

    Event event = new Event("JenkinsSystemEvent");
    Jenkins jenkins = this.eventHelper.getJenkins();
    
    event.put("inQuietDownMode", jenkins.isQuietingDown());
    
    long agentConnectedCount = Arrays.asList(jenkins.getComputers())
        .stream()
        .filter(computer -> computer.isOnline() && computer.isAcceptingTasks())
        .peek(computer -> {
          if (isLoggingTrace) {
            LOGGER.fine("Found computer" + computer.getName());
          }
        })
        .count();
    
    event.put("agentConnectedCount", (int)agentConnectedCount);
    
    NodeStats nodeStats = new NodeStats();
    
    if (jenkins.getNumExecutors() > 0) {
      updateNodeStats(jenkins, nodeStats);
    }
    
    for (Node node : jenkins.getNodes()) {
      updateNodeStats(node, nodeStats);
    }
    
    event.put("executorCount", nodeStats.executorCount);
    event.put("executorsInUse", nodeStats.executorBuilding);
    event.put("executorsFree", nodeStats.executorCount - nodeStats.executorBuilding);
    event.put("nodeCount", nodeStats.nodeCount);
    event.put("nodesOnline", nodeStats.nodeOnline);
    event.put("nodesOffline", nodeStats.nodeCount - nodeStats.nodeOnline);
    
    QueueStats queueStats = getQueueStats(jenkins);
    
    event.put("queueSize", queueStats.pending);
    event.put("queueItemCount", queueStats.pending);
    event.put("queueItemsPending", queueStats.pending);
    event.put("queueItemsWaiting", queueStats.buildable);
    event.put("queueItemsBuildable", queueStats.buildable);
    event.put("queueItemsBlocked", queueStats.blocked);
    event.put("queueItemsStuck", queueStats.stuck);
    
    this.eventHelper.recordEvent(event);
    
    if (isLoggingDebug) {
      LOGGER.finest(String.format("Added Jenkins system event"));
    }
        
    if (isLoggingTrace) {
      LOGGER.exiting(CLASS_NAME, methodName);
    }
  }
  
  private QueueStats getQueueStats(Jenkins jenkins) {
    QueueStats queueStats = new QueueStats();
    Queue queue = jenkins.getQueue();
    
    if (queue == null) {
      return queueStats;
    }
    
    queueStats.pending = queue == null ? 0 : queue.getPendingItems().size();
    
    for (Queue.Item i : queue.getItems()) {
      if (i == null) {
        continue;
      }
      
      queueStats.length += 1;
      if (i.isBlocked()) {
        queueStats.blocked += 1;
      }
      if (i.isBuildable()) {
        queueStats.buildable += 1;
      }
      if (i.isStuck()) {
        queueStats.stuck += 1;
      }
    }
   
    return queueStats;
  }
  
  class QueueStats {
    int length = 0;
    int blocked = 0;
    int buildable = 0;
    int stuck = 0;
    int pending = 0;
  }
  
  private void updateNodeStats(Node node, NodeStats stats) {
    stats.nodeCount += 1;
    
    Computer computer = node.toComputer();
    
    if (computer == null) {
      return;
    }
    
    if (!computer.isOffline()) {
      stats.nodeOnline += 1;
      for (Executor e : computer.getExecutors()) {
        stats.executorCount += 1;
        if (!e.isIdle()) {
          stats.executorBuilding += 1;
        }
      }
    }
  }
  
  class NodeStats {
    int nodeCount = 0;
    int nodeOnline = 0;
    int executorCount = 0;
    int executorBuilding = 0;
  }
 
}
