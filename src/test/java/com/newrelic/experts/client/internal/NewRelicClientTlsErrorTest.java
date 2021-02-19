/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.client.internal;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.experts.client.api.NewRelicClientException;

import org.apache.http.ConnectionClosedException;
import org.apache.http.ExceptionLogger;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

public class NewRelicClientTlsErrorTest {

  static class StdErrorExceptionLogger implements ExceptionLogger {
    @Override
    public void log(final Exception ex) {
      if (ex instanceof SocketTimeoutException) {
        System.err.println("Connection timed out");
      } else if (ex instanceof ConnectionClosedException) {
        System.err.println(ex.getMessage());
      } else {
        ex.printStackTrace();
      }
    }
  }
  
  static class SimpleRequestHandler implements HttpRequestHandler {
    @Override
    public void handle(
        HttpRequest request,
        HttpResponse response,
        HttpContext context
    ) throws HttpException, IOException {
      response.setStatusCode(HttpStatus.SC_OK);
    }
  }
  
  private static HttpServer server;
  
  /**
   * Setup a local HTTPs server on port 12345 with a self signed certificate.
   * 
   * @throws Exception if server setup fails.
   */
  @BeforeClass
  public static void setUp() throws Exception {
    URL keyStoreUrl = NewRelicClientTlsErrorTest.class.getResource("/self-signed.jks");
    SSLContext sslContext = SSLContextBuilder.create()
        .loadKeyMaterial(
            keyStoreUrl,
            "inttest".toCharArray(),
            "inttest".toCharArray()
        )
        .build();
    SocketConfig socketConfig = SocketConfig.custom()
            .setSoTimeout(15000)
            .setTcpNoDelay(true)
            .build();
    server = ServerBootstrap.bootstrap()
            .setListenerPort(12345)
            .setSocketConfig(socketConfig)
            .setSslContext(sslContext)
            .setExceptionLogger(new StdErrorExceptionLogger())
            .registerHandler("*", new SimpleRequestHandler())
            .create();
    server.start();
  }
  
  @AfterClass
  public static void teardown() {
    server.shutdown(0, TimeUnit.MILLISECONDS);
  }
  
  /**
   * Validate that connecting to the local HTTPs server should fail because
   * the server certificate chain could not be validated.
   */
  @Test
  public void getShouldFailWithSslHandshakeException() {
    BasicHttpClientConnectionManager connManager =
        new BasicHttpClientConnectionManager();
    ObjectMapper mapper = new ObjectMapper().configure(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
        false
    );
        
    NewRelicApiHelper helper = new NewRelicApiHelper(connManager, mapper);
    CloseableHttpClient client = helper.createHttpClient(
        helper.createClientConnectionConfig(5000, 2000, 5000, null),
        new ArrayList<Header>()
    );
    
    try {
      helper.get(client, URI.create("https://localhost:12345/"), String.class);
      Assert.fail("Connection should not have succeeded!");
    } catch (NewRelicClientException nre) {
      Throwable cause = nre.getCause();
      Assert.assertTrue(cause instanceof javax.net.ssl.SSLHandshakeException);
    } 
  }
}
