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
import com.newrelic.experts.client.api.NewRelicClient;
import com.newrelic.experts.client.api.NewRelicClientException;
import com.newrelic.experts.client.model.Application;
import com.newrelic.experts.client.model.ApplicationList;
import com.newrelic.experts.client.model.Deployment;
import com.newrelic.experts.client.model.DeploymentMarker;
import com.newrelic.experts.jenkins.JenkinsUtils;
import com.newrelic.experts.jenkins.Messages;
import com.newrelic.experts.jenkins.events.AppDeploymentEventProducer;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;

import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * A Jenkins {@link Notifier} object that posts APM deployment markers when it
 * is called.
 * 
 * @author Scott DeWitt (sdewitt@newrelic.com)
 */
public class DeploymentMarkerNotifier extends Notifier implements SimpleBuildStep {

  private String apiKeyCredentialsId;
  private String appId;
  private String revision;
  private String changeLog;
  private String description;
  private String user;
  private boolean createInsightsMarker;
  
  /**
   * Create a new {@link DeploymentMarkerNotifier} notifier describable.
   * <p>
   * Because this constructor is marked with {@code @DataBoundConstructor},
   * The Stapler Web Framework will staple these form values into this model
   * object automatically.
   * </p>
   * 
   * @param apiKeyCredentialsId the APM API Key Jenkins credential ID.
   * @param appId the APM application ID.
   * @param revision the deployment marker "revision" property.
   * @param changeLog the deployment marker "changelog" property.
   * @param description the deployment marker "description" property.
   * @param user the deployment marker "user" property.
   * @param createInsightsMarker flag indicating if a corresponding Insights
   *        AppDeployEvent should be created mirroring the APM deployment
   *        marker.
   */
  @DataBoundConstructor
  public DeploymentMarkerNotifier(
      String apiKeyCredentialsId,
      String appId,
      String revision,
      String changeLog,
      String description,
      String user,
      boolean createInsightsMarker
  ) {
    this.apiKeyCredentialsId = apiKeyCredentialsId;
    this.appId = appId;
    this.revision = revision;
    this.changeLog = changeLog;
    this.description = description;
    this.user = user;
    this.createInsightsMarker = createInsightsMarker;
  }
  
  /**
   * Convenience method to return the APM API key credentials.
   * 
   * @return the {@link StandardUsernamePasswordCredentials} associated with
   *        the credential ID stored in this object.  If for some reason this
   *        object has not stored credential ID or credentials do not exist
   *        with this ID, return {@code NULL}.
   */
  public StandardUsernamePasswordCredentials getApiKeyCredentials() {
    if (this.apiKeyCredentialsId == null) {
      return null;
    }
    return JenkinsUtils.getCredentials(this.apiKeyCredentialsId);
  }

  public String getApiKeyCredentialsId() {
    return this.apiKeyCredentialsId;
  }

  public String getAppId() {
    return this.appId;
  }
  
  public String getRevision() {
    return this.revision;
  }

  public String getChangeLog() {
    return this.changeLog;
  }

  public String getDescription() {
    return this.description;
  }

  public boolean isCreateInsightsMarker() {
    return this.createInsightsMarker;
  }
    
  public String getUser() {
    return user;
  }

  @Override
  public void perform(
      Run<?, ?> run, 
      FilePath workspace,
      Launcher launcher,
      TaskListener listener
  ) throws InterruptedException, IOException {
    StandardUsernamePasswordCredentials creds = getApiKeyCredentials();
    
    if (creds == null) {
      listener.getLogger().println(
          Messages.DeploymentMarkerNotifier_PerformCalledWithNoCreds(
              run.getFullDisplayName(),
              run.getParent().getFullDisplayName()
          )
      );
      return;
    }
    
    if (this.revision == null) {
      listener.getLogger().println(
          Messages.DeploymentMarkerNotifier_PerformCalledWithNoRevision(
              run.getFullDisplayName(),
              run.getParent().getFullDisplayName()
          )
      );
      return;
    }
    
    listener.getLogger().println(
        Messages.DeploymentMarkerNotifier_RecordingDeploymentMarker(
            run.getFullDisplayName(),
            run.getParent().getFullDisplayName(),
            this.revision
        )
    );
    
    AppDeploymentEventProducer producer = JenkinsUtils.getService(AppDeploymentEventProducer.class);
    String revision = JenkinsUtils.expandTokens(run, listener, this.revision);
    String changeLog = JenkinsUtils.expandTokens(run, listener, this.changeLog);
    String description = JenkinsUtils.expandTokens(run, listener, this.description);
    String user = JenkinsUtils.expandTokens(run, listener, this.user);
    
    DeploymentMarker deploymentMarker = new DeploymentMarker();
    deploymentMarker.setRevision(revision);
    deploymentMarker.setChangelog(changeLog);
    deploymentMarker.setDescription(description);
    deploymentMarker.setUser(user);

    Deployment deployment = new Deployment();
    deployment.setDeployment(deploymentMarker);
    
    // Send an official deployment marker
    NewRelicClient client = JenkinsUtils.getService(NewRelicClient.class);
    client.recordDeployment(
        JenkinsUtils.createClientConnectionConfig(client), 
        Secret.toString(creds.getPassword()), 
        this.appId, 
        deployment
    );
    
    // Record an "Insights deployment marker"
    if (this.createInsightsMarker) {
      producer.recordEvent(
          this.appId,
          revision,
          changeLog,
          description,
          user
      );
    }
  }

