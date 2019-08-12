/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.client.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.experts.client.api.ClientConnectionConfiguration;
import com.newrelic.experts.client.api.NewRelicClientException;
import com.newrelic.experts.client.api.ProxyConfiguration;
import com.newrelic.experts.jenkins.Messages;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import javax.xml.bind.DatatypeConverter;

/**
 * A helper class for create New Relic API clients and performing various
 * New Relic ReST API operations.
 * 
 * @author Scott DeWitt (sdewitt@newrelic.com)
 */
public class NewRelicApiHelper {

  private static final String CLASS_NAME = NewRelicApiHelper.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

  public static final String PROPERTY_NAME_HEADER_X_INSERT_KEY = "X-Insert-Key";
  public static final String PROPERTY_NAME_HEADER_X_API_KEY = "X-Api-Key";
  
  private HttpClientConnectionManager connManager;
  private ObjectMapper mapper;

  /**
   * Create a new {@code NewRelicApiHelper}.
   * <p>
   * This creates and initializes a new
   * {@link PoolingHttpClientConnectionManager} for use with the create*
   * methods.
   * </p>
   * 
   * @param connManager the {@link HttpClientConnectionManager} to use.
   * @param mapper the JSON {@link ObjectMapper} to use.
   */
  public NewRelicApiHelper(
      HttpClientConnectionManager connManager,
      ObjectMapper mapper
  ) {
    this.connManager = connManager;
    this.mapper = mapper;
  }
  
  //CHECKSTYLE:OFF
  /**
   * Make sure to clean up the {@link PoolingHttpClientConnectionManager}.
   * 
   * Normally a finalizers are not a great idea and Checkstyle wants us to
   * avoid them but in this case, it is critical to ensure we always release
   * the connection manager.
   */
  @Override
  protected void finalize() throws Throwable {
    try {
      this.connManager.shutdown();
    } finally {
      super.finalize();
    }
  }
  //CHECKSTYLE:ON
  
  /**
   * Build a new {@link java.net.URI} object given a string and optional params.
   * 
   * @param uri The string URL.
   * @param queryParams Optional query parameter map.
   * @return A {@link java.net.URI} object.
   * @throws URISyntaxException if {@code uri} is not a valid URI.
   */
  public URI buildUri(
      String uri,
      Map<String, String> queryParams
  ) throws URISyntaxException {
    URIBuilder builder = new URIBuilder(uri);
    if (queryParams != null) {
      for (Map.Entry<String, String> entry : queryParams.entrySet()) {
        builder.addParameter(entry.getKey(), entry.getValue());
      }
    }
    return builder.build();
  }

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
  public ClientConnectionConfiguration createClientConnectionConfig(
      int socketTimeout,
      int connectTimeout,
      int connRequestTimeout,
      ProxyConfiguration proxyConfig    
  ) {
    final String methodName = "createClientConnectionConfig";
    final boolean isLoggingTrace = LOGGER.isLoggable(Level.FINE);
    final boolean isLoggingDebug = LOGGER.isLoggable(Level.FINEST);

    if (isLoggingTrace) {
      LOGGER.entering(CLASS_NAME, methodName, new Object[] {
          socketTimeout, connectTimeout, connRequestTimeout
      });
    }
    
    ClientConnectionConfiguration ccc = new ClientConnectionConfiguration();
    
    ccc.setSocketTimeout(socketTimeout);
    ccc.setConnectTimeout(connectTimeout);
    ccc.setConnectionRequestTimeout(connRequestTimeout);
    
    if (proxyConfig == null) {
      if (isLoggingTrace) {
        LOGGER.logp(Level.FINE, CLASS_NAME, methodName,
            "RETURN EARLY No proxy set."
        );
      }
      return ccc;
    }
    
    String host = proxyConfig.getHost();
    int port = proxyConfig.getPort();
    
    if (host == null || port <= 0) {
      if (isLoggingTrace) {
        LOGGER.logp(Level.FINE, CLASS_NAME, methodName,
            "RETURN EARLY Invalid proxy set."
        );
      }
      return ccc;
    }
    
    if (isLoggingDebug) {
      LOGGER.finest(String.format(
          "Setting proxy host %s and port %d", 
          host,
          port
      ));
    }
    
    ccc.setUseProxy(true);
    ccc.setProxyHost(host);
    ccc.setProxyPort(port);
    
    String proxyUser = proxyConfig.getUsername();
    String proxyPass = proxyConfig.getPassword();
    
    if (proxyUser != null && proxyPass != null) {
      if (isLoggingDebug) {
        LOGGER.finest(String.format(
            "Setting proxy user %s with password XXXXX", 
            proxyUser
        ));
      }
      ccc.setProxyUsername(proxyUser);
      ccc.setProxyPassword(proxyPass);
    }
    
    if (isLoggingTrace) {
      LOGGER.exiting(CLASS_NAME, methodName, ccc);
    }
    
    return ccc;
  }

