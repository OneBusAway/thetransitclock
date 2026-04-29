<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="org.transitclock.db.webstructs.WebAgency" %>
<%@ page import="org.transitclock.ipc.clients.ServerStatusInterfaceFactory" %>
<%@ page import="org.transitclock.monitoring.MonitorResult" %>
<%@ page import="org.transitclock.reports.DbDiskSpaceQuery" %>
<%@ page import="org.transitclock.reports.ScheduleAdherenceController" %>
<%@ page import="org.transitclock.utils.Time" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.List" %>
<%
String agencyId = request.getParameter("a");
if (agencyId == null || agencyId.isEmpty()) {
    response.getWriter().write("You must specify agency in query string (e.g. ?a=mbta)");
    return;
}

int scheduleEarlySec = ScheduleAdherenceController.getScheduleEarlySeconds();
int scheduleLateSec  = ScheduleAdherenceController.getScheduleLateSeconds();
pageContext.setAttribute("agencyId",   agencyId);
pageContext.setAttribute("agencyName", WebAgency.getCachedWebAgency(agencyId).getAgencyName());
pageContext.setAttribute("now",        Time.timeStrNoTimeZone(new Date()));
pageContext.setAttribute("earlyMsec",  scheduleEarlySec * -1000);
pageContext.setAttribute("lateMsec",   scheduleLateSec  * 1000);

List<MonitorResult> monitorResults = null;
String monitorError = null;
try {
    monitorResults = ServerStatusInterfaceFactory.get(agencyId).get().getMonitorResults();
} catch (Exception e) {
    application.log("dashboard could not reach Core for agency=" + agencyId, e);
    StringBuilder detail = new StringBuilder(String.valueOf(e));
    for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause())
        detail.append(" -> ").append(cause);
    monitorError = detail.toString();
}
pageContext.setAttribute("monitorResults", monitorResults);
pageContext.setAttribute("monitorError",   monitorError);

List<DbDiskSpaceQuery.TableSize> topTables = null;
String dbError = null;
long maxBytes = 0L;
try {
    topTables = DbDiskSpaceQuery.getTopTables(agencyId, 5);
    for (DbDiskSpaceQuery.TableSize t : topTables) maxBytes = Math.max(maxBytes, t.bytes());
} catch (java.sql.SQLException e) {
    application.log("dashboard could not load db disk space for agency=" + agencyId, e);
    StringBuilder detail = new StringBuilder(String.valueOf(e));
    for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause())
        detail.append(" -> ").append(cause);
    dbError = detail.toString();
}
pageContext.setAttribute("topTables", topTables);
pageContext.setAttribute("maxBytes",  maxBytes);
pageContext.setAttribute("dbError",   dbError);
%>
<t:layout>
  <jsp:attribute name="title"><fmt:message key="div.dashboard"/></jsp:attribute>
  <jsp:body>

<div class="mb-6">
  <h1 class="text-2xl font-bold tracking-tight text-gray-900 m-0"><fmt:message key="div.dashboard"/></h1>
  <p class="mt-1 text-sm text-gray-500 m-0">Live overview of fleet status and system health for <c:out value="${agencyName}"/>.</p>
</div>

