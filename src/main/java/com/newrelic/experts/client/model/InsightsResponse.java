/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.client.model;

/**
 * A bean for holding an Insights API response.
 * 
 * @author Scott DeWitt (sdewitt@newrelic.com)
 */
public class InsightsResponse {

  private boolean success = false;

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

}
