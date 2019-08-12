/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.client.api;

/**
 * A bean for holding API agnostic connection configuration.
 * 
 * @author Scott DeWitt (sdewitt@newrelic.com)
 */
public class ClientConnectionConfiguration {

  private int connectionRequestTimeout = 2000;
  private int socketTimeout = 2000;
  private int connectTimeout = 2000;
  private int maximumConnections = 10;
  private int maximumConnectionsPerRoute = 10;
  private boolean useProxy;
  private String proxyHost;
  private int proxyPort = 80;
  private String proxyScheme = "http";
  private String proxyUsername;
  private String proxyPassword;

  public int getConnectionRequestTimeout() {
    return connectionRequestTimeout;
  }

  public void setConnectionRequestTimeout(int connectionRequestTimeout) {
    this.connectionRequestTimeout = connectionRequestTimeout;
  }

  public int getSocketTimeout() {
    return socketTimeout;
  }

  public void setSocketTimeout(int socketTimeout) {
    this.socketTimeout = socketTimeout;
  }

  public int getConnectTimeout() {
    return connectTimeout;
  }

  public void setConnectTimeout(int connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  public int getMaximumConnections() {
    return maximumConnections;
  }

  public void setMaximumConnections(int maximumConnections) {
    this.maximumConnections = maximumConnections;
  }

  public int getMaximumConnectionsPerRoute() {
    return maximumConnectionsPerRoute;
  }

  public void setMaximumConnectionsPerRoute(int maximumConnectionsPerRoute) {
    this.maximumConnectionsPerRoute = maximumConnectionsPerRoute;
  }

  public boolean isUseProxy() {
    return useProxy;
  }

  public void setUseProxy(boolean useProxy) {
    this.useProxy = useProxy;
  }

  public String getProxyHost() {
    return proxyHost;
  }

  public void setProxyHost(String proxyHost) {
    this.proxyHost = proxyHost;
  }

  public int getProxyPort() {
    return proxyPort;
  }

  public void setProxyPort(int proxyPort) {
    this.proxyPort = proxyPort;
  }

  public String getProxyScheme() {
    return proxyScheme;
  }

  public void setProxyScheme(String proxyScheme) {
    this.proxyScheme = proxyScheme;
  }

  public String getProxyUsername() {
    return proxyUsername;
  }

  public void setProxyUsername(String proxyUsername) {
    this.proxyUsername = proxyUsername;
  }

  public String getProxyPassword() {
    return proxyPassword;
  }

  public void setProxyPassword(String proxyPassword) {
    this.proxyPassword = proxyPassword;
  }

}