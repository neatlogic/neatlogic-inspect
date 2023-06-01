[中文](README.md) / English
<p align="left">
    <a href="https://opensource.org/licenses/Apache-2.0" alt="License">
        <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" /></a>
<a target="_blank" href="https://join.slack.com/t/neatlogichome/shared_invite/zt-1w037axf8-r_i2y4pPQ1Z8FxOkAbb64w">
<img src="https://img.shields.io/badge/Slack-Neatlogic-orange" /></a>
</p>

---

## About

neatlogic-inspect is a patrol module that can obtain real-time data on various indicators of assets through the
initiation of patrols, compare them with established indicator rules, and summarize asset issues. The patrol module
mainly includes functions such as patrol definition, asset patrols, application patrols, and viewing patrol results.

## Feature

### Inspection Definition

The main function of the inspection definition is to manage the global indicator rules of the data set and set the page
of the model inspection tool, supporting the export of indicators and rules of the data set.
![img.png](README_IMAGES/img1.png)
![img.png](README_IMAGES/img.png)

### Asset Inspection

Asset inspection is a page that performs inspections on assets, including batch inspections and scheduled inspections.
![img.png](README_IMAGES/img2.png)
![img.png](README_IMAGES/img3.png)

- Support searching for asset objects based on requirements, and use the current search results as the inspection scope
  for batch inspections.
- Support for viewing asset details
- Support viewing inspection reports
  ![img.png](README_IMAGES/img4.png)
- Support viewing inspection tasks
- Support threshold rules (indicator rules) for viewing assets

### Application inspection

Application inspection is a page that initiates inspections based on the application scope. The inspection method
supports batch inspection of applications and scheduled inspection of applications.
![img.png](README_IMAGES/img5.png)

- Support for adding, editing, and deleting applications and modules
- Support for viewing the latest issues and asset lists of applications and modules
- Support redefining threshold rules (indicator rules) at the application layer
  ![img.png](README_IMAGES/img6.png)
- Support for viewing asset inspection reports
- Support viewing inspection tasks
- Support for viewing asset details

### Inspection job

The inspection task page displays all inspection tasks, and allows for searching for tasks according to needs. It
supports viewing task details and inspection reports.
![img.png](README_IMAGES/img7.png)

### Latest Problem

The latest problem page is a summary of inspection results and problem for all assets.
![img.png](README_IMAGES/img8.png)

- Support users to configure filtering conditions based on their needs and save them as personal categories.
- Support exporting problem summaries in search results.
- Support for summarizing issues and sending them via email to the designated recipient's email address.

## All Features

<table border="1"><tr><td>Number</td><td>Category</td><td>Feature</td><td>Description</td></tr><tr><td>1</td><td rowspan="15">Inspection Management</td><td rowspan="2">Inspection Definition</td><td>Supports threshold definition for inspection plugin metrics within the scope of inspection.</td></tr><tr><td>2</td><td>Supports configuration of inspection object thresholds from an application perspective.</td></tr><tr><td>3</td><td rowspan="6">Application Inspection</td><td>Allows viewing of the application inspection resource inventory in a tree structure based on application systems, application modules, and environments.</td></tr><tr><td>4</td><td>Enables scheduled inspections for individual applications within the scope of inspection.</td></tr><tr><td>5</td><td>Supports manual initiation of inspections for individual applications, application modules, and environments.</td></tr><tr><td>6</td><td>Enables export of the latest problem list for application inspections.</td></tr><tr><td>7</td><td>Supports email notifications for application inspection problem lists.</td></tr><tr><td>8</td><td>Enables export of inspection reports from an application perspective.</td></tr><tr><td>9</td><td rowspan="4">Asset Inspection</td><td>Allows viewing of asset inspection objects from the perspective of assets and functional positions.</td></tr><tr><td>10</td><td>Supports scheduled inspections for specific types of assets.</td></tr><tr><td>11</td><td>Allows manual initiation of inspections for individual asset objects.</td></tr><tr><td>12</td><td>Enables export of inspection reports for individual asset objects.</td></tr><tr><td>13</td><td rowspan="3">Configuration Inspection</td><td>Supports inspection of application, operating system, and network configuration file backup content.</td></tr><tr><td>14</td><td>Supports defining the path of inspection configuration files and supports wildcard expressions in the asset inventory interface.</td></tr><tr><td>15</td><td>Automatically generates versions when configuration files change and supports online comparison of configuration file version differences.</td></tr><tr><td>16</td><td rowspan="2">Inspection Methods</td><td rowspan="2">Inspection Methods</td><td>Provides general inspection collection plugins that match the actual inspection scope of users.</td></tr><tr><td>17</td><td>Supports user-defined script-based inspections in common scripting languages, including Python, Ruby, VBScript, Perl, PowerShell, CMD, Bash, csh, ksh, sh, and JavaScript.</td></tr><tr><td>18</td><td rowspan="3">Inspection Results</td><td rowspan="3">Latest Problems</td><td>Allows quick searching of inspection assets by application, asset, inspection status, and other conditions.</td></tr><tr><td>19</td><td>Enables quick viewing of problem lists for assets by type.</td></tr><tr><td>20</td><td>Supports export of the latest problem lists.</td></tr><tr><td>21</td><td rowspan="12">Inspection Scope</td><td rowspan="4">Application Inspection</td><td>Supports HTTP URL simulation: The system can simulate HTTP request sequences to inspect relevant performance metrics such as response status and response time.</td></tr><tr><td>22</td><td>Supports ICMP detection: The system simulates ICMP request sequences and returns indicators such as system response time.</td></tr><tr><td>23</td><td>Supports packet sequence detection: The system simulates Socket requests and returns information such as system response time and status.</td></tr><tr><td>24</td><td>Simulates user access to the system to confirm system availability and inspect system access and functional usage time.</td></tr><tr><td>25</td><td>Operating System Inspection</td><td>Supports inspections of Windows, Linux, and AIX operating systems in multiple versions, collecting data such as host running status, CPU, memory, storage, I/O, inbound/outbound network traffic, etc.</td></tr><tr><td>26</td><td>Virtualization Inspection</td><td>Supports inspections of vCenter, VMware, and Huawei FusionCompute virtualization devices.</td></tr><tr><td>27</td><td>Middleware Inspection</td><td>Supports inspections of middleware resources such as weblogic, Tomcat, Apache, Jetty, WebSphere, tuxedo, Nginx, etc., collecting middleware running status.</td></tr><tr><td>28</td><td>Database Inspection</td><td>Supports inspections of Oracle, MySQL, SQL Server, MongoDB, PostgreSQL databases, etc., collecting database running status.</td></tr><tr><td>29</td><td>Network Inspection</td><td>Supports inspections of network devices/objects such as network switches, F5 servers, firewalls, dedicated lines, etc., collecting device/object running status, resource consumption, etc. Covers common network switch and firewall brands.</td></tr><tr><td>30</td><td>Container Inspection</td><td>Supports inspections of the health and performance status of running Docker containers and supports inspections of applications within Docker containers.</td></tr><tr><td>31</td><td>Storage Inspection</td><td>The specific storage device depends on the customer's on-site environment, and supports inspections of IBM DS series, IBM Flash series, IBM V7000 series, IBM SVC, IBM FlashSystem 900, EMC RPA, EMC VNX, NetApp, HDS VSP series, HDS AMS series, etc.</td></tr><tr><td>32</td><td>Server Hardware Inspection</td><td>Supports inspections of servers from Dell, IBM, Inspur, Huawei, etc., using out-of-band management network cards.</td></tr></table>