<%@page import="org.transitclock.db.webstructs.WebAgency"%>
<%
String agencyId = request.getParameter("a");
if (agencyId == null || agencyId.isEmpty()) {
    response.getWriter().write("You must specify agency in query string (e.g. ?a=mbta)");
    return;
}
pageContext.setAttribute("agencyId", agencyId);
pageContext.setAttribute("agencyName", WebAgency.getCachedWebAgency(agencyId).getAgencyName());
%>
<t:layout>
  <jsp:attribute name="title"><fmt:message key="div.apicalls" /></jsp:attribute>
  <jsp:body>
<div id="mainDiv">
<div id="title"><fmt:message key="div.apicallsfor" />${agencyName}</div>
<div id="subtitle" style="margin-bottom: 20px;"><fmt:message key="div.notethisis" /></div>
<div id="subtitle"><fmt:message key="div.asac" /></div>
<ul class="choicesList">
  <li><a href="routeApiParams.jsp?a=${agencyId}"
    title="Summary data for all routes, listed in order. Useful for creating a UI selector for routes.">
      <fmt:message key="div.rou" /></a></li>
  <li><a href="routeDetailsApiParams.jsp?a=${agencyId}"
    title="Detailed data for selected routes. Includes stop and path information needed to show route on map.">
      <fmt:message key="div.roude" /></a></li>

  <li><a href="vehiclesApiParams.jsp?a=${agencyId}"
    title="Data for vehicles, including GPS info, for a route. Useful for showing location of vehicles on map.">
      <fmt:message key="div.veh" /></a></li>
  <li><a href="vehiclesDetailsApiParams.jsp?a=${agencyId}"
    title="Detailed data for vehicles, including GPS info, for a route. Contains additional data such as schedule adherence and assignment information.">
      <fmt:message key="div.vd" /></a></li>
  <li><a href="vehicleConfigsApiParams.jsp?a=${agencyId}"
    title="Configuration data for vehicles. A way of getting list of vehicles configured for agency.">
      <fmt:message key="div.vc" /></a></li>

  <li><a href="predsByRouteStopApiParams.jsp?a=${agencyId}"
    title="Predictions for specified route and stop.">
      <fmt:message key="div.pbrs" /></a></li>
  <li><a href="predsByLocApiParams.jsp?a=${agencyId}"
    title="Predictions for stops near specified latitude, longitude for the agency.">
      <fmt:message key="div.pbl" /></a></li>
  <li><a href="tripApiParams.jsp?a=${agencyId}"
    title="Data for a single trip. Includes trip pattern and schedule info.">
      <fmt:message key="div.dtrip" /></a></li>
  <li><a href="tripWithTravelTimesApiParams.jsp?a=${agencyId}"
    title="Data for a single trip. Includes trip pattern and schedule info as well as historic travel times used for generating predictions.">
      <fmt:message key="div.twtt" /></a></li>
  <li><a href="blocksTerseApiParams.jsp?a=${agencyId}"
    title="Data for a block assignment. Shows each trip that makes up the block in a terse format, without trip pattern or schedule info.">
      <fmt:message key="div.dblock" /></a></li>
  <li><a href="blocksApiParams.jsp?a=${agencyId}"
    title="Data for a block assignment. Shows each trip that makes up the block in a verbose format, including trip pattern and schedule info.">
      <fmt:message key="div.bd" /></a></li>

  <li><a href="serviceIdsApiParams.jsp?a=${agencyId}"
    title="Data for all service IDs configured for agency.">
      <fmt:message key="div.si" /></a></li>
  <li><a href="serviceIdsCurrentApiParams.jsp?a=${agencyId}"
    title="Data for service IDs that are currently active for agency.">
      <fmt:message key="div.sic" /></a></li>

  <li><a href="calendarsApiParams.jsp?a=${agencyId}"
    title="Data for all calendars configured for agency.">
      Calendars</a></li>
  <li><a href="calendarsCurrentApiParams.jsp?a=${agencyId}"
    title="Data for calendars that are currently active for agency.">
      Calendars Current</a></li>

  <li><a href="gtfsRealtimeTripUpdatesApiParams.jsp?a=${agencyId}"
    title="GTFS-realtime Trip Updates includes prediction data for entire agency">
      <fmt:message key="div.grtu" /></a></li>
  <li><a href="gtfsRealtimeVehiclePositionsApiParams.jsp?a=${agencyId}"
    title="GTFS-realtime Vehicle Positions for entire agency">
      <fmt:message key="div.grvp" /></a></li>

  <li><a href="siriVehicleMonitoringApiParams.jsp?a=${agencyId}"
    title="SIRI Vehicle Monitoring for specified route or entire agency">
      <fmt:message key="div.svm" /></a></li>
  <li><a href="siriStopMonitoringApiParams.jsp?a=${agencyId}"
    title="SIRI Stop Monitoring for specified route and stop">
      <fmt:message key="div.ssm" /></a></li>

  <li><a href="horizStopsScheduleApiParams.jsp?a=${agencyId}"
    title="Schedule for route. For displaying schedule with stops listed in horizontal direction">
      <fmt:message key="div.sfrsh" /></a></li>
  <li><a href="vertStopsScheduleApiParams.jsp?a=${agencyId}"
    title="Schedule for route. For displaying schedule with stops listed in vertical direction">
      Schedule for Route, stops vertical</a></li>
  <li><a href="resetVehicleApiParams.jsp?a=${agencyId}"
    title="Reset specific vehicle">
      <fmt:message key="div.sfrsv" /></a></li>
</ul>

<div id="subtitle">Not Agency Specific</div>
<ul class="choicesList">
  <li><a href="agenciesApiParams.jsp"
    title="List of all agencies available through the API">
      <fmt:message key="div.agencies" /></a></li>
  <li><a href="predsByLocForAllAgenciesApiParams.jsp?a=${agencyId}"
    title="Predictions for stops near specified latitude, longitude. Will return predictions for all agencies that have nearby stops.">
      <fmt:message key="div.pbl" /></a></li>

</ul>
</div>
  </jsp:body>
</t:layout>
