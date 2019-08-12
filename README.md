# New Relic Jenkins Plugin
The New Relic Jenkins Plugin is a Jenkins plugin that provides various
mechanisms for reporting information from your Jenkins server to New Relic.

The following integrations are supported.

1. Application build events (initialized, started, finalized, etc) with various
   build metrics reported as custom attributes are automatically pushed into
   Insights using the Insights Insert API.
1. APM deployment markers can be pushed to New Relic using the
   "Record New Relic Deployment Marker" notifier by adding a post-build step. 
1. Application deployment events that mirror the APM deployment markers can
   be pushed into Insights whenever a deployment marker is sent using the
   "Create Insights Deployment Event" check box on the
   "Record New Relic Deployment Marker" notifier.
  
## Installation
To install the New Relic Jenkins plugin, perform the following steps.

1. [Build](#build) the New Relic Jenkins Plugin `.hpi` file or [download the
   latest plugin release](/releases)
1. Login to your Jenkins server as an adminstrator
1. Navigate to `/jenkins/pluginManager/advanced`
1. Locate the section labeled "Upload Plugin"
1. Click on the button labeled "Choose File"
1. Navigate to the New Relic Jenkins Plugin `.hpi` file and select it
1. Click on the button labeled "Upload"
1. Restart Jenkins

