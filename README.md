中文 / [English](README.en.md)
<p align="left">
    <a href="https://opensource.org/licenses/Apache-2.0" alt="License">
        <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" /></a>
<a target="_blank" href="https://join.slack.com/t/neatlogichome/shared_invite/zt-1w037axf8-r_i2y4pPQ1Z8FxOkAbb64w">
<img src="https://img.shields.io/badge/Slack-Neatlogic-orange" /></a>
</p>

---

## 关于

neatlogic-inspect是巡检模块，巡检模块可以通过发起巡检作用获取资产各项指标的实时数据，与已设置的指标规则进行对比，并汇总资产问题，主要包括巡检定义、资产巡检、应用巡检和查看巡检结果等功能。
neatlogic-inspect需要自动化模块[neatlogic-autoexec](../../../neatlogic-autoexec/blob/develop3.0.0/README.md)提供支持。

## 主要功能

### 巡检定义

巡检定义的主要功能是管理数据集合的全局指标规则和设置模型巡检工具的页面，支持导出数据集合的指标和规则。
![img.png](README_IMAGES/img1.png)
![img.png](README_IMAGES/img.png)

### 资产巡检

资产巡检是对资产执行巡检的页面，巡检的方式包括批量巡检和定时巡检。
![img.png](README_IMAGES/img2.png)
![img.png](README_IMAGES/img3.png)

- 支持根据需求搜索资产对象，并且以当前搜索结果为批量巡检的巡检范围。
- 支持查看资产的详情
- 支持查看巡检报告
  ![img.png](README_IMAGES/img4.png)
- 支持查看巡检作业
- 支持查看资产的阈值规则（指标规则）

### 应用巡检

应用巡检是按应用范围发起巡检的页面，巡检的方式支持应用批量巡检和应用定时巡检。
![img.png](README_IMAGES/img5.png)

- 支持添加、编辑和删除应用及模块
- 支持查看应用、模块的最新问题和资产清单
- 支持在应用层重定义阈值规则（指标规则）
  ![img.png](README_IMAGES/img6.png)
- 支持查看资产巡检报告
- 支持查看巡检作业
- 支持查看资产的详情

### 巡检作业

巡检作业页面展示所有巡检作业，可根据需求搜索作业，支持查看作业详情和巡检报告。
![img.png](README_IMAGES/img7.png)

### 最新问题

最新问题页面是所有资产的巡检结果问题汇总。
![img.png](README_IMAGES/img8.png)

- 支持用户根据需求配置过滤条件，并保存为个人分类。
- 支持导出搜索结果中的问题汇总。
- 支持将问题汇总通过邮件发送到指定收件人的邮箱。