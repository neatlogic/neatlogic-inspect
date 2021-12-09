<html xmlns:v="urn:schemas-microsoft-com:vml" xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">
<style>

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
</table>
</html>