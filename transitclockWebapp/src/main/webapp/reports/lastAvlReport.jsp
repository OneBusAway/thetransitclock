<%@ page import="org.transitclock.utils.web.WebUtils" %>
<%
String agencyId = request.getParameter("a");
if (agencyId == null || agencyId.isEmpty()) {
    response.getWriter().write("You must specify agency in query string (e.g. ?a=mbta)");
    return;
}
pageContext.setAttribute("ajaxDataString", WebUtils.getAjaxDataString(request));
%>
<t:layout>
  <jsp:attribute name="title"><fmt:message key="div.lastgpsreports" /></jsp:attribute>
  <jsp:attribute name="head">
<style>

</style>

<script>

/* Programatically create contents of table */
function dataReadCallback(jsonData) {
	var table = document.getElementById("dataTable");

	for (var i=0; i<jsonData.data.length; ++i) {
		var vehicleInfo = jsonData.data[i];

		// Insert row (after the header)
		var row = table.insertRow(i+1);
		row.insertCell(0).innerHTML = vehicleInfo.vehicleId;
		row.insertCell(1).innerHTML = vehicleInfo.maxTime;
		row.insertCell(2).innerHTML = vehicleInfo.lat;
		row.insertCell(3).innerHTML = vehicleInfo.lon;
	}
}

// Initiate AJAX call to get data to put into table
$( document ).ready(function() {
  $.ajax({
   	// The page being requested
  	url: "/web/reports/lastAvlJsonData.jsp",
   	// Pass in query string parameters to page being requested
   	data: {${ajaxDataString}},
  	// Needed so that parameters passed properly to page being requested
   	traditional: true,
    dataType:"json",
	success: dataReadCallback
  });
});
</script>
  </jsp:attribute>
  <jsp:body>
<div id="title"><fmt:message key="div.lgpsr" /></div>
<table id="dataTable">
  <tr><th><fmt:message key="div.Vehicle" /></th><th><fmt:message key="div.lgps" /></th></tr>
  </table>
  </jsp:body>
</t:layout>