  /**
   * Create a new {@link CloseableHttpClient}.
   * 
   * @param connConfig The connection configuration.
   * @param headers Additional request headers.
   * @return A new {@link CloseableHttpClient} configured based on the given
   *  {@code connConfig} and {@code headers}.
   */
  public CloseableHttpClient createHttpClient(
      ClientConnectionConfiguration connConfig,
      List<Header> headers
  ) {
    final String methodName = "createClient";
    final boolean isLoggingTrace = LOGGER.isLoggable(Level.FINE);
    final boolean isLoggingDebug = LOGGER.isLoggable(Level.FINEST);
    
    if (isLoggingTrace) {
      LOGGER.entering(CLASS_NAME, methodName);
    }
    
    if (isLoggingDebug) {
      LOGGER.finest("Creating request config...");
    }
    
    RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
        .setConnectionRequestTimeout(
            connConfig.getConnectionRequestTimeout()
        )
        .setConnectTimeout(connConfig.getConnectTimeout())
        .setSocketTimeout(connConfig.getSocketTimeout());
    
    if (connConfig.isUseProxy()) {
      if (isLoggingDebug) {
        LOGGER.finest(String.format(
            "Setting HTTP proxy with %s:%d and scheme %s",
            connConfig.getProxyHost(),
            connConfig.getProxyPort(),
            connConfig.getProxyScheme()          
        ));
      }
      requestConfigBuilder = requestConfigBuilder.setProxy(new HttpHost(
          connConfig.getProxyHost(),
          connConfig.getProxyPort(),
          connConfig.getProxyScheme()
      ));
    }
    
    List<Header> newHeaders = new ArrayList<Header>(headers);

    if (connConfig.isUseProxy()) {
      if (
          connConfig.getProxyUsername() != null
          && connConfig.getProxyPassword() != null
      ) {
        String proxyUserAndPass = 
            connConfig.getProxyUsername().trim()
            + ":"
            + connConfig.getProxyPassword().trim();
        if (isLoggingDebug) {
          LOGGER.finest(String.format(
              "Adding Proxy-Authorization using %s and XXXXX",
              connConfig.getProxyUsername(),
              connConfig.getProxyPassword()       
          ));
        }
        newHeaders.add(new BasicHeader(
            "Proxy-Authorization",
            "Basic " + DatatypeConverter.parseString(proxyUserAndPass)
        ));
      }
    }
    
    if (isLoggingTrace) {
      LOGGER.exiting(CLASS_NAME, methodName);
    }
    
    return HttpClientBuilder.create()
        .setDefaultRequestConfig(requestConfigBuilder.build())
        .setDefaultHeaders(newHeaders)
        .setConnectionManager(this.connManager)
        .setConnectionManagerShared(true)
        .build();
  }
  
