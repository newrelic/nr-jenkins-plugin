/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.jenkins;

import com.newrelic.experts.client.model.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of the {@link EventRecorder} that utilizes a simple
 * {@link java.util.LinkedList} to store the events.
 * 
 * @author Scott DeWitt (sdewitt@newrelic.com)
 */
public class EventRecorderImpl implements EventRecorder {
  
  private List<Event> events;
  
  public EventRecorderImpl() {
    this.events = new ArrayList<Event>();
  }
  
  @Override
  public synchronized void recordEvent(Event event) {
    this.events.add(event);
  }
  
  @Override
  public synchronized Event[] popEvents() {
    Event[] eventAry = new Event[this.events.size()];
    eventAry = this.events.toArray(eventAry);
    this.events.clear();
    return eventAry;
  }
}
