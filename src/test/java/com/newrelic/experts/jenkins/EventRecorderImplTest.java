/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.experts.jenkins;

import com.newrelic.experts.client.model.Event;
import com.newrelic.experts.client.model.JenkinsMasterEvent;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;


/**
 * @author dalvizu
 */
@Test(groups = { "unit" })
public class EventRecorderImplTest {

  private EventRecorderImpl eventRecorder;

  @BeforeMethod
  public void setup() {
    eventRecorder = new EventRecorderImpl();
  }

  @Test
  public void testRecordAppDeploymentEvent() {

    eventRecorder.recordAppDeploymentEvent("appId", "sha1",
      "Changelog", "Description", "dalvizu");
    List<Event> results = Arrays.asList(eventRecorder.popEvents());
    assertEquals(1, results.size());
    assertEquals("AppDeploymentEvent", results.get(0).get(Event.PROPERTY_NAME_EVENT_TYPE));
    assertEquals("appId", results.get(0).get("appId"));
    assertEquals("sha1", results.get(0).get("revision"));
    assertEquals("Description", results.get(0).get("description"));
    assertEquals("Changelog", results.get(0).get("changelog"));
    assertEquals("dalvizu", results.get(0).get("user"));
  }

  @Test
  public void testRecordJenkinsMasterEvent() {
    JenkinsMasterEvent jenkinsMasterEvent = new JenkinsMasterEvent();
    jenkinsMasterEvent.setAgentConnectedCount(10);
    jenkinsMasterEvent.setQuietDownMode(true);
    eventRecorder.recordJenkinsMasterEvent(jenkinsMasterEvent);
    List<Event> results = Arrays.asList(eventRecorder.popEvents());
    assertEquals(1, results.size());
    assertEquals("", results.get(0).get(Event.PROPERTY_NAME_EVENT_TYPE));
    assertEquals(10.0f, results.get(0).get("agentConnectedCount"));
    assertEquals(1.0f, results.get(0).get("quietDownMode"));
  }

}
