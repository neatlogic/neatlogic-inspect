<!DOCTYPE html>
<html>
<head lang="en">
    <meta charset="UTF-8"/>
    <style>
        <#if DATA.docType == 'pdf'>
        body {
            font-family: "SimSun";
        }
        </#if>

        .innerTable {
            width: 100%;
            text-align: center;
            border-collapse: collapse;
        }

        .innerTable th, .innerTable td {
            border: 1px solid #000;
            font-weight: normal;
        }

        .lineTd {
            text-align: right;
            word-break: break-all;
            word-wrap: break-word;
        }

        .text-success {
            color: #25b865;
        }

        .text-warning {
            color: #f9a825;
        }

        .text-error {
            color: #f71010;
        }

        .bg-error {
            color: #f71010;
        }

        .title {
            font-size: 20px;
            text-align: center;
            font-weight: bold;
        }

        .userAndTime {
            text-align: center;
        }
    </style>
</head>

<body>
<#if DATA.reportName??>
    <p class="title">${DATA.reportName}</p>
</#if>
<#if DATA.execUser?? || DATA.reportTime>
    <p class="userAndTime"><span>${DATA.execUser}</span>&nbsp;<span>${DATA.reportTime}</span></p>
</#if>
<#--正文-->
<table class="innerTable">
    <tbody>
    <#assign alertLevelClassMap = DATA.alertLevelClassMap/>
    <#--告警列表-->
    <#if DATA.alert?? && alertLevelClassMap??>
        <#assign headList = DATA.alert.headList/>
        <#assign rowList = DATA.alert.rowList/>
        <#if rowList?? && rowList?size gt 0>
            <tr>
                <td>告警</td>
                <td>
                    <table class="innerTable">
                        <thead>
                        <tr>
                            <#list headList as head>
                                <th>${head}</th>
                            </#list>
                        </tr>
                        </thead>
                        <tbody>
                        <#list rowList as value>
                            <tr class="${alertLevelClassMap[value["level"]]}">
                                <#list headList as head>
                                    <td>
                                        ${value[head]}
                                    </td>
                                </#list>
                            </tr>
                        </#list>
                        </tbody>
                    </table>
                </td>
            </tr>
        </#if>
    </#if>

    <#assign lineList = DATA.lineList/>
    <#if lineList?? && lineList?size gt 0>
        <#assign i = 1/>
        <#list lineList as line>
            <#assign key = line.key/>
            <#assign value = line.value/>
            <tr>
                <td>${line.key}</td>
                <td class="lineTd <#if alertLevelClassMap?? && line.alertLevel??> ${alertLevelClassMap[line.alertLevel]} </#if>">${line.value}</td>
            </tr>
            <#assign i++/>
        </#list>
    </#if>

    <#assign tableList = DATA.tableList/>
    <#if tableList?? && tableList?size gt 0>
        <#list tableList as value>
            <tr>
                <td>${value.key}</td>
                <td>
                    <@getTable table = value alertLevelClassMap = alertLevelClassMap/>
                </td>
            </tr>
        </#list>
    </#if>
    </tbody>
</table>
</body>
</html>

<#macro getTable table alertLevelClassMap>
    <table class="innerTable">
        <thead>
        <tr>
            <#list table.headList as head>
                <th>${head}</th>
            </#list>
        </tr>
        </thead>
        <tbody>
        <#assign valueList = table.valueList/>
        <#if valueList?? && valueList?size gt 0>
            <#list valueList as value>
                <tr>
                    <#list table.headList as head>
                        <#assign cell = value[head]/>
                        <#if cell?is_hash>
                            <td>
                                <@getTable table = cell alertLevelClassMap = alertLevelClassMap/>
                            </td>
                        <#else>
                            <#if cell?contains("&=&") && alertLevelClassMap??>
                                <td class="${alertLevelClassMap[cell?split("&=&")[1]]}">${cell?split("&=&")[0]}</td>
                            <#else>
                                <td style="word-break: break-all;word-wrap: break-word;">
                                    <!--有些命令行或环境变量太长，导致超出纸张大小，故强制换行-->
                                    <#assign l = cell?length/>
                                    <#assign step = 30/>
                                    <#if l gt step>
                                        <#assign n = (l / step)?int>
                                        <#list 0..n as i>
                                            <#assign start = i*step/>
                                            <#assign end = (i+1)*step-1/>
                                            <#if i lt n>
                                                ${cell[start..end]}<br/>
                                            <#else>
                                                ${cell[start..(l-1)]}
                                            </#if>
                                        </#list>
                                    <#else>
                                        ${cell}
                                    </#if>
                                </td>
                            </#if>
                        </#if>
                    </#list>
                </tr>
            </#list>
        </#if>
        </tbody>
    </table>
</#macro>
