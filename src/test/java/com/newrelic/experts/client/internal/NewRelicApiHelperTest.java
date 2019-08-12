/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.client.internal;

import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.conn.HttpClientConnectionManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@Test(groups = { "unit" })
public class NewRelicApiHelperTest {

  private HttpClientConnectionManager connManagerStub;
  private ObjectMapper objMapperStub;
  private NewRelicApiHelper apiHelper;
  
  @BeforeMethod
  public void setUp() {
    this.connManagerStub = mock(HttpClientConnectionManager.class);
    this.objMapperStub = mock(ObjectMapper.class);
    this.apiHelper = new NewRelicApiHelper(
        this.connManagerStub,
        this.objMapperStub
    );
  }
  
  @Test
  public void buildUriShouldThrowSyntaxExceptionGivenBadUrl() {
    try {
      this.apiHelper.buildUri("^ ^ ^", null);
      Assert.fail("buildUri() should have thrown an exception");
    } catch (URISyntaxException urise) {
      Assert.assertNotNull(urise);
    } 
  }

  @Test
  public void buildUriShouldReturnUriGivenValidUrl() {
    try {
      URI uri = this.apiHelper.buildUri("https://www.newrelic.com", null);
      Assert.assertNotNull(uri);
      Assert.assertEquals(uri.getScheme(), "https");
      Assert.assertEquals(uri.getHost(), "www.newrelic.com");
      Assert.assertNull(uri.getQuery());
    } catch (URISyntaxException urise) {
      Assert.fail("buildUri() threw an exception but should have succeeded");
    } 
  }
  
  @Test
  public void buildUriShouldReturnUriWithQueryGivenUrlAndParams() {
    try {
      Map<String, String> params = new HashMap<String, String>();
      params.put("foo", "bar");
      
      URI uri = this.apiHelper.buildUri(
          "https://www.newrelic.com",
          params
      );
      Assert.assertNotNull(uri);
      Assert.assertEquals(uri.getScheme(), "https");
      Assert.assertEquals(uri.getHost(), "www.newrelic.com");
      Assert.assertNotNull(uri.getQuery());
      Assert.assertEquals(uri.getRawQuery(), "foo=bar");
    } catch (URISyntaxException urise) {
      Assert.fail("buildUri() threw an exception but should have succeeded");
    } 
  }
}
