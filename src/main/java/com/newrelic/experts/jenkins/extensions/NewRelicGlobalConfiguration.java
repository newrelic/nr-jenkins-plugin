/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.jenkins.extensions;

import com.google.inject.Inject;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.newrelic.experts.jenkins.JenkinsUtils;
import com.newrelic.experts.jenkins.Messages;
import com.newrelic.experts.jenkins.events.EventHelper;

import hudson.Extension;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.Collections;
import java.util.List;


@Extension
public class NewRelicGlobalConfiguration extends GlobalConfiguration {

  private String insightsInsertCredentialsId = null;
  private int eventHarvestInterval = 60;
  private int systemSampleInterval = 15;
  private EventHelper eventHelper;

  /**
   * Create a new {@link NewRelicGlobalConfiguration}.
   * <p>
   * This public no-argument constructors is required in order for SezPoz
   * to work properly.  If you call it directly it will thrown an
   * {@link UnsupportedOperationException}.
   * </p>
   */
  public NewRelicGlobalConfiguration() {
    throw new UnsupportedOperationException(
      "Public no-argument constructor is required but not supported."
    );
  } 
  
  /**
   * Create a new {@link NewRelicGlobalConfiguration}.
   * 
   * @param eventHelper the {@link EventHelper} singleton.
   */
  @Inject
  public NewRelicGlobalConfiguration(
      EventHelper eventHelper
  ) {
    super();
    
    this.eventHelper = eventHelper;
    
    load();
  }
  
  @Override
  public String getDisplayName() {
    return Messages.NewRelicGlobalConfiguration_DisplayName();
  }

  /**
   * Convenience method to return the Insights Insert API key credentials.
   * 
   * @return the {@link StandardUsernamePasswordCredentials} associated with
   *        the credential ID stored in this object.  If for some reason this
   *        object has not stored credential ID or credentials do not exist
   *        with this ID, return {@code NULL}.
   */
  public StandardUsernamePasswordCredentials getInsightsInsertCredentials() {
    if (this.insightsInsertCredentialsId == null) {
      return null;
    }
    return JenkinsUtils.getCredentials(this.insightsInsertCredentialsId);
  }
  
  public String getInsightsInsertCredentialsId() {
    return this.insightsInsertCredentialsId;
  }

  public void setInsightsInsertCredentialsId(
      String insightsInsertCredentialsId
  ) {
    this.insightsInsertCredentialsId = insightsInsertCredentialsId;
  }
  
  public int getEventHarvestInterval() {
    return eventHarvestInterval;
  }

  public void setEventHarvestInterval(int eventHarvestInterval) {
    this.eventHarvestInterval = eventHarvestInterval;
  }

  public int getSystemSampleInterval() {
    return systemSampleInterval;
  }

  public void setSystemSampleInterval(int systemSampleInterval) {
    this.systemSampleInterval = systemSampleInterval;
  }

  /**
   * Populate the {@link ListBoxModel} with Insights Insert Key credential ID
   * choices.
   * 
   * @param insightsInsertCredentialsId the current credential ID value.
   * @return populated {@link ListBoxModel}.
   */
  public ListBoxModel doFillInsightsInsertCredentialsIdItems(
      @QueryParameter String insightsInsertCredentialsId
  ) {
    StandardUsernameListBoxModel result = new StandardUsernameListBoxModel();
    if (!this.eventHelper.getJenkins().hasPermission(Jenkins.ADMINISTER)) {
      return new StandardUsernameListBoxModel()
          .includeCurrentValue(insightsInsertCredentialsId);
    }
    
    result.includeEmptyValue();
    
    List<StandardUsernamePasswordCredentials> credentials =
        JenkinsUtils.getCredentials();
    
    for (StandardUsernamePasswordCredentials credential : credentials) {
      result.add(credential.getId(), credential.getId());
    }
    
    return result.includeCurrentValue(insightsInsertCredentialsId);
  }

  /**
   * Check the user selected Insights Insert API key credential ID.
   * 
   * @param value selected credential ID.
   * @return a {@link FormValidation} result.
   */
  public FormValidation doCheckInsightsInsertCredentialsId(
      @QueryParameter String value
  ) {
    Jenkins jenkins = this.eventHelper.getJenkins();
    
    if (!jenkins.hasPermission(Jenkins.ADMINISTER)) {
      return FormValidation.ok();
    }
    if (StringUtils.isBlank(value)) {
      return FormValidation.ok();
    }
    if (value.startsWith("${") && value.endsWith("}")) {
      return FormValidation.warning(
          Messages.NewRelicGlobalConfiguration_errors_InsertCredsExpressionNotSupported()
      );
    }
    if (CredentialsProvider.listCredentials(
        StandardUsernameCredentials.class,
        jenkins,
        ACL.SYSTEM,
        Collections.<DomainRequirement>emptyList(),
        CredentialsMatchers.withId(value)
    ).isEmpty()) {
      return FormValidation.error(
          Messages.NewRelicGlobalConfiguration_errors_MissingInsertCreds()
      );
    }
    return FormValidation.ok();
  }
  
  /**
   * Applies the submitted configuration to this object.
   */
  @Override
  public boolean configure(StaplerRequest req, JSONObject json)
      throws FormException {
    req.bindJSON(this, json);
    save();
    return true;
  }
}