<div class="space-y-8">

  <section data-controller="active-blocks"
           data-active-blocks-early-msec-value="${earlyMsec}"
           data-active-blocks-late-msec-value="${lateMsec}">
    <div class="flex items-end justify-between gap-4 flex-wrap mb-4">
      <div>
        <h2 class="text-lg font-bold tracking-tight text-gray-900 m-0"><fmt:message key="div.acbiveblock"/></h2>
        <p class="mt-1 text-sm text-gray-500 m-0">Real-time status of vehicle assignments across the fleet.</p>
      </div>
      <div class="flex items-center gap-3">
        <span class="text-xs text-gray-500 inline-flex items-center gap-1.5">
          <svg class="size-3 text-gray-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>
          <fmt:message key="div.AsOf"/>
          <span data-active-blocks-target="asOf" class="font-semibold text-gray-700 tabular-nums">—</span>
        </span>
        <a href="${pageContext.request.contextPath}/status/activeBlocks.jsp?a=${agencyId}"
           class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium text-gray-700 bg-white border border-gray-300 hover:bg-gray-50">
          View details &rarr;
        </a>
      </div>
    </div>
    <t:activeBlocksSummary/>
  </section>

  <section>
    <div class="flex items-end justify-between gap-4 flex-wrap mb-4">
      <div>
        <h2 class="text-lg font-bold tracking-tight text-gray-900 m-0"><fmt:message key="div.ssf"/> <c:out value="${agencyName}"/></h2>
      </div>
      <div class="flex items-center gap-3">
        <span class="text-xs text-gray-500 inline-flex items-center gap-1.5">
          <fmt:message key="div.AsOf"/>
          <span class="font-mono text-gray-700 tabular-nums">${now}</span>
        </span>
        <a href="${pageContext.request.contextPath}/status/serverStatus.jsp?a=${agencyId}"
           class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium text-gray-700 bg-white border border-gray-300 hover:bg-gray-50">
          View details &rarr;
        </a>
      </div>
    </div>
    <t:serverStatusGrid monitorResults="${monitorResults}" error="${monitorError}"/>
  </section>

  <section>
    <div class="flex items-end justify-between gap-4 flex-wrap mb-4">
      <div>
        <h2 class="text-lg font-bold tracking-tight text-gray-900 m-0"><fmt:message key="div.ddsflt"/></h2>
        <p class="mt-1 text-sm text-gray-500 m-0">The five tables consuming the most on-disk space.</p>
      </div>
      <a href="${pageContext.request.contextPath}/status/dbDiskSpace.jsp?a=${agencyId}"
         class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium text-gray-700 bg-white border border-gray-300 hover:bg-gray-50">
        View details &rarr;
      </a>
    </div>
    <c:choose>
      <c:when test="${not empty dbError}">
        <div class="rounded-lg border border-red-200 bg-red-50 p-4">
          <h3 class="text-sm font-semibold text-red-900">Database query failed</h3>
          <p class="mt-1 text-sm text-red-800 font-mono break-all"><c:out value="${dbError}"/></p>
        </div>
      </c:when>
      <c:when test="${empty topTables}">
        <div class="rounded-lg border border-gray-200 bg-white p-4 text-sm text-gray-500">No table size data available.</div>
      </c:when>
      <c:otherwise>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6 bg-white border border-gray-200 rounded-lg p-5 shadow-[0_1px_2px_rgba(0,0,0,0.03)]">
          <ul class="space-y-3 m-0 p-0 list-none">
            <c:forEach var="t" items="${topTables}">
              <li>
                <div class="flex items-baseline justify-between gap-3 mb-1 text-xs">
                  <span class="font-mono text-gray-900 truncate" title="${t.tableName}"><c:out value="${t.tableName}"/></span>
                  <span class="text-gray-500 font-mono tabular-nums shrink-0"><c:out value="${t.prettySize}"/></span>
                </div>
                <div class="h-2 rounded-full bg-gray-100 overflow-hidden">
                  <div class="h-full bg-brand-accent rounded-full" style="width: ${(t.bytes * 100.0) / maxBytes}%"></div>
                </div>
              </li>
            </c:forEach>
          </ul>
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b border-gray-200 text-left">
                  <th class="py-2 pr-3 font-semibold text-gray-700 text-xs uppercase tracking-wider">Table</th>
                  <th class="py-2 px-3 font-semibold text-gray-700 text-xs uppercase tracking-wider text-right">Size</th>
                  <th class="py-2 pl-3 font-semibold text-gray-700 text-xs uppercase tracking-wider text-right">Bytes</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach var="t" items="${topTables}">
                  <tr class="border-b border-gray-100 last:border-b-0">
                    <td class="py-2 pr-3 font-mono text-gray-900 break-all"><c:out value="${t.tableName}"/></td>
                    <td class="py-2 px-3 font-mono text-gray-700 text-right tabular-nums whitespace-nowrap"><c:out value="${t.prettySize}"/></td>
                    <td class="py-2 pl-3 font-mono text-gray-500 text-right tabular-nums whitespace-nowrap">
                      <fmt:formatNumber value="${t.bytes}" pattern="#,###"/>
                    </td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </div>
      </c:otherwise>
    </c:choose>
  </section>

</div>
  </jsp:body>
</t:layout>