## Usage
1. [Register an Insights Event API insert key](https://docs.newrelic.com/docs/insights/insights-data-sources/custom-data/send-custom-events-event-api#register) <sup>New Relic</sup>
1. [Install the plugin](#installation) <sup>Jenkins</sup>
1. [Setup the required credentials](#setup-credentials) <sup>Jenkins</sup>
1. [Setup the proxy configuration (optional)](#setup-proxy) <sup>Jenkins</sup>
1. [Create custom dashboards!](#dashboards) <sup>New Relic</sup>

### Setup credentials
The [Jenkins Credentials](https://wiki.jenkins.io/display/JENKINS/Credentials+Plugin)
plugin is required to store the New Relic API keys used by the New Relic
Jenkins plugin.  To setup the required credentials, perform the following steps.

1. Login to your Jenkins server as an adminstrator
1. Navigate to `/jenkins/credentials/store/system/domain/_/`
1. Locate the section labeled "Upload Plugin"
1. Click on the button labeled "Add Credentials"
1. Ensure that "Username with password" is selected from the "Kind" dropdown
   menu
1. Ensure "Global (...)" is selected from the "Scope" dropdown menu
1. Enter your RPM account ID in the "Username" field
1. Enter your Insights Insert API Key in the "Password" field
1. Enter a value for the "ID" field, e.g. "insights-insert-key"
1. Optionally enter a value for the "Description" field.
1. Click on the button labeled "OK"
1. Navigate to `/jenkins/configure`
1. Scroll down to the section labeled "New Relic"
1. Select the key you just created in the dropdown labeled
   "Insights Insert Credentials"
1. Click on the button labeled "Save"

### Setup proxy
The New Relic Jenkins Plugin supports the use of
[the global Jenkins Proxy Configuration](https://wiki.jenkins.io/display/JENKINS/JenkinsBehindProxy)
for allowing Jenkins installations behind proxies to send data to New Relic.  To
setup the proxy configuration, perform the following steps.

1. Login to your Jenkins server as an adminstrator
1. Navigate to `/jenkins/pluginManager/advanced`
1. Locate the section labeled "HTTP Proxy Configuration"
1. Enter the host name of your proxy server in the field labeled "Server"
1. Enter the port of your proxy server in the field labeled "Port"
1. If your proxy server requires HTTP Proxy Authentication:
   1. Enter the proxy server username in the "User name" field
   1. Enter the proxy server password in the "Password" field
1. Optionally enter host name patterns for connections that should *not* go
   through the proxy in the field labeled "No Proxy Host", one per line.
1. Click on the button labeled "Submit"

### Dashboards
Once installed and configured, the New Relic Jenkins Plugin will immediately
start sending build events for all builds in Jenkins to Insights.  Build events
are collected and reported to Insights in 1 minute harvest cycles.  Insights
dashboards can use the custom event type `AppBuildEvent` in NRQL queries to
display Jenkins build data.

#### AppBuildEvent
Each application build event is represented by a custom Insights event of type
`AppBuildEvent`.  `AppBuildEvent`s have the following attributes.

| Attribute name | Attribute description | Example value(s) |
| --- | --- | --- |
| provider | Name of the CI/CD provider | Jenkins |
| providerVersion | Version of the CI/CD provider | 2.140 |
| buildId | The numeric build ID of the build | 65 |
| buildUrl | The relative URL of the build | job/my%20job/65 |
| buildName | The short name of the build | #65 |
| buildFullName | The full "display name of the build | my job #65 |
| buildEventType | One of initialized, started, completed, finalized | started |
| buildQueueId | The numeric queue ID on which the build runs | 10 |
| buildMessage | A human readable string for the build event | Completed build "my job #65" for job "my job" |
| buildResult | One of SUCCESS, FAILURE | SUCCESS |
| buildScheduled | Time the build was scheduled to start | April 19, 2019 16:48 |
| buildStarted | Time the build was actually started | April 19, 2019 16:49 |
| buildStartDelay | The delay between the build scheduled and start time, if any, in milliseconds | 0 |
| buildDuration | The total duration, in milliseconds, of the build | 2,748 |
| buildStatusSummary | The Jenkins build status | "broken since this build", "back to normal" |
| buildAgentName | Name of the node where the build ran | master |
| buildAgentDesc | Description of the node where the build ran | master |
| buildAgentLabels | The labels of the node where the build ran, separated by "\|" | master\|docker\|macos |
| buildAgentHost | The host name or IP of the node where the build ran | master\|docker\|macos |
| jobUrl | The relative URL of the Jenkins job for the build | job/my%20job/ |
| jobName | The short name of the Jenkins job for the build | my job |
| jobFullName | The full "display" name of the Jenkins job for the build | My Job |
| jenkinsMasterLabels | The node labels of the Jenkins master node, separated by "\|" | master\|docker\|macos |
| jenkinsMasterHost | The host name or IP of the Jenkins master node | master-jenkins.myco.com |

In addition to the attributes above, each job supports sending custom attributes
with the `AppBuildEvent` as well as a switch to disable events for the job from
being reported via the "Customize New Relic build event settings".

#### AppDeploymentEvent
`AppDeploymentEvent`s can be created simultaneously with APM deployment markers
by selecting the "Create Insights Deployment Event" check box on the
"Record New Relic Deployment Marker" notifier when creating a post-build step.
`AppDeploymentEvent`s have the following attributes.

| Attribute name | Attribute description | Example value(s) |
| --- | --- | --- |
| appId | The APM application ID | 2536781 |
| revision | The revision string for the deployment marker | prod-master-13.3 |
| changelog | The change log string for the deployment marker | Added bug fix for #14 |
| description | The description string for the deployment marker | The build for prod-master-13.3 |
| user | The user string for the deployment marker | beeker@muppetmaster.com |

#### Example NRQL queries
Below are some sample NRQL queries that can be used to visualize build event
information.

**Build count billboard**
```sql
SELECT uniqueCount(buildId) AS Builds, filter(uniqueCount(buildId), WHERE buildEventType='finalized' AND buildResult ='SUCCESS') AS 'Passed', filter(uniqueCount(buildId), WHERE buildEventType='finalized' AND buildResult != 'SUCCESS')  AS 'Failed' FROM AppBuildEvent SINCE TODAY
```

**Build results timeseries**
```sql
SELECT filter(uniqueCount(buildId), WHERE buildEventType='finalized' AND buildResult ='SUCCESS') AS 'Passed', filter(uniqueCount(buildId), WHERE buildEventType='finalized' AND buildResult != 'SUCCESS')  AS 'Failed' FROM AppBuildEvent TIMESERIES
```

**Build count by job bar chart**
```sql
SELECT uniqueCount(buildId) AS Builds FROM AppBuildEvent FACET jobName
```

**Average build duration timeseries**
```sql
SELECT average(buildDuration), max(buildDuration), min(buildDuration) FROM AppBuildEvent TIMESERIES
```

**Average build duration by job bar chart**
```sql
SELECT average(buildDuration) FROM AppBuildEvent FACET jobName
```

**Average build duration by job timeseries**
```sql
SELECT average(buildDuration) FROM AppBuildEvent FACET jobName TIMESERIES
```

**Today's builds vs yesterday's builds timeseries**
```sql
SELECT uniqueCount(buildId) AS Builds FROM AppBuildEvent SINCE TODAY COMPARE WITH 1 DAY AGO TIMESERIES
```

**Average build delay timeseries**
```sql
SELECT average(buildDelay) FROM AppBuildEvent
```

**Build message event stream**
```sql
SELECT buildMessage FROM AppBuildEvent LIMIT 50
```

## Troubleshooting
To troubleshoot issues, the first steps is creating a new Jenkins logger to
view the New Relic Jenkins logs.  This is accomplished as follows.

1. Login to your Jenkins server as an adminstrator
1. Navigate to `/jenkins/log`
1. Click on the button labeled "Add new log recorder"
1. In the field labeled "Name", enter "New Relic Logger"
1. Click on the button labeled "OK"
1. Click on the button labeled "Add" next to the label "Loggers"
1. In the field labeled "Logger", enter "com.newrelic.experts"
1. Select the value "ALL" from the menu labeled "Log level"
1. Click on the button labeled "Save"

Once this logger has been created, logs can be viewed as follows.

1. Login to your Jenkins server as an adminstrator
1. Navigate to `/jenkins/log`
1. Click on the link labeled "New Relic Logger"
1. This page will refresh every 30s to display detailed logging output for
   the New Relic Jenkins plugin

### Build
[As with most Jenkins plugins](https://jenkins.io/doc/developer/tutorial/prepare/),
this plugin uses Maven to build.

1. Clone this repository
1. Run `mvn package`

The Jenkins HPI file will be built and placed into the `target` directory, e.g.
`[project-root]/newrelic-jenkins-plugin.hpi`

## Privacy Disclaimer
The New Relic Jenkins plugin collects and sends data from your Jenkins servers
to the New Relic service. Pursuant to section 1.8 of the [New Relic Terms &
Conditions](https://newrelic.com/termsandconditions/terms),

> You shall not process or submit to the Services any Customer Data that could
> be legally considered sensitive in any applicable jurisdiction.

Please refer to section 1.8 of the [New Relic Terms & Conditions](https://newrelic.com/termsandconditions/terms)
for more information.

## Contributing
Full details are available in our [CONTRIBUTING.md](CONTRIBUTING.md) file.

We'd love to get your contributions to improve AWS Lambda OpenTracing Java SDK!
Keep in mind when you submit your pull request, you'll need to sign the CLA via
the click-through using CLA-Assistant. You only have to sign the CLA one time
per project.

To execute our corporate CLA, which is required if your contribution is on
behalf of a company, or if you have any questions, please drop us an email at
open-source@newrelic.com.
 
## Licensing
The New Relic Jenkins Plugin is [licensed under the Apache 2.0 License](LICENSE).

The New Relic Jenkins Plugin also uses source code from third party libraries.
Full details on which libraries are used and the terms under which they are
licensed can be found in the
[third party notices document](THIRD_PARTY_NOTICES.md).
