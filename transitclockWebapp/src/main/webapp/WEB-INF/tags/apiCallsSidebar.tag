<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t"   tagdir="/WEB-INF/tags" %>
<t:secondarySidebar title="API Calls">
  <div>
    <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider">Routes</h3>
    <ul class="space-y-0.5">
      <t:secondarySidebarLink path="/reports/apiCalls/routeApiParams.jsp"        title="Summary data for all routes, listed in order. Useful for creating a UI selector for routes."><fmt:message key="div.rou"/></t:secondarySidebarLink>
      <t:secondarySidebarLink path="/reports/apiCalls/routeDetailsApiParams.jsp" title="Detailed data for selected routes. Includes stop and path information needed to show route on map."><fmt:message key="div.roude"/></t:secondarySidebarLink>
    </ul>
  </div>
  <div>
    <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider">Vehicles</h3>
    <ul class="space-y-0.5">
      <t:secondarySidebarLink path="/reports/apiCalls/vehiclesApiParams.jsp"        title="Data for vehicles, including GPS info, for a route. Useful for showing location of vehicles on map."><fmt:message key="div.veh"/></t:secondarySidebarLink>
      <t:secondarySidebarLink path="/reports/apiCalls/vehiclesDetailsApiParams.jsp" title="Detailed data for vehicles, including GPS info, for a route. Contains additional data such as schedule adherence and assignment information."><fmt:message key="div.vd"/></t:secondarySidebarLink>
      <t:secondarySidebarLink path="/reports/apiCalls/vehicleConfigsApiParams.jsp"  title="Configuration data for vehicles. A way of getting list of vehicles configured for agency."><fmt:message key="div.vc"/></t:secondarySidebarLink>
    </ul>
  </div>
  <div>
    <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider">Predictions</h3>
    <ul class="space-y-0.5">
      <t:secondarySidebarLink path="/reports/apiCalls/predsByRouteStopApiParams.jsp" title="Predictions for specified route and stop."><fmt:message key="div.pbrs"/></t:secondarySidebarLink>
      <t:secondarySidebarLink path="/reports/apiCalls/predsByLocApiParams.jsp"      title="Predictions for stops near specified latitude, longitude for the agency."><fmt:message key="div.pbl"/></t:secondarySidebarLink>
    </ul>
  </div>
  <div>
    <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider">Trips &amp; Blocks</h3>
    <ul class="space-y-0.5">
      <t:secondarySidebarLink path="/reports/apiCalls/tripApiParams.jsp"                title="Data for a single trip. Includes trip pattern and schedule info."><fmt:message key="div.dtrip"/></t:secondarySidebarLink>
      <t:secondarySidebarLink path="/reports/apiCalls/tripWithTravelTimesApiParams.jsp" title="Data for a single trip. Includes trip pattern and schedule info as well as historic travel times used for generating predictions."><fmt:message key="div.twtt"/></t:secondarySidebarLink>
      <t:secondarySidebarLink path="/reports/apiCalls/blocksTerseApiParams.jsp"         title="Data for a block assignment. Shows each trip that makes up the block in a terse format, without trip pattern or schedule info."><fmt:message key="div.dblock"/></t:secondarySidebarLink>
      <t:secondarySidebarLink path="/reports/apiCalls/blocksApiParams.jsp"              title="Data for a block assignment. Shows each trip that makes up the block in a verbose format, including trip pattern and schedule info."><fmt:message key="div.bd"/></t:secondarySidebarLink>
    </ul>
  </div>
  <div>
    <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider">Service IDs</h3>
    <ul class="space-y-0.5">
      <t:secondarySidebarLink path="/reports/apiCalls/serviceIdsApiParams.jsp"        title="Data for all service IDs configured for agency."><fmt:message key="div.si"/></t:secondarySidebarLink>
      <t:secondarySidebarLink path="/reports/apiCalls/serviceIdsCurrentApiParams.jsp" title="Data for service IDs that are currently active for agency."><fmt:message key="div.sic"/></t:secondarySidebarLink>
    </ul>
  </div>
  <div>
    <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider">Calendars</h3>
    <ul class="space-y-0.5">
      <t:secondarySidebarLink path="/reports/apiCalls/calendarsApiParams.jsp"        title="Data for all calendars configured for agency.">Calendars</t:secondarySidebarLink>
      <t:secondarySidebarLink path="/reports/apiCalls/calendarsCurrentApiParams.jsp" title="Data for calendars that are currently active for agency.">Calendars Current</t:secondarySidebarLink>
    </ul>
  </div>
  <div>
    <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider">GTFS-realtime</h3>
    <ul class="space-y-0.5">
      <t:secondarySidebarLink path="/reports/apiCalls/gtfsRealtimeTripUpdatesApiParams.jsp"      title="GTFS-realtime Trip Updates includes prediction data for entire agency"><fmt:message key="div.grtu"/></t:secondarySidebarLink>
      <t:secondarySidebarLink path="/reports/apiCalls/gtfsRealtimeVehiclePositionsApiParams.jsp" title="GTFS-realtime Vehicle Positions for entire agency"><fmt:message key="div.grvp"/></t:secondarySidebarLink>
    </ul>
  </div>
  <div>
    <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider">SIRI</h3>
    <ul class="space-y-0.5">
      <t:secondarySidebarLink path="/reports/apiCalls/siriVehicleMonitoringApiParams.jsp" title="SIRI Vehicle Monitoring for specified route or entire agency"><fmt:message key="div.svm"/></t:secondarySidebarLink>
      <t:secondarySidebarLink path="/reports/apiCalls/siriStopMonitoringApiParams.jsp"    title="SIRI Stop Monitoring for specified route and stop"><fmt:message key="div.ssm"/></t:secondarySidebarLink>
    </ul>
  </div>
  <div>
    <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider">Schedules</h3>
    <ul class="space-y-0.5">
      <t:secondarySidebarLink path="/reports/apiCalls/horizStopsScheduleApiParams.jsp" title="Schedule for route. For displaying schedule with stops listed in horizontal direction"><fmt:message key="div.sfrsh"/></t:secondarySidebarLink>
      <t:secondarySidebarLink path="/reports/apiCalls/vertStopsScheduleApiParams.jsp"  title="Schedule for route. For displaying schedule with stops listed in vertical direction"><fmt:message key="div.sfrsv"/></t:secondarySidebarLink>
    </ul>
  </div>
  <div>
    <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider">Commands</h3>
    <ul class="space-y-0.5">
      <t:secondarySidebarLink path="/reports/apiCalls/resetVehicleApiParams.jsp" title="Reset specific vehicle"><fmt:message key="div.rv"/></t:secondarySidebarLink>
    </ul>
  </div>
  <div>
    <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider"><fmt:message key="div.nas"/></h3>
    <ul class="space-y-0.5">
      <t:secondarySidebarLink path="/reports/apiCalls/agenciesApiParams.jsp"              title="List of all agencies available through the API" omitAgency="true"><fmt:message key="div.agencies"/></t:secondarySidebarLink>
      <t:secondarySidebarLink path="/reports/apiCalls/predsByLocForAllAgenciesApiParams.jsp" title="Predictions for stops near specified latitude, longitude. Will return predictions for all agencies that have nearby stops."><fmt:message key="div.pbl"/></t:secondarySidebarLink>
    </ul>
  </div>
</t:secondarySidebar>
