/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.experts.client.model;

/**
 * A JenkinsMasterEvent.
 * <p>
 * Describes monitorable events on Jenkins master.
 * </p>
 * @author Dan Alvizu (alvizu@gmail.com)
 */
public class JenkinsMasterEvent
    extends Event {

  public static final String EVENT_TYPE = "JenkinsMasterEvent";

  public JenkinsMasterEvent() {
    super(EVENT_TYPE);
  }

  /**
   * Set whether the Jenkins server is in 'Quiet Down' mode.
   * @link https://support.cloudbees.com/hc/en-us/articles/203737684-How-can-I-prevent-jenkins-from-starting-new-jobs-after-a-restart-
   * @param isQuietDownMode the quiet down mode
   */
  public void setQuietDownMode(boolean isQuietDownMode) {
    // only float and string are allowed, so send as float
    // https://docs.newrelic.com/docs/insights/insights-data-sources/custom-data/introduction-event-api#
    this.put("quietDownMode", isQuietDownMode ? 1.0f : 0.0f );
  }

  /**
   * Set the number of agents currently connect to this Jenkins master.
   * @param numberOfAgentsConnected the number of agents connected
   */
  public void setAgentConnectedCount(int numberOfAgentsConnected) {
    // only float and string are allowed, so send as float
    // https://docs.newrelic.com/docs/insights/insights-data-sources/custom-data/introduction-event-api#
    this.put("agentConnectedCount", (float)numberOfAgentsConnected );
  }

}
