/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.client.model;

import java.util.HashMap;

/**
 * A {@link java.util.Map} bean for building custom events.
 * 
 * @author Scott DeWitt (sdewitt@newrelic.com)
 */
public class Event extends HashMap<String, Object> {

  private static final long serialVersionUID = -5258963477481799100L;
  
  public static final String PROPERTY_NAME_EVENT_TYPE = "eventType";

  public Event(String eventType) {
    put(PROPERTY_NAME_EVENT_TYPE, eventType);
  }

}