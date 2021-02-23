package com.newrelic.experts.jenkins.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.experts.client.model.Event;

import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import hudson.util.TagCloud;
import hudson.util.VersionNumber;

import jenkins.model.Jenkins;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Jenkins.class)
public class EventHelperTest {

  private void configureSetLabelsMocks(
      Node mockNode,
      String[] labels
  ) {
    SortedSet<LabelAtom> labelSet = new TreeSet<LabelAtom>();
    
    for(String label : labels) {
      labelSet.add(new LabelAtom(label));
    }
    
    TagCloud<LabelAtom> tagCloud =  new TagCloud<>(labelSet, new TagCloud.WeightFunction<LabelAtom>() {
      public float weight(LabelAtom item) {
        return 1;
      }
    });
    
    when(mockNode.getLabelCloud()).thenReturn(tagCloud);
  }
  
  private void configureSetHostnameMocks(
      Node mockNode,
      String hostname
  ) throws IOException, InterruptedException {
    Computer computer = mock(Computer.class);
    
    when(mockNode.toComputer()).thenReturn(computer);
    when(computer.getHostName()).thenReturn(hostname);
  }
  
  @SuppressWarnings("deprecation")
  private void configureSetCommonAttributesMocks(
      String version,
      String[] labels,
      String hostname
  ) throws IOException, InterruptedException {
    PowerMockito.mockStatic(Jenkins.class);
    Jenkins jenkins = mock(Jenkins.class);
    when(Jenkins.getInstance()).thenReturn(jenkins);
    when(Jenkins.getVersion()).thenReturn(new VersionNumber(version));
    configureSetLabelsMocks(jenkins, labels);
    configureSetHostnameMocks(jenkins, hostname);
  }
  
  private void verifyLabels(Event event, String attributeName, String[] labels) {
    String actualLabels = (String)event.get(attributeName);
    List<String> labelsList = Arrays.asList(labels);
    labelsList.sort(null);
    String expectedLabels = String.join("|", labelsList);
    
    assertNotNull(actualLabels);
    assertEquals(expectedLabels, actualLabels);
  }
  
  private void verifyHostname(Event event, String attributeName, String hostname) {
    String actualHostname = (String)event.get(attributeName);
    
    assertNotNull(actualHostname);
    assertEquals(hostname, actualHostname);
  }
  
  private void verifyCommonAttributes(
      Event event,
      String version,
      String[] labels,
      String hostname
  ) {
    assertEquals("Jenkins", (String)event.get("provider"));
    assertEquals(version, (String)event.get("providerVersion"));
    verifyLabels(event, "jenkinsMasterLabels", labels);
    verifyHostname(event, "jenkinsMasterHost", hostname);
  }
  
  @Test
  public void testSetLabels() {
    // Setup the test
    Event event = new Event("TestEvent");
    Node node = mock(Node.class);
    String[] labels = new String[] {
        "newrelic",
        "jenkins",
        "plugin"
    };
    
    configureSetLabelsMocks(node, labels);
    
    // Execute the test
    EventHelper eventHelper = new EventHelper();
    eventHelper.setLabels(event, "labels", node);
    
    // Verify the result
    verifyLabels(event, "labels", labels);
  }

  @Test
  public void testSetHostname() {
    // Setup the test
    Event event = new Event("TestEvent");
    Node node = mock(Node.class);    
    String hostname = "www.newrelic.com";
    
    try {
      configureSetHostnameMocks(node, hostname);
      
      // Execute the test
      EventHelper eventHelper = new EventHelper();
      eventHelper.setHostname(event, "hostname", node);
      
      // Verify the result
      verifyHostname(event, "hostname", hostname);
    } catch (IOException|InterruptedException t) {
      t.printStackTrace();
      fail("Mock setup failed");
    }
  }

  @Test
  public void testSetCommonAttributes() {
    Event event = new Event("TestEvent");
    String[] labels = new String[] {
        "newrelic",
        "jenkins",
        "plugin"
    };
    String hostname = "www.newrelic.com";
    String version = "1.289";

    try {
      configureSetCommonAttributesMocks(version, labels, hostname);
      
      // Execute the test
      EventHelper eventHelper = new EventHelper();
      eventHelper.setCommonAttributes(event);
      
      // Verify the result
      verifyCommonAttributes(event, version, labels, hostname);
    } catch (IOException|InterruptedException t) {
      t.printStackTrace();
      fail("Mock setup failed");
    }
  }

  @Test
  public void testRecordEvent() {
    Event event = new Event("TestEvent");
    String[] labels = new String[] {
        "newrelic",
        "jenkins",
        "plugin"
    };
    String hostname = "www.newrelic.com";
    String version = "1.289";

    try {
      configureSetCommonAttributesMocks(version, labels, hostname);
      
      // Execute the test
      EventHelper eventHelper = new EventHelper();
      eventHelper.recordEvent(event);
      
      // Verify the result
      Event[] events = eventHelper.popEvents();
      
      assertNotNull(events);
      assertEquals(1, events.length);
      verifyCommonAttributes(events[0], version, labels, hostname);
    } catch (IOException|InterruptedException t) {
      t.printStackTrace();
      fail("Mock setup failed");
    }
  }

  @Test
  public void testPopEvents() {
    String[] labels = new String[] {
        "newrelic",
        "jenkins",
        "plugin"
    };
    String hostname = "www.newrelic.com";
    String version = "1.289";
  
    try {
      // Execute the test
      EventHelper eventHelper = new EventHelper();

      for (int index = 0; index < 5; index += 1) {
        Event event = new Event("TestEvent" + index);
        String[] newLabels = labels.clone();
        
        for (int jindex = 0; jindex < newLabels.length; jindex += 1) {
          newLabels[jindex] = newLabels[jindex] + index;
        }
        
        configureSetCommonAttributesMocks(
            version + index,
            newLabels,
            hostname + index
        );
        
        eventHelper.recordEvent(event);        
      }

      
      // Verify the result
      Event[] events = eventHelper.popEvents();
      
      assertNotNull(events);
      assertEquals(5, events.length);
      for (int index = 0; index < 5; index += 1) {
        String[] newLabels = labels.clone();
        
        for (int jindex = 0; jindex < newLabels.length; jindex += 1) {
          newLabels[jindex] = newLabels[jindex] + index;
        }

        verifyCommonAttributes(
            events[index],
            version + index,
            newLabels,
            hostname + index
        );
      }
    } catch (IOException|InterruptedException t) {
      t.printStackTrace();
      fail("Mock setup failed");
    }
  }

}