  /**
   * The {@link Descriptor} class for the {@link DeploymentMarkerNotifier}.
   * 
   * @author Scott DeWitt (sdewitt@newrelic.com)
   */
  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    private NewRelicClient newRelicClient;
    
    @Inject
    public void setNewRelicClient(NewRelicClient newRelicClient) {
      this.newRelicClient = newRelicClient;
    }
    
    /**
     * Populate the {@link ListBoxModel} with APM API Key credential ID choices.
     * 
     * @param item ancestor item in the path.
     * @param apiKeyCredentialsId the current credential ID value.
     * @return populated {@link ListBoxModel}.
     */
    public ListBoxModel doFillApiKeyCredentialsIdItems(
        @AncestorInPath Item item,
        @QueryParameter String apiKeyCredentialsId
    ) {
      StandardUsernameListBoxModel result = new StandardUsernameListBoxModel();
      
      if (item == null) {
        if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
          return result.includeCurrentValue(apiKeyCredentialsId);
        }
      } else {
        if (!item.hasPermission(Item.EXTENDED_READ)
            && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
          return result.includeCurrentValue(apiKeyCredentialsId);
        }
      }
      
      result.includeEmptyValue();
      
      List<StandardUsernamePasswordCredentials> credentials =
          JenkinsUtils.getCredentials();
      
      for (StandardUsernamePasswordCredentials credential : credentials) {
        result.add(credential.getId(), credential.getId());
      }
      
      return result.includeCurrentValue(apiKeyCredentialsId);
    }
    
    /**
     * Fill the {@link ListBoxModel} with New Relic Application IDs..
     * 
     * @param item ancestor item in the path.
     * @param apiKeyCredentialsId the selected APM API Key credential id.
     * @param appId the current appId value.
     * @return populated {@link ListBoxModel}.
     */
    public ListBoxModel doFillAppIdItems(
        @AncestorInPath Item item,
        @QueryParameter String apiKeyCredentialsId,
        @QueryParameter String appId
    ) {
      ListBoxModel result = new ListBoxModel();
      StandardUsernamePasswordCredentials creds;
      
      result.add(Messages.DeploymentMarkerNotifier_NoneOption(), "");
      
      if (
          apiKeyCredentialsId == null
          || apiKeyCredentialsId.trim().isEmpty()
      ) {
        return result;
      }
       
      creds = JenkinsUtils.getCredentials(apiKeyCredentialsId);
      
      try {
        ApplicationList applications
            = this.newRelicClient.getApplications(
              JenkinsUtils.createClientConnectionConfig(this.newRelicClient),
              Secret.toString(creds.getPassword())
        );
        
        for (Application appl : applications.getApplications()) {
          result.add(appl.getName(), appl.getId());
        }
        
        return result;
      } catch (NewRelicClientException nrce) {
        // TODO log
        return result;
      }
    }
    
    
    /**
     * Check the user selected APM API key credential ID.
     * 
     * @param item ancestor item in path.
     * @param apiKeyCredentialsId selected credential ID.
     * @return a {@link FormValidation} result.
     */
    public FormValidation doCheckApiKeyCredentialsId(
        @AncestorInPath Item item,
        @QueryParameter String apiKeyCredentialsId
    ) {
      if (item == null) {
        if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
          return FormValidation.ok();
        }
      } else {
        if (!item.hasPermission(Item.EXTENDED_READ)
            && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
          return FormValidation.ok();
        }
      }
      if (StringUtils.isBlank(apiKeyCredentialsId)) {
        return FormValidation.error(
            Messages.DeploymentMarkerNotifier_errors_EmptyApiCreds()
        );
      }
      if (apiKeyCredentialsId.startsWith("${")
          && apiKeyCredentialsId.endsWith("}")) {
        return FormValidation.warning(
            Messages.DeploymentMarkerNotifier_errors_ApiCredsExpressionNotSupported()
        );
      }
      if (CredentialsProvider.listCredentials(
          StandardUsernameCredentials.class,
          Jenkins.getInstance(),
          ACL.SYSTEM,
          Collections.<DomainRequirement>emptyList(),
          CredentialsMatchers.withId(apiKeyCredentialsId)
      ).isEmpty()) {
        return FormValidation.error(
            Messages.DeploymentMarkerNotifier_errors_MissingApiCreds()
        );
      }
      return FormValidation.ok();
    }
    
    /**
     * Ensure that the user selected the required APM application ID.
     * 
     * @param appId selected application ID.
     * @return a {@link FormValidation} result.
     */
    public FormValidation doCheckAppId(
        @QueryParameter String appId
    ) {
      if (StringUtils.isBlank(appId)) {
        return FormValidation.error(
            Messages.DeploymentMarkerNotifier_errors_EmptyAppId()
        );
      }
      return FormValidation.ok();
    }
    
    /**
     * Ensure that the user entered the required vision.
     * 
     * @param revision the revision string.
     * @return a {@link FormValidation} result.
     */
    public FormValidation doCheckRevision(
        @QueryParameter String revision
    ) {
      if (StringUtils.isBlank(revision)) {
        return FormValidation.error(
            Messages.DeploymentMarkerNotifier_errors_EmptyRevision()
        );
      }
      return FormValidation.ok();
    }

    @Override
    public boolean isApplicable(
        @SuppressWarnings("rawtypes") Class<? extends AbstractProject> clazz
    ) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return Messages.DeploymentMarkerNotifier_DisplayName();
    }

  }

}