<%@ page import="org.transitclock.utils.web.WebUtils" %>
<%
String agencyId = request.getParameter("a");
String vehicleId = request.getParameter("v");
if (agencyId == null || agencyId.isEmpty()) {
    response.getWriter().write("You must specify agency in query string (e.g. ?a=mbta)");
    return;
}
if (vehicleId == null || vehicleId.isEmpty()) {
    response.getWriter().write("You must specify vehicle in query string (e.g. ?v=1234)");
    return;
}
pageContext.setAttribute("ajaxDataString", WebUtils.getAjaxDataString(request));
%>
<t:layout>
  <jsp:attribute name="title">Events for Vehicle</jsp:attribute>
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
		row.insertCell(0).innerHTML = vehicleInfo.time;
		row.insertCell(1).innerHTML = vehicleInfo.description;
	}
};

// Initiate AJAX call to get data to put into table
$( document ).ready(function() {
  $.ajax({
   	// The page being requested
  	url: "/web/reports/vehicleEventData.jsp",
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
<div id="title">Vehicle Events for vehicle </div>
<table id="dataTable">
  <tr><th>Time</th><th>Event Description</th></tr>
  </table>
  </jsp:body>
</t:layout>
