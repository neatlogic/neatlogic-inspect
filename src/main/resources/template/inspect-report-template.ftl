<html xmlns:v="urn:schemas-microsoft-com:vml" xmlns:o="urn:schemas-microsoft-com:office:office"
      xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">
<style>
    .innerTable {
        width: 100%;
        text-align: center;
        border-collapse: collapse;
    }

    .innerTable th {
        border: 1px solid;
    }

    .innerTable td {
        border: 1px solid;
    }
</style>
<table width="100%">
    <#assign lineList = DATA.lineList/>
    <#if lineList?? && lineList?size gt 0>
        <#assign i = 1/>
        <#list lineList as line>
            <#if i % 2 != 0>
                <tr>
            </#if>
            <td>${line.key}</td>
            <td>${line.value}</td>
            <#if i % 2 == 0 || i == lineList?size>
                </tr>
            </#if>
            <#assign i++/>
        </#list>
    </#if>
    <#assign tableList = DATA.tableList/>
    <#if tableList?? && tableList?size gt 0>
        <#list tableList as value>
            <@getTable table = value/>
        </#list>
    </#if>
</table>
</html>

<#macro getTable table>
    <tr>
        <td>${table.key}</td>
        <td>
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
                                <td>
                                    <#assign cell = value[head]/>
                                    <#if cell?is_hash>
                                        <@getTable table = cell/>
                                    </#if>
                                    ${cell}
                                </td>
                            </#list>
                        </tr>
                    </#list>
                </#if>
                </tbody>
            </table>
        </td>
    </tr>
</#macro>