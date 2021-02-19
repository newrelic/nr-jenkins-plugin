/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.jenkins;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.experts.client.api.NewRelicClient;
import com.newrelic.experts.client.internal.NewRelicApiHelper;
import com.newrelic.experts.client.internal.NewRelicClientImpl;

import hudson.Extension;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * The main New Relic google module which sets up the service mappings
 * for our implementation.
 * 
 * @author Scott DeWitt (sdewitt@newrelic.com)
 */
@Extension
public class NewRelicJenkinsModule extends AbstractModule {
  
  /**
   * Create a new {@link NewRelicApiHelper} singleton instance.
   * <p>
   * Create a new {@link NewRelicApiHelper} instance configured with a
   * {@link PoolingHttpClientConnectionManager} and a simple
   * {@link ObjectMapper}.
   * </p>
   * 
   * @return A new {@link NewRelicApiHelper}.
   */
  public NewRelicApiHelper newRelicApiHelper() {
    PoolingHttpClientConnectionManager connManager
        = new PoolingHttpClientConnectionManager();
    // TODO: Expose these as configuration.
    connManager.setMaxTotal(10);
    connManager.setDefaultMaxPerRoute(10);
    
    ObjectMapper mapper = new ObjectMapper().configure(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
        false
    );
    
    return new NewRelicApiHelper(connManager, mapper);
  }
  
  /**
   * Create a new {@link NewRelicClientImpl} singleton instance.
   * 
   * @return A new {@link NewRelicApiHelper}.
   */
  public NewRelicClientImpl newRelicClientImpl() {
    return new NewRelicClientImpl(newRelicApiHelper());
  }
  
  @Singleton
  @Provides
  public EventRecorder eventRecorder() {
    return new EventRecorderImpl();
  }
  
  @Override
  public void configure() {
    bind(NewRelicClient.class)
      .toInstance(newRelicClientImpl());
  }

}
