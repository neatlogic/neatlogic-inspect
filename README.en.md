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