  /**
   * Create a new {@link CloseableHttpClient} for interacting with the New Relic
   * ReST APIs.
   * 
   * @param connConfig The connection configuration.
   * @param apiKey New Relic ReST API key.
   * @return A new {@link CloseableHttpClient} configured based on the given
   *    {@code connConfig} using the given NR ReST API key.
   */
  public CloseableHttpClient createRestApiClient(
      ClientConnectionConfiguration connConfig,
      String apiKey
  ) {
    final String methodName = "createRestClient";
    final boolean isLoggingTrace = LOGGER.isLoggable(Level.FINE);
    
    
    if (isLoggingTrace) {
      LOGGER.exiting(CLASS_NAME, methodName);
    }
    
    List<Header> headers = new ArrayList<Header>();
    CloseableHttpClient client = null;
    
    headers.add(new BasicHeader(
        PROPERTY_NAME_HEADER_X_API_KEY,
        apiKey
    ));
    
    client = createHttpClient(connConfig, headers);
   
    if (isLoggingTrace) {
      LOGGER.exiting(CLASS_NAME, methodName);
    }
    
    return client;
  }
  
  /**
   * Create a new {@link CloseableHttpClient} for interacting with the New Relic
   * Insights Insert API.
   * 
   * @param connConfig a connection configuration to be used for creating
   *        the client connection.
   * @param insightsApiInsertKey the Insights Insert API key for the
   *        corresponding {@code rpmAccountId}.
   * @return A new {@link CloseableHttpClient} configured based on the given
   *    {@code connConfig} using the given NR Insights Insert API key.
   */
  public CloseableHttpClient createInsightsApiClient(
      ClientConnectionConfiguration connConfig,
      String insightsApiInsertKey
  ) {
    final String methodName = "createInsightsClient";
    final boolean isLoggingTrace = LOGGER.isLoggable(Level.FINE);

    if (isLoggingTrace) {
      LOGGER.entering(CLASS_NAME, methodName);
    }
    
    List<Header> headers = new ArrayList<Header>();
    CloseableHttpClient client = null;
    
    headers.add(new BasicHeader(
        PROPERTY_NAME_HEADER_X_INSERT_KEY,
        insightsApiInsertKey
    ));
    
    client = createHttpClient(connConfig, headers);
   
    if (isLoggingTrace) {
      LOGGER.exiting(CLASS_NAME, methodName);
    }
    
    return client;
  }
  
  /**
   * POST the {@code payload} and return a value of type {@code valueType}.
   * <p>
   * The {@code payload} to send will be mapped to the POST body using a
   * Jackson {@link ObjectMapper}. The return value (if any) from the POST
   * will be mapped to a return value of type {@code valueType} also using
   * a Jackson {@link ObjectMapper}.
   * </p>
   * 
   * @param <V> The type of the return value.
   * @param client The HTTP client to use.
   * @param uri The URI to POST to.
   * @param payload The payload to send.
   * @param valueType The expected type of the return payload.
   * @param gzip A flag to control if the payload will be gzip'ed.
   * @return The return payload of the POST converted to a {@code valueType}
   *     instance using a Jackson {@link ObjectMapper}.
   * @throws NewRelicClientException if any type of error occurs during the
   *     POST.
   */
  public <V> V post(
      CloseableHttpClient client,
      URI uri,
      Object payload,
      Class<V> valueType,
      boolean gzip
  ) throws NewRelicClientException {
    final String methodName = "post";
    final boolean isLoggingTrace = LOGGER.isLoggable(Level.FINE);
    final boolean isLoggingDebug = LOGGER.isLoggable(Level.FINEST);

    if (isLoggingTrace) {
      LOGGER.entering(CLASS_NAME, methodName, new Object [] {
          uri
      });
    }
    
    if (isLoggingDebug) {
      LOGGER.finest(String.format(
          "Creating client with URL %s", 
          uri
      ));
    }
    
    HttpPost postRequest = new HttpPost(uri);
    CloseableHttpResponse response = null;
    HttpEntity entity = null;
    
    try {
      ByteArrayOutputStream jsonBytes = new ByteArrayOutputStream();
      OutputStream out = (
          gzip ? new GZIPOutputStream(jsonBytes) : jsonBytes
      );
      
      out.write(this.mapper.writeValueAsBytes(payload));
      out.flush();
      out.close();
      
      if (isLoggingDebug) {
        LOGGER.finest(
            String.format(
              "Posting to %s",
              postRequest.getURI()
            )
        );
      }
      
      EntityBuilder entityBuilder = EntityBuilder.create()
          .setBinary(jsonBytes.toByteArray())
          .setContentType(ContentType.APPLICATION_JSON);
      
      if (gzip) {
        entityBuilder.setContentEncoding("gzip");
      }
      
      postRequest.setEntity(entityBuilder.build());
      response = client.execute(postRequest);
      int status = response.getStatusLine().getStatusCode();
      entity = response.getEntity();
      if (status < 200 || status >= 300) {
        LOGGER.log(Level.SEVERE,
            String.format("The HTTP post failed with status code %s",
                response.getStatusLine()
            )
        );
        throw new NewRelicClientException(
            Messages.NewRelicApiHelper_errors_HttpPostError(
              response.getStatusLine()
            )
        );
      }

      return this.mapper.readValue(
          new BufferedReader(new InputStreamReader(entity.getContent())), 
          valueType
      );
    } catch (UnsupportedOperationException | IOException exc) {
      LOGGER.log(Level.SEVERE,
          String.format("The HTTP post failed with exception: %s",
              exc.getClass().getName())
      );
      throw new NewRelicClientException(exc);
    } finally {
      if (entity != null) {
        EntityUtils.consumeQuietly(entity);
      }
      if (response != null) {
        try {
          response.close();
        } catch (IOException ignore) {
          LOGGER.log(Level.WARNING, "Ignoring exception on response close");
        }
      }
      if (isLoggingTrace) {
        LOGGER.exiting(CLASS_NAME, methodName);
      }
    }
  }
  
