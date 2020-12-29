/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.jenkins;

import com.newrelic.experts.client.model.Event;

import hudson.model.Computer;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;

/**
 * A very simple data sink object for temporarily "queuing" up Insights events.
 * 
 * @author Scott DeWitt (sdewitt@newrelic.com)
 */
public interface EventRecorder {
  
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
  
  /**
   * Record an Insights event for the given Jenkins {@link BuildEventType}.
   * <p>
   * This version of the method is called when a {@link TaskListener} is
   * available.  Listeners are not available at all stages of a Jenkins build.
   * For example if the build is being finalized or deleted, the listener
   * object is no longer available.  The availability of the listener affects
   * the ability to perform certain activities such as token replacement.
   * </p>
   * 
   * @param eventType the build event type.
   * @param build the Jenkins run instance for an active run of a job.
   * @param listener the run listener for this run instance.
   */
  void recordBuildEvent(
      BuildEventType eventType,
      Run<? extends Job<?, ?>, ? extends Run<?, ?>> build,
      TaskListener listener
  );

  /**
   * Record an Insights event for the given Jenkins {@link BuildEventType}.
   * <p>
   * This version of the method is called when a {@link TaskListener} is
   * not available.
   * </p>
   * 
   * @param eventType the build event type.
   * @param build the Jenkins run instance for an active run of a job.
   * @see #recordBuildEvent(BuildEventType, Run, TaskListener)
   */
  void recordBuildEvent(
      BuildEventType eventType,
      Run<? extends Job<?, ?>, ? extends Run<?, ?>> build
  );

  /**
   * Record custom Insights "application deployment event".
   * 
   * @param appId the APM application ID.
   * @param revision the deployment marker "revision" property.
   * @param changeLog the deployment marker "changelog" property.
   * @param description the deployment marker "description" property.
   * @param user the deployment marker "user" property.
   */
  void recordAppDeploymentEvent(
      String appId,
      String revision,
      String changeLog,
      String description,
      String user
  );

  /**
   * Record custom Insights for queue status.
   * 
   * @param queue
   */
  void recordQueueEvent(
      Queue queue
  );

  /**
   * Record custom Insights for computer status.
   * 
   * @param computer
   */
  void recordNodeEvent(Computer computer);

  /**
   * Drain all events out of this recorder.
   * <p>
   * "Pop" is really not the right verb here since it's not a stack.  But this
   * operation is non-idempotent.  It will actually remove all the items in
   * the internal object storage.
   * </p>
   * 
   * @return all events which have been recorded since the last call to this
   *        method.
   */
  Event[] popEvents();
}
