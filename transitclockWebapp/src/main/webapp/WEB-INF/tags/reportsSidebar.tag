<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t"   tagdir="/WEB-INF/tags" %>
<t:secondarySidebar title="Reports">
  <div>
    <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider"><fmt:message key="div.pa"/></h3>
    <ul class="space-y-0.5">
      <t:secondarySidebarLink path="/reports/predAccuracyRangeParams.jsp"     title="Shows percentage of predictions that were accurate to within the specified limits."><fmt:message key="div.prediction"/></t:secondarySidebarLink>
      <t:secondarySidebarLink path="/reports/predAccuracyIntervalsParams.jsp" title="Shows average prediction accuracy for each prediction length, with upper and lower bounds."><fmt:message key="div.PredictionAccuracyIntervalChart"/></t:secondarySidebarLink>
      <t:secondarySidebarLink path="/reports/predAccuracyScatterParams.jsp"   title="Shows each individual datapoint for prediction accuracy. Useful for finding specific issues with predictions."><fmt:message key="div.predictionscatter"/></t:secondarySidebarLink>
      <t:secondarySidebarLink path="/reports/predAccuracyCsvParams.jsp"       title="Download prediction accuracy data in CSV format."><fmt:message key="div.csv"/></t:secondarySidebarLink>
      <t:secondarySidebarLink path="/reports/routePerformanceTable.jsp"       title="Shows route performance: number of on-time predictions over total predictions for a given route."><fmt:message key="div.RoutePerformanceTable"/></t:secondarySidebarLink>
    </ul>
  </div>
  <div>
    <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider"><fmt:message key="div.ar"/></h3>
    <ul class="space-y-0.5">
      <t:secondarySidebarLink path="/reports/avlMap.jsp"        title="Display historic AVL data for a vehicle on a map."><fmt:message key="div.AVLDataInMap"/></t:secondarySidebarLink>
      <t:secondarySidebarLink path="/reports/avlMapParams.jsp"  title="Display historic AVL data for a vehicle on a map (with parameters)."><fmt:message key="div.AVLDataInMapParametersPage"/></t:secondarySidebarLink>
      <t:secondarySidebarLink path="/reports/lastAvlReport.jsp" title="Show the last time each vehicle reported its GPS position over the last 24 hours."><fmt:message key="div.lastgpsreports"/></t:secondarySidebarLink>
    </ul>
  </div>
  <div>
    <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider"><fmt:message key="div.EventReports"/></h3>
    <ul class="space-y-0.5">
      <t:secondarySidebarLink path="/reports/vehicleEventParams.jsp" title="View all events for a vehicle."><fmt:message key="div.EventForVehicle"/></t:secondarySidebarLink>
    </ul>
  </div>
  <div>
    <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider"><fmt:message key="div.sar"/></h3>
    <ul class="space-y-0.5">
      <t:secondarySidebarLink path="/reports/schAdhByRouteParams.jsp" title="Schedule adherence by route, in a bar chart. Can compare multiple routes."><fmt:message key="div.scheduleroutr"/></t:secondarySidebarLink>
      <t:secondarySidebarLink path="/reports/schAdhByStopParams.jsp"  title="Schedule adherence for each stop on a route, in a bar chart."><fmt:message key="div.schedulebystop"/></t:secondarySidebarLink>
      <t:secondarySidebarLink path="/reports/schAdhByTimeParams.jsp"  title="Schedule adherence for a route grouped by how early or late."><fmt:message key="div.earlylate"/></t:secondarySidebarLink>
    </ul>
  </div>
  <div>
    <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider"><fmt:message key="div.mr"/></h3>
    <ul class="space-y-0.5">
      <t:secondarySidebarLink path="/reports/scheduleHorizStopsParams.jsp" title="Display the schedule for a specified route in a table."><fmt:message key="div.schedulefor"/></t:secondarySidebarLink>
      <t:secondarySidebarLink path="/reports/scheduleVertStopsParams.jsp"  title="Schedule for a route with stops listed vertically. Useful when there are few trips per day."><fmt:message key="div.sfrvss"/></t:secondarySidebarLink>
    </ul>
  </div>
  <div>
    <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider"><fmt:message key="div.sr"/></h3>
    <ul class="space-y-0.5">
      <t:secondarySidebarLink path="/status/activeBlocks.jsp" title="How many block assignments are currently active and which have assigned vehicles."><fmt:message key="div.acbiveblock"/></t:secondarySidebarLink>
      <t:secondarySidebarLink path="/status/serverStatus.jsp" title="How well the system is running, including the AVL feed."><fmt:message key="div.ss"/></t:secondarySidebarLink>
    </ul>
  </div>
</t:secondarySidebar>
