<%@page import="org.transitclock.db.webstructs.WebAgency"%>
<%@page import="org.transitclock.ipc.interfaces.ServerStatusInterface"%>
<%@page import="org.transitclock.ipc.clients.ServerStatusInterfaceFactory"%>
<%@page import="org.transitclock.monitoring.*"%>
<%@page import="org.transitclock.utils.Time"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.List"%>
<%
String agencyId = request.getParameter("a");
if (agencyId == null || agencyId.isEmpty()) {
    response.getWriter().write("You must specify agency in query string (e.g. ?a=mbta)");
    return;
}
pageContext.setAttribute("agencyId",   agencyId);
pageContext.setAttribute("agencyName", WebAgency.getCachedWebAgency(agencyId).getAgencyName());
pageContext.setAttribute("now",        Time.timeStrNoTimeZone(new Date()));

try {
    ServerStatusInterface serverStatusInterface =
        org.transitclock.ipc.clients.ServerStatusInterfaceFactory.get(agencyId);
    List<MonitorResult> monitorResults = serverStatusInterface.get().getMonitorResults();
    pageContext.setAttribute("monitorResults", monitorResults);
} catch (Exception e) {
    // RemoteException.getMessage() is often terse; walk the cause chain so
    // operators can diagnose without tailing Tomcat logs.
    application.log("serverStatus.jsp could not reach Core for agency=" + agencyId, e);
    StringBuilder detail = new StringBuilder(String.valueOf(e));
    for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
        detail.append(" -> ").append(cause);
    }
    pageContext.setAttribute("rmiError", detail.toString());
}
%>
<t:layout>
  <jsp:attribute name="title"><fmt:message key="div.serwerstatus" /></jsp:attribute>
  <jsp:body>

<%-- Sticky summary header. Bleeds full-width within main by undoing the
     layout's p-8 padding via -mx-8/-mt-8, then re-applies px-8 itself. --%>
<div class="sticky top-0 z-10 -mx-8 -mt-8 mb-6 px-8 py-4 bg-white/95 backdrop-blur border-b border-gray-200">
  <div class="flex flex-wrap items-center gap-x-6 gap-y-2">
    <h1 class="mr-auto text-lg font-semibold text-gray-900">
      <fmt:message key="div.ssf" /> ${agencyName}
    </h1>
    <dl class="flex items-baseline gap-1.5 text-sm">
      <dt class="text-gray-500"><fmt:message key="div.AsOf" /></dt>
      <dd class="font-mono text-gray-900">${now}</dd>
    </dl>
    <a href="?a=${agencyId}"
       class="px-3 py-1.5 text-xs font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50">
      <fmt:message key="div.refresh" />
    </a>
  </div>
</div>

<t:serverStatusGrid monitorResults="${monitorResults}" error="${rmiError}"/>
  </jsp:body>
</t:layout>
