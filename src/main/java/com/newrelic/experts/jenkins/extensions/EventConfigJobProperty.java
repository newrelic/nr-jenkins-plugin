/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.jenkins.extensions;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.JobProperty;

import jenkins.model.Jenkins;
import jenkins.model.OptionalJobProperty;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.List;

/**
 * Definition of an optional {@link JobProperty} for New Relic configuration.
 * 
 * @author Scott DeWitt (sdewitt@newrelic.com)
 */
@ExportedBean
public class EventConfigJobProperty extends OptionalJobProperty<Job<?,?>> {

  private boolean disableAppBuildEvents;
  private List<KeyValuePair> customAttributes;
  
  @DataBoundConstructor
  public EventConfigJobProperty(
      boolean disableAppBuildEvents,
      List<KeyValuePair> customAttributes
  ) {
    this.disableAppBuildEvents = disableAppBuildEvents;
    this.customAttributes = customAttributes;
  }
  
  @Exported
  public boolean isDisableAppBuildEvents() {
    return disableAppBuildEvents;
  }

  @Exported
  public List<KeyValuePair> getCustomAttributes() {
    return this.customAttributes;
  }
  
  @Extension(ordinal = -1000)
  public static final class DescriptorImpl extends OptionalJobPropertyDescriptor {
    
    @Override
    public boolean isApplicable(Class<? extends Job> jobType) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return "Customize New Relic build event settings";
    }
    
    public List<? extends Descriptor> getCustomAttributesDescriptors() {
      return Jenkins.getInstance().getDescriptorList(KeyValuePair.class);
    }
  }
}
  