  /**
   * GET the {@code uri} as a value of type {@code valueType}.
   * <p>
   * The return value (if any) from the POST will be mapped to a return value
   * of type {@code valueType} also using a Jackson {@link ObjectMapper}.
   * </p>
   * 
   * @param <V> The type of the return value.
   * @param client The HTTP client to use.
   * @param uri The URI to GET.
   * @param valueType The expected type of the return payload.
   * @return The return payload of the GET converted to a {@code valueType}
   *     instance using a Jackson {@link ObjectMapper}.
   * @throws NewRelicClientException if any type of error occurs during the
   *     GET.
   */
  public <V> V get(
      CloseableHttpClient client,
      URI uri,
      Class<V> valueType
  ) throws NewRelicClientException {
    final String methodName = "get";
    final boolean isLoggingTrace = LOGGER.isLoggable(Level.FINE);
    final boolean isLoggingDebug = LOGGER.isLoggable(Level.FINEST);

    if (isLoggingTrace) {
      LOGGER.entering(CLASS_NAME, methodName, new Object [] {
          client, uri
      });
    }
    
    if (isLoggingDebug) {
      LOGGER.finest(String.format(
          "Creating client with URL %s", 
          uri
      ));
    }
    
    HttpGet getRequest = new HttpGet(uri);
    CloseableHttpResponse response = null;
    HttpEntity entity = null;
    
    try {
      if (isLoggingDebug) {
        LOGGER.finest(
            String.format(
              "Getting from %s",
              getRequest.getURI()
            )
        );
      }
      
      response = client.execute(getRequest);
      int status = response.getStatusLine().getStatusCode();
      entity = response.getEntity();
      if (status < 200 || status >= 300) {
        LOGGER.log(Level.SEVERE,
            String.format("The HTTP get failed with status code %s",
                response.getStatusLine()
            )
        );
        throw new NewRelicClientException(
            Messages.NewRelicApiHelper_errors_HttpGetError(status)
        );
      }
      
      return this.mapper.readValue(
          new BufferedReader(new InputStreamReader(entity.getContent())),
          valueType
      );
    } catch (UnsupportedOperationException | IOException exc) {
      LOGGER.log(Level.SEVERE,
          String.format("The HTTP get failed with exception: %s",
              exc.getClass().getName())
      );
      throw new NewRelicClientException(exc);
    } finally {
      if (entity != null) {
        EntityUtils.consumeQuietly(entity);
      }
      if (response != null) {
        try {
          response.close();
        } catch (IOException ignore) {
          LOGGER.log(Level.WARNING, "Ignoring exception on response close");
        }
      }
      if (isLoggingTrace) {
        LOGGER.exiting(CLASS_NAME, methodName);
      }
    }
  }
}