/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.experts;

import com.newrelic.experts.client.model.JenkinsMasterEvent;
import org.testng.annotations.Test;


import static org.testng.Assert.assertEquals;

/**
 * @author dalvizu
 */
@Test(groups = { "unit" })
public class JenkinsMasterEventTest
{

  @Test
  public void testBasicObject() {
    JenkinsMasterEvent jenkinsMasterEvent = new JenkinsMasterEvent();
    jenkinsMasterEvent.setQuietDownMode(true);
    jenkinsMasterEvent.setAgentConnectedCount(12);

    assertEquals(1.0f, jenkinsMasterEvent.get("quietDownMode"));
    assertEquals(12.0f, jenkinsMasterEvent.get("agentConnectedCount"));
    assertEquals(Float.class, jenkinsMasterEvent.get("agentConnectedCount").getClass());
  }
}
