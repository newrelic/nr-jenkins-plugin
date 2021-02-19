/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.experts.jenkins;

import com.newrelic.experts.client.model.Event;
import hudson.model.Computer;
import hudson.model.Label;
import hudson.model.labels.LabelAtom;
import hudson.util.TagCloud;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;


import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * @author dalvizu
 */
public class EventRecorderImplTest {

  private EventRecorderImpl eventRecorder;

  @Before
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
  public void testRecordJenkinsMasterEvent() throws Exception {
    try(MockedStatic<Jenkins> theMock = Mockito.mockStatic(Jenkins.class)) {
      Jenkins mockJenkins = mock(Jenkins.class);
      theMock.when(Jenkins::getInstance).thenReturn(mockJenkins);
      theMock.when(Jenkins::get).thenReturn(mockJenkins);
      mockHostname(mockJenkins, "mock hostname");
      mockLabels(mockJenkins, "corp it");
      eventRecorder.recordJenkinsMasterEvent(true, 10);
      List<Event> results = Arrays.asList(eventRecorder.popEvents());
      assertEquals(1, results.size());
      assertEquals("JenkinsMasterEvent", results.get(0).get(Event.PROPERTY_NAME_EVENT_TYPE));
      assertEquals(10.0f, results.get(0).get("agentConnectedCount"));
      assertEquals(1.0f, results.get(0).get("quietDownMode"));
      assertEquals("corp|it", results.get(0).get("jenkinsMasterLabels"));
      assertEquals("mock hostname", results.get(0).get("jenkinsMasterHost"));
    }
  }

  private void mockHostname(Jenkins mockJenkins, String hostname)
    throws IOException, InterruptedException {
    Computer computer = mock(Computer.class);
    when(mockJenkins.toComputer()).thenReturn(computer);
    when(computer.getHostName()).thenReturn(hostname);
  }

  /**
   * Mock the labels to add on recorded events
   *
   * @param mockJenkins - the Jenkins object to mock
   * @param labelString - a whitespace separated list of labels describing the
   *                    master
   *                    (Jenkins convention)
   */
  private void mockLabels(Jenkins mockJenkins, String labelString) {
    when(mockJenkins.getLabelAtom(anyString())).thenAnswer( I -> new LabelAtom(I.getArgument(0)) );
    Set<LabelAtom> labels = Label.parse(labelString);
    TagCloud<LabelAtom> tagCloud =  new TagCloud<>(labels, new TagCloud.WeightFunction<LabelAtom>() {
      public float weight(LabelAtom item) {
        return 1;
      }
    });
    when(mockJenkins.getLabelCloud()).thenReturn(tagCloud);
  }
}
