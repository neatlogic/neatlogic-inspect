<!DOCTYPE html>
<html xmlns:v="urn:schemas-microsoft-com:vml" xmlns:o="urn:schemas-microsoft-com:office:office"
      xmlns:w="urn:schemas-microsoft-com:office:word" xmlns="http://www.w3.org/TR/REC-html40">
<head lang="en">
    <!--[if gte mso 9]><xml><w:WordDocument><w:View>Print</w:View><w:TrackMoves>false</w:TrackMoves><w:TrackFormatting/><w:ValidateAgainstSchemas/><w:SaveIfXMLInvalid>false</w:SaveIfXMLInvalid><w:IgnoreMixedContent>false</w:IgnoreMixedContent><w:AlwaysShowPlaceholderText>false</w:AlwaysShowPlaceholderText><w:DoNotPromoteQF/><w:LidThemeOther>EN-US</w:LidThemeOther><w:LidThemeAsian>ZH-CN</w:LidThemeAsian><w:LidThemeComplexScript>X-NONE</w:LidThemeComplexScript><w:Compatibility><w:BreakWrappedTables/><w:SnapToGridInCell/><w:WrapTextWithPunct/><w:UseAsianBreakRules/><w:DontGrowAutofit/><w:SplitPgBreakAndParaMark/><w:DontVertAlignCellWithSp/><w:DontBreakConstrainedForcedTables/><w:DontVertAlignInTxbx/><w:Word11KerningPairs/><w:CachedColBalance/><w:UseFELayout/></w:Compatibility><w:BrowserLevel>MicrosoftInternetExplorer4</w:BrowserLevel><m:mathPr><m:mathFont m:val="Cambria Math"/><m:brkBin m:val="before"/><m:brkBinSub m:val="--"/><m:smallFrac m:val="off"/><m:dispDef/><m:lMargin m:val="0"/> <m:rMargin m:val="0"/><m:defJc m:val="centerGroup"/><m:wrapIndent m:val="1440"/><m:intLim m:val="subSup"/><m:naryLim m:val="undOvr"/></m:mathPr></w:WordDocument></xml><![endif]-->
    <meta charset="UTF-8"/>
    <style>
        body {
            font-family: "SimSun";
        }

        .innerTable th, .innerTable td {
            border: 1px solid #000;
            font-weight: normal;
        }

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

        .lineBox{
            width: 100%;
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
        <div class="lineBox">
            <span>${line.key}：</span>
            <span <#if alertLevelClassMap?? && line.alertLevel??> class="${alertLevelClassMap[line.alertLevel]}" </#if>>${line.value}</span>
        </div>
        <#assign i++/>
    </#list>
</#if>
<#assign tableList = DATA.tableList/>
<#if tableList?? && tableList?size gt 0>
    <div>
        <#list tableList as value>
            <div>${value.key}：</div>
            <div>
                <@getTable table = value alertLevelClassMap = alertLevelClassMap/>
            </div>
        </#list>
    </div>
</#if>
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
