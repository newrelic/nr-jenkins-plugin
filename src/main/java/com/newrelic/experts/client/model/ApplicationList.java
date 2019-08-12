/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.client.model;

/**
 * A bean for holding a list of New Relic APM {@link Application}s.
 * 
 * @author Scott DeWitt (sdewitt@newrelic.com)
 */
public class ApplicationList {

  private Application[] applications;
  
  public ApplicationList() {
  }
  
  public Application[] getApplications() {
    return applications;
  }

  public void setApplications(Application[] applications) {
    this.applications = applications;
  }
  
  public int count() {
    return this.applications != null ? this.applications.length : 0;
  }
}
