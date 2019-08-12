/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.client.model;

/**
 * A bean for holding a New Relic APM {@link DeploymentMarker}.
 * 
 * @author Scott DeWitt (sdewitt@newrelic.com)
 */
public class Deployment {

  private DeploymentMarker deployment;
  
  public Deployment() {
  }

  public DeploymentMarker getDeployment() {
    return deployment;
  }

  public void setDeployment(DeploymentMarker deployment) {
    this.deployment = deployment;
  }

}
