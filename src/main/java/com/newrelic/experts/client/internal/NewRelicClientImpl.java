/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.client.internal;

import com.newrelic.experts.client.api.ClientConnectionConfiguration;
import com.newrelic.experts.client.api.NewRelicClient;
import com.newrelic.experts.client.api.NewRelicClientException;
import com.newrelic.experts.client.api.ProxyConfiguration;
import com.newrelic.experts.client.model.ApplicationList;
import com.newrelic.experts.client.model.Deployment;
import com.newrelic.experts.client.model.Event;
import com.newrelic.experts.client.model.InsightsResponse;
import com.newrelic.experts.jenkins.Messages;

import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of the {@link NewRelicClient} that utilizes the various
 * New Relic ReST APIs.
 * 
 * @author Scott DeWitt (sdewitt@newrelic.com)
 */
public class NewRelicClientImpl implements NewRelicClient {

  private static final String CLASS_NAME = NewRelicClientImpl.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);
  
  private NewRelicApiHelper apiHelper;

  public NewRelicClientImpl(NewRelicApiHelper apiHelper) {
    this.apiHelper = apiHelper;
  }  
  
  @Override
  public ClientConnectionConfiguration createClientConnectionConfig(
      int socketTimeout,
      int connectTimeout,
      int connRequestTimeout,
      ProxyConfiguration proxyConfig
  ) {
    return this.apiHelper.createClientConnectionConfig(
        socketTimeout,
        connectTimeout,
        connRequestTimeout,
        proxyConfig
    );
  }
  
  @Override
  public void recordEvents(
      ClientConnectionConfiguration connConfig,
      String rpmAccountId,
      String insightsApiInsertKey,
      Event[] events
  ) throws NewRelicClientException {
    final String methodName = "recordEvents";
    final boolean isLoggingTrace = LOGGER.isLoggable(Level.FINE);
    final boolean isLoggingDebug = LOGGER.isLoggable(Level.FINEST);
    
    if (isLoggingTrace) {
      LOGGER.entering(CLASS_NAME, methodName);
    }
    
    CloseableHttpClient client = null;

    try {
      String insightsApiUrl = 
          "https://insights-collector.newrelic.com/v1/accounts/"
          + rpmAccountId
          + "/events";
      
      client = this.apiHelper.createInsightsApiClient(
          connConfig,
          insightsApiInsertKey
      );
      
      if (isLoggingDebug) {
        LOGGER.finest(String.format("Sending %d events", events.length));
      }

      InsightsResponse result = this.apiHelper.post(
          client,
          this.apiHelper.buildUri(insightsApiUrl, null),
          events,
          InsightsResponse.class,
          true
      );
      if (result == null || !result.isSuccess()) {
        LOGGER.log(Level.SEVERE, "The Insights events could not be sent");
        throw new NewRelicClientException(
            Messages.NewRelicClientImpl_errors_RecordEventsPostError()
        );
      }
      
      if (isLoggingDebug) {
        LOGGER.finest(String.format("%d events sent ", events.length));
      }
    } catch (URISyntaxException exc) {
      LOGGER.log(Level.SEVERE, "Invalid URL for Insights Insert API");
      throw new NewRelicClientException(
          Messages.NewRelicClientImpl_errors_RecordEventsInvalidUri()
      );
    } finally {
      try {
        client.close();
      } catch (IOException ignore) {
        LOGGER.log(Level.WARNING, "Ignoring exception on closing client");
      }
      
      if (isLoggingTrace) {
        LOGGER.exiting(CLASS_NAME, methodName);
      }
    }
  }

  @Override
  public Deployment recordDeployment(
      ClientConnectionConfiguration connConfig,
      String apiKey,
      String appId,
      Deployment deployment
  ) throws NewRelicClientException {
    final String methodName = "recordDeployment";
    final boolean isLoggingTrace = LOGGER.isLoggable(Level.FINE);
    final boolean isLoggingDebug = LOGGER.isLoggable(Level.FINEST);
    
    if (isLoggingTrace) {
      LOGGER.entering(CLASS_NAME, methodName, new Object[] {
          appId
      });
    }
    
    CloseableHttpClient client = null;
    
    try {
      String apmApiUrl = 
          "https://api.newrelic.com/v2/applications/"
          + appId
          + "/deployments.json";
      
      client = this.apiHelper.createRestApiClient(
          connConfig,
          apiKey
      );
      
      if (isLoggingDebug) {
        LOGGER.finest("Posting deployment marker...");
      }

      Deployment result = this.apiHelper.post(
          client,
          this.apiHelper.buildUri(apmApiUrl, null),
          deployment,
          Deployment.class,
          false
      );
      
      if (result == null) {
        LOGGER.log(Level.SEVERE, "The deployment marker could not be created");
        throw new NewRelicClientException(
            Messages.NewRelicClientImpl_errors_RecordDeploymentPostError()
        );
      }
      
      if (isLoggingDebug) {
        LOGGER.finest(String.format(
            "Deployment with id %s created at %s",
            result.getDeployment().getId(),
            result.getDeployment().getTimestamp()
        ));
      }
      
      return result;
    } catch (URISyntaxException exc) {
      LOGGER.log(
          Level.SEVERE,
          "Invalid URL for APM applications ReST API endpoint"
      );
      throw new NewRelicClientException(
          Messages.NewRelicClientImpl_errors_RecordDeploymentInvalidUri()
      );
    } finally {
      try {
        client.close();
      } catch (IOException ignore) {
        LOGGER.log(Level.WARNING,
            String.format(
                "Ignoring exception on closing client: %s",
                ignore.getMessage()
            ),
            ignore);
      }
      
      if (isLoggingTrace) {
        LOGGER.exiting(CLASS_NAME, methodName);
      }
    }
  }
  
  @Override
  public ApplicationList getApplications(
      ClientConnectionConfiguration connConfig,
      String apiKey
  ) throws NewRelicClientException  {
    final String methodName = "getApplications";
    final boolean isLoggingTrace = LOGGER.isLoggable(Level.FINE);
    final boolean isLoggingDebug = LOGGER.isLoggable(Level.FINEST);
    
    if (isLoggingTrace) {
      LOGGER.entering(CLASS_NAME, methodName);
    }
    
    CloseableHttpClient client = null;
    
    try {      
      client = this.apiHelper.createRestApiClient(
          connConfig,
          apiKey
      );
      
      if (isLoggingDebug) {
        LOGGER.finest("Retrieving applications...");
      }

      ApplicationList result = this.apiHelper.get(
          client,
          this.apiHelper.buildUri(
              "https://api.newrelic.com/v2/applications.json",
              null
          ),
          ApplicationList.class
      );
      
      
      if (result == null) {
        if (isLoggingDebug) {
          LOGGER.finest("Result was null, returning empty application list.");
        }
        return new ApplicationList();
      }
      
      if (isLoggingDebug) {
        LOGGER.finest(String.format(
            "Retrieved %d applications.",
            result.count()
            )
        );
      }
      
      return result;
    } catch (URISyntaxException exc) {
      LOGGER.log(
          Level.SEVERE,
          "Invalid URL for APM applications ReST API endpoint"
      );
      throw new NewRelicClientException(
          Messages.NewRelicClientImpl_errors_GetApplicationsInvalidUri()
      );
    } finally {
      try {
        client.close();
      } catch (IOException ignore) {
        LOGGER.log(Level.WARNING,
            String.format(
                "Ignoring exception on closing client: %s",
                ignore.getMessage()
            ),
            ignore);
      }
      
      if (isLoggingTrace) {
        LOGGER.exiting(CLASS_NAME, methodName);
      }
    }
  }
}
