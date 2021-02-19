/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.jenkins;

import com.newrelic.experts.client.model.Event;

/**
 * A list based event sink.
 * 
 * @author Scott DeWitt (sdewitt@newrelic.com)
 */
public interface EventRecorder {
  
  /**
   * Record a custom Insights event.
   * 
   * @param event the event.
   */
  void recordEvent(Event event);
  
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
