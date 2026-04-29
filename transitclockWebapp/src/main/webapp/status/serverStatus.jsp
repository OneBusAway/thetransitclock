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

<c:choose>
  <c:when test="${not empty rmiError}">
    <div class="rounded-lg border border-red-200 bg-red-50 p-4">
      <h2 class="text-sm font-semibold text-red-900"><fmt:message key="div.coreUnreachable" /></h2>
      <p class="mt-1 text-sm text-red-800 font-mono break-all"><c:out value="${rmiError}"/></p>
    </div>
  </c:when>
  <c:otherwise>
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <c:forEach var="monitorResult" items="${monitorResults}">
        <c:if test="${not empty monitorResult.message}">
          <article class="bg-white border border-gray-200 rounded-lg p-4">
            <h2 class="text-xs font-semibold uppercase tracking-wider text-gray-500">
              <c:out value="${monitorResult.type}"/>
            </h2>
            <c:choose>
              <c:when test="${not empty monitorResult.stats}">
                <dl class="mt-3 grid grid-cols-2 gap-x-4 gap-y-1.5 text-sm">
                  <c:forEach var="stat" items="${monitorResult.stats}">
                    <div class="flex items-baseline gap-1.5 col-span-2 sm:col-span-1">
                      <dt class="text-gray-500"><c:out value="${stat.key}"/>:</dt>
                      <dd class="font-mono text-gray-900"><c:out value="${stat.value}"/></dd>
                    </div>
                  </c:forEach>
                </dl>
              </c:when>
              <c:otherwise>
                <p class="mt-2 text-sm text-gray-900 leading-relaxed tabular-nums">
                  <c:out value="${monitorResult.message}"/>
                </p>
              </c:otherwise>
            </c:choose>
          </article>
        </c:if>
      </c:forEach>
    </div>
  </c:otherwise>
</c:choose>
  </jsp:body>
</t:layout>
