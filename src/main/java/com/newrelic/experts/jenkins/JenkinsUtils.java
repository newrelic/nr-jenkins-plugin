/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.jenkins;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.newrelic.experts.client.api.ClientConnectionConfiguration;
import com.newrelic.experts.client.api.NewRelicClient;
import com.newrelic.experts.client.api.ProxyConfiguration;

import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Build;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;

import jenkins.model.Jenkins;

import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility class which provides static Jenkins convenience methods.
 * <p>
 * TODO: Convert this class to a helper singleton that can be injected.
 * </p>
 * 
 * @author Scott DeWitt (sdewitt@newrelic.com)
 */
public class JenkinsUtils {
  
  // Copied from hudson.Util
  public static final Pattern VARIABLE =
      Pattern.compile("\\$([A-Za-z0-9_]+|\\{[A-Za-z0-9_.]+\\}|\\$)");
  
  /**
   * Convenience method to create a {@link ClientConnectionConfiguration}
   * populated with the global Jenkins proxy configuration and a sane set of
   * timeout settings.
   * 
   * @param client the New Relic client facade.
   * @return a populated, configured {@link ClientConnectionConfiguration}.
   */
  public static final ClientConnectionConfiguration createClientConnectionConfig(
      NewRelicClient client
  ) {
    hudson.ProxyConfiguration jenkinsProxy = Jenkins.getInstance().proxy;
    ProxyConfiguration proxy = null;
    
    if (jenkinsProxy != null) {
      proxy = new ProxyConfiguration();
      proxy.setHost(jenkinsProxy.name);
      proxy.setPort(jenkinsProxy.port);
      proxy.setUsername(jenkinsProxy.getUserName());
      proxy.setPassword(jenkinsProxy.getPassword());
    }
    return client.createClientConnectionConfig(
        5000,
        2000,
        5000,
        proxy
    );
  }
  
  /**
   * Return all {@link StandardUsernamePasswordCredentials} in the global
   * "Jenkins" credential domain.
   * 
   * @return the set of all {@link StandardUsernamePasswordCredentials} in the
   *        global "Jenkins" credential domain.
   */
  public static final List<StandardUsernamePasswordCredentials>
      getCredentials() {
    return CredentialsProvider.lookupCredentials(
        StandardUsernamePasswordCredentials.class,
        Jenkins.getInstance(),
        ACL.SYSTEM,
        Collections.<DomainRequirement>emptyList());
  }
  
  /**
   * Return the specific {@link StandardUsernamePasswordCredentials} for the
   * global Jenkins credential with ID {@code credentialsId}.
   * 
   * @param credentialsId the ID of the credentials to lookup in the global
   *        Jenkins domain.
   * @return the {@link StandardUsernamePasswordCredentials} for the
   *        global Jenkins credential with ID {@code credentialsId}. 
   */
  public static final StandardUsernamePasswordCredentials getCredentials(
      String credentialsId
  ) {
    return CredentialsMatchers.firstOrNull(
        getCredentials(),
        CredentialsMatchers.withId(credentialsId)
    );
  }
  
  /**
   * Perform token expansion on {@code template}.
   * <p>
   * This method is very similar to
   * {@link TokenMacro#expandAll(Run, FilePath, TaskListener, String, boolean, List)}
   * with the exception that a workspace is not required to perform environment
   * variable substitution or build variables. Therefore, these substitutions
   * are done first and then token expansion only proceeds if there is a
   * workspace.  Unlike the referenced method, this method will never throw
   * an exception if the workspace is not available.
   * </p>
   * 
   * @param run the Jenkins run instance for an active run of a job.
   * @param listener the run listener for this run instance.
   * @param template the string to process.
   * @return {@code template} with tokens expanded.
   * @throws IOException if any I/O errors occur while generating token
   *        values (for example if the token generator has to perform a file
   *        system operation.)
   * @throws InterruptedException if some thread being used for the expansion
   *        process get interrupted.
   */
  public static String expandTokens(
      Run<?, ?> run, 
      TaskListener listener,
      String template
  ) throws IOException, InterruptedException {
    if (template == null) {
      return "";
    }
    
    try {
      // Expand environment variables
      String result = run
          .getEnvironment(listener)
          .expand(template.replaceAll("\\$\\$", "\\$\\$\\$\\$"));

      if (run instanceof AbstractBuild) {
        // Expand build variables
        AbstractBuild<?,?> build = (AbstractBuild<?, ?>)run;
        FilePath workspace = build.getWorkspace();
        
        result = Util.replaceMacro(
            result.replaceAll("\\$\\$", "\\$\\$\\$\\$"),
            build.getBuildVariableResolver()
        );
      
        // Expand Macros
        if (workspace != null) {
          result = TokenMacro.expand(run, workspace, listener, result);
        }
      }
      return TokenMacro.expandAll((Build<?, ?>)run, listener, template);
    } catch (MacroEvaluationException mee) {
      // TODO: log
      return template;
    }
  }
  
  /**
   * Get an instance of a service bean by service key/id from the DI container.
   * 
   * @param key the service key.
   * @param <T> the interface type of the service.
   * @return an implementation of the service that implements {@code T}
   *        for {@code key}.
   */
  public static final <T> T getService(Class<T> key) {
    return (T)Jenkins.getInstance().getInjector().getInstance(key);
  }
}
