/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.jenkins.extensions;

import com.google.inject.Inject;

import com.newrelic.experts.jenkins.EventRecorder;
import com.newrelic.experts.jenkins.EventRecorder.BuildEventType;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

/**
 * Implementation of a {@link RunListener} for creating events from build
 * notifications.
 * 
 * @author Scott DeWitt (sdewitt@newrelic.com)
 */
@Extension
public class EventRunListener extends RunListener<Run<?, ?>> {

  private EventRecorder recorder;

  /**
   * Create a new {@link EventRunListener}.
   * <p>
   * This public no-argument constructors is required in order for SezPoz
   * to work properly.  If you call it directly it will thrown an
   * {@link UnsupportedOperationException}.
   * </p>
   */
  public EventRunListener() {
    throw new UnsupportedOperationException(
      "Public no-argument constructor is required but not supported."
    );
  }

  @Inject
  public EventRunListener(EventRecorder test) {
    this.recorder = test;
  }
  
  /**
   * Return {@code true} if the configuration of the job for the given
   * {@code build} has the custom {@link EventConfigJobProperty#isDisableAppBuildEvents()}
   * set to {@code true}.
   * 
   * @param build the Jenkins run instance for the active build.
   * @return {@code true} if notifications for this build should be recorded,
   *        otherwise {@code false}.
   */
  public boolean isReporting(
      Run<? extends Job<?, ?>, ? extends Run<?, ?>> build
  ) {
    Job<?, ?> job = build.getParent();
    EventConfigJobProperty prop = job.getProperty(
        EventConfigJobProperty.class
    );
    
    return prop != null ? !prop.isDisableAppBuildEvents() : true;
  }
  
  @Override
  public void onStarted(
      Run<? extends Job<?, ?>, ? extends Run<?, ?>> build,
      TaskListener listener
  ) {
    if (!isReporting(build)) {
      return;
    }
    this.recorder.recordBuildEvent(BuildEventType.STARTED, build, listener);
  }

  @Override
  public void onCompleted(
      Run<? extends Job<?, ?>, ? extends Run<?, ?>> build,
      TaskListener listener
  ) {
    if (!isReporting(build)) {
      return;
    }    
    this.recorder.recordBuildEvent(BuildEventType.COMPLETED, build, listener);
  }

  @Override
  public void onFinalized(
      Run<? extends Job<?, ?>, ? extends Run<?, ?>> build
  ) {
    if (!isReporting(build)) {
      return;
    } 
    this.recorder.recordBuildEvent(BuildEventType.FINALIZED, build);
  }

  @Override
  public void onInitialize(
      Run<? extends Job<?, ?>, ? extends Run<?, ?>> build
  ) {
    if (!isReporting(build)) {
      return;
    } 
    this.recorder.recordBuildEvent(BuildEventType.INITIALIZED, build);
  }

  @Override
  public void onDeleted(
      Run<? extends Job<?, ?>, ? extends Run<?, ?>> build
  ) {
    if (!isReporting(build)) {
      return;
    } 
    this.recorder.recordBuildEvent(BuildEventType.DELETED, build);
  }
  
  
}