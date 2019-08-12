/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.client.api;

import com.newrelic.experts.client.model.ApplicationList;
import com.newrelic.experts.client.model.Deployment;
import com.newrelic.experts.client.model.Event;

/**
 * A facade object for interfacing with New Relic ReST APIs.
 * 
 * @author Scott DeWitt (sdewitt@newrelic.com)
 */
public interface NewRelicClient {
  
  /**
   * Create a new {@link ClientConnectionConfiguration}.
   * 
   * <p>
   * Create a new {@link ClientConnectionConfiguration} with the given timeout
   * values and proxy configuration to be used to connect to New Relic API
   * endpoints.  This common configuration is used to specify connection
   * data to all ReST APIs.
   * </p>
   * 
   * @param socketTimeout the OS level socket timeout.
   * @param connectTimeout the connection timeout.
   * @param connRequestTimeout the connection request timeout.
   * @param proxyConfig the proxy configuration.
   * @return a new {@link ClientConnectionConfiguration} configured with
   *        the given timeout and proxy configuration.
   * @see ClientConnectionConfiguration
   * @see ProxyConfiguration
   */
  ClientConnectionConfiguration createClientConnectionConfig(
      int socketTimeout,
      int connectTimeout,
      int connRequestTimeout,
      ProxyConfiguration proxyConfig
  );
  
  /**
   * Record the given set of custom {@code events} in Insights.
   * 
   * @param connConfig the connection configuration for the Insights Insert API.
   * @param rpmAccountId the RPM account ID.
   * @param insightsApiInsertKey the Insights Insert API key for the
   *        corresponding {@code rpmAccountId}.
   * @param events the array of custom events to record.
   * @throws NewRelicClientException if an error occurs with the connection
   *        or if their is a ReST error. 
   */
  void recordEvents(
      ClientConnectionConfiguration connConfig,
      String rpmAccountId,
      String insightsApiInsertKey,
      Event[] events
  ) throws NewRelicClientException;
  
  /**
   * Record an APM deployment marker for the application {@code appId}.
   * 
   * @param connConfig the connection configuration for the APM ReST API.
   * @param apiKey the APM API key to use (does not need to be an admin key).
   * @param appId the APM application ID.
   * @param deployment the deployment marker object.
   * @return a "POJO" representing the deployment that was recorded.
   * @throws NewRelicClientException if an error occurs with the connection
   *        or if their is a ReST error. 
   */
  Deployment recordDeployment(
      ClientConnectionConfiguration connConfig,
      String apiKey,
      String appId,
      Deployment deployment
  ) throws NewRelicClientException;
  
  /**
   * Return a list of "POJO" objects for applications available to the {@code apiKey}.
   * 
   * @param connConfig the connection configuration for the APM ReST API.
   * @param apiKey the APM API key to use (does not need to be an admin key).
   * @return a "POJO" representing the list of applications available to the
   *        the giving APM API key.
   * @throws NewRelicClientException if an error occurs with the connection
   *        or if their is a ReST error. 
   */
  ApplicationList getApplications(
      ClientConnectionConfiguration connConfig,
      String apiKey
  ) throws NewRelicClientException;
}
