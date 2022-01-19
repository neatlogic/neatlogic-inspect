<#--<html xmlns:v="urn:schemas-microsoft-com:vml" xmlns:o="urn:schemas-microsoft-com:office:office"-->
<#--      xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">-->
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

    .title {
        font-size: 20px;
        text-align: center;
        font-weight: bold;
    }

    .userAndTime {
        text-align: center;
    }

    .tdone{
        flex:1;
    }
    .title-text{
        display: inline-block;
        width: 25%;
        /*padding-right: 10px;*/
        text-align: right;
    }
    .block-text{
        width:10%!important;
    }
    .tdone .text{
        display: inline-block;
    }
    .tdBox{
        display: flex;
    }
    .boxtab{
        display: inline-block;
        width: 89%;
    }
</style>
<#if DATA.reportName??>
    <p class="title">${DATA.reportName}</p>
</#if>
<#if DATA.execUser?? || DATA.reportTime>
    <p class="userAndTime"><span>${DATA.execUser}</span>&nbsp;&nbsp;&nbsp;&nbsp;<span>${DATA.reportTime}</span></p>
</#if>
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
<#assign lineList = DATA.lineList/>
<#if lineList?? && lineList?size gt 0>
<#assign i = 1/>
<#list lineList as line>
<#if i % 2 != 0>
<div class="tdBox">
    </#if>
    <div class="tdone">
        <div class="title-text">${line.key}</div>
        <#if alertLevelClassMap?? && line.alertLevel??>
        <div class="${alertLevelClassMap[line.alertLevel]} text">
            <#else>
            <div class="text">
                </#if>
                ${line.value}</div>
        </div>
        <#if i % 2 == 0 || i == lineList?size>
    </div>
    </#if>
    <#assign i++/>
    </#list>
    </#if>
    <#assign tableList = DATA.tableList/>
    <#if tableList?? && tableList?size gt 0>
        <div>
            <#list tableList as value>
                <div class="title-text block-text">${value.key}</div>
                <div class="boxtab">
                    <@getTable table = value alertLevelClassMap = alertLevelClassMap/>
                </div>
            </#list>
        </div>
    </#if>

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