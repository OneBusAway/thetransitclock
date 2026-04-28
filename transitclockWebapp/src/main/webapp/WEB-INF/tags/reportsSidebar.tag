<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx"  value="${pageContext.request.contextPath}"/>
<c:set var="qs"   value="?a=${param.a}"/>
<c:set var="path" value="${pageContext.request.servletPath}"/>
<c:set var="active"   value="bg-indigo-50 text-indigo-700 font-medium"/>
<c:set var="inactive" value="text-gray-700 hover:bg-gray-100 hover:text-gray-900"/>
<aside class="hidden md:flex md:flex-col w-64 lg:w-72 shrink-0 border-r border-gray-200 bg-gray-50">
  <div class="px-5 py-4 border-b border-gray-200">
    <h2 class="text-base font-semibold text-gray-900">Reports</h2>
  </div>
  <nav class="flex-1 overflow-y-auto px-3 py-4 space-y-6">
    <div>
      <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider"><fmt:message key="div.pa"/></h3>
      <ul class="space-y-0.5">
        <li><a class="block px-2 py-1.5 text-sm rounded ${path == '/reports/predAccuracyRangeParams.jsp'     ? active : inactive}" href="${ctx}/reports/predAccuracyRangeParams.jsp${qs}"     title="Shows percentage of predictions that were accurate to within the specified limits."><fmt:message key="div.prediction"/></a></li>
        <li><a class="block px-2 py-1.5 text-sm rounded ${path == '/reports/predAccuracyIntervalsParams.jsp' ? active : inactive}" href="${ctx}/reports/predAccuracyIntervalsParams.jsp${qs}" title="Shows average prediction accuracy for each prediction length, with upper and lower bounds."><fmt:message key="div.PredictionAccuracyIntervalChart"/></a></li>
        <li><a class="block px-2 py-1.5 text-sm rounded ${path == '/reports/predAccuracyScatterParams.jsp'   ? active : inactive}" href="${ctx}/reports/predAccuracyScatterParams.jsp${qs}"   title="Shows each individual datapoint for prediction accuracy. Useful for finding specific issues with predictions."><fmt:message key="div.predictionscatter"/></a></li>
        <li><a class="block px-2 py-1.5 text-sm rounded ${path == '/reports/predAccuracyCsvParams.jsp'       ? active : inactive}" href="${ctx}/reports/predAccuracyCsvParams.jsp${qs}"       title="Download prediction accuracy data in CSV format."><fmt:message key="div.csv"/></a></li>
        <li><a class="block px-2 py-1.5 text-sm rounded ${path == '/reports/routePerformanceTable.jsp'      ? active : inactive}" href="${ctx}/reports/routePerformanceTable.jsp${qs}"      title="Shows route performance: number of on-time predictions over total predictions for a given route."><fmt:message key="div.RoutePerformanceTable"/></a></li>
      </ul>
    </div>
    <div>
      <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider"><fmt:message key="div.ar"/></h3>
      <ul class="space-y-0.5">
        <li><a class="block px-2 py-1.5 text-sm rounded ${path == '/reports/avlMap.jsp'        ? active : inactive}" href="${ctx}/reports/avlMap.jsp${qs}"        title="Display historic AVL data for a vehicle on a map."><fmt:message key="div.AVLDataInMap"/></a></li>
        <li><a class="block px-2 py-1.5 text-sm rounded ${path == '/reports/avlMapParams.jsp'  ? active : inactive}" href="${ctx}/reports/avlMapParams.jsp${qs}"  title="Display historic AVL data for a vehicle on a map (with parameters)."><fmt:message key="div.AVLDataInMapParametersPage"/></a></li>
        <li><a class="block px-2 py-1.5 text-sm rounded ${path == '/reports/lastAvlReport.jsp' ? active : inactive}" href="${ctx}/reports/lastAvlReport.jsp${qs}" title="Show the last time each vehicle reported its GPS position over the last 24 hours."><fmt:message key="div.lastgpsreports"/></a></li>
      </ul>
    </div>
    <div>
      <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider"><fmt:message key="div.EventReports"/></h3>
      <ul class="space-y-0.5">
        <li><a class="block px-2 py-1.5 text-sm rounded ${path == '/reports/vehicleEventParams.jsp' ? active : inactive}" href="${ctx}/reports/vehicleEventParams.jsp${qs}" title="View all events for a vehicle."><fmt:message key="div.EventForVehicle"/></a></li>
      </ul>
    </div>
    <div>
      <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider"><fmt:message key="div.sar"/></h3>
      <ul class="space-y-0.5">
        <li><a class="block px-2 py-1.5 text-sm rounded ${path == '/reports/schAdhByRouteParams.jsp' ? active : inactive}" href="${ctx}/reports/schAdhByRouteParams.jsp${qs}" title="Schedule adherence by route, in a bar chart. Can compare multiple routes."><fmt:message key="div.scheduleroutr"/></a></li>
        <li><a class="block px-2 py-1.5 text-sm rounded ${path == '/reports/schAdhByStopParams.jsp'  ? active : inactive}" href="${ctx}/reports/schAdhByStopParams.jsp${qs}"  title="Schedule adherence for each stop on a route, in a bar chart."><fmt:message key="div.schedulebystop"/></a></li>
        <li><a class="block px-2 py-1.5 text-sm rounded ${path == '/reports/schAdhByTimeParams.jsp'  ? active : inactive}" href="${ctx}/reports/schAdhByTimeParams.jsp${qs}"  title="Schedule adherence for a route grouped by how early or late."><fmt:message key="div.earlylate"/></a></li>
      </ul>
    </div>
    <div>
      <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider"><fmt:message key="div.mr"/></h3>
      <ul class="space-y-0.5">
        <li><a class="block px-2 py-1.5 text-sm rounded ${path == '/reports/scheduleHorizStopsParams.jsp' ? active : inactive}" href="${ctx}/reports/scheduleHorizStopsParams.jsp${qs}" title="Display the schedule for a specified route in a table."><fmt:message key="div.schedulefor"/></a></li>
        <li><a class="block px-2 py-1.5 text-sm rounded ${path == '/reports/scheduleVertStopsParams.jsp'  ? active : inactive}" href="${ctx}/reports/scheduleVertStopsParams.jsp${qs}"  title="Schedule for a route with stops listed vertically. Useful when there are few trips per day."><fmt:message key="div.sfrvss"/></a></li>
      </ul>
    </div>
    <div>
      <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider"><fmt:message key="div.sr"/></h3>
      <ul class="space-y-0.5">
        <li><a class="block px-2 py-1.5 text-sm rounded ${path == '/status/activeBlocks.jsp' ? active : inactive}" href="${ctx}/status/activeBlocks.jsp${qs}" title="How many block assignments are currently active and which have assigned vehicles."><fmt:message key="div.acbiveblock"/></a></li>
        <li><a class="block px-2 py-1.5 text-sm rounded ${path == '/status/serverStatus.jsp' ? active : inactive}" href="${ctx}/status/serverStatus.jsp${qs}" title="How well the system is running, including the AVL feed."><fmt:message key="div.ss"/></a></li>
      </ul>
    </div>
  </nav>
</aside>
