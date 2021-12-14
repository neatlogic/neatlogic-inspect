<html xmlns:v="urn:schemas-microsoft-com:vml" xmlns:o="urn:schemas-microsoft-com:office:office"
      xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">
<style>
    .innerTable {
        width: 100%;
        text-align: center;
        border-collapse: collapse;
    }

    .innerTable th, .innerTable td {
        border: 1px solid #000;
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
</style>
<#assign alertLevelClassMap = DATA.alertLevelClassMap/>
<#--告警列表-->
<#if DATA.alert?? && alertLevelClassMap??>
    <#assign headList = DATA.alert.headList/>
    <#assign rowList = DATA.alert.rowList/>
    <#if rowList?? && rowList?size gt 0>
        <table class="innerTable">
            <thead>
            <tr>
                <#list headList as head>
                    <th style="min-width: 30px">${head}</th>
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
    </#if>
</#if>
<#--报告正文-->
<table width="100%">
    <#assign lineList = DATA.lineList/>
    <#if lineList?? && lineList?size gt 0>
        <#assign i = 1/>
        <#list lineList as line>
            <#if i % 2 != 0>
                <tr>
            </#if>
            <td>${line.key}</td>
            <#if alertLevelClassMap?? && line.alertLevel??>
                <td class="${alertLevelClassMap[line.alertLevel]}">
            <#else>
                <td>
            </#if>
            ${line.value}</td>
            <#if i % 2 == 0 || i == lineList?size>
                </tr>
            </#if>
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
</table>
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
                                <td>${cell}</td>
                            </#if>
                        </#if>
                    </#list>
                </tr>
            </#list>
        </#if>
        </tbody>
    </table>
</#macro>