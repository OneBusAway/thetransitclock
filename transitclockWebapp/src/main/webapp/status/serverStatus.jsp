<%@page import="org.transitclock.db.webstructs.WebAgency"%>
<%@page import="java.rmi.RemoteException"%>
<%@page import="org.transitclock.ipc.interfaces.ServerStatusInterface"%>
<%@page import="org.transitclock.ipc.clients.ServerStatusInterfaceFactory"%>
<%@page import="org.transitclock.monitoring.*"%>
<%@page import="java.util.List"%>
<%
String agencyId = request.getParameter("a");
if (agencyId == null || agencyId.isEmpty()) {
    response.getWriter().write("You must specify agency in query string (e.g. ?a=mbta)");
    return;
}
pageContext.setAttribute("agencyName", WebAgency.getCachedWebAgency(agencyId).getAgencyName());

try {
    ServerStatusInterface serverStatusInterface =
        org.transitclock.ipc.clients.ServerStatusInterfaceFactory.get(agencyId);
    List<MonitorResult> monitorResults = serverStatusInterface.get().getMonitorResults();
    pageContext.setAttribute("monitorResults", monitorResults);
} catch (RemoteException e) {
    pageContext.setAttribute("rmiError", e.getMessage());
}
%>
<t:layout>
  <jsp:attribute name="title"><fmt:message key="div.serwerstatus" /></jsp:attribute>
  <jsp:attribute name="head">
<style>
  h3, .content {
    margin-left: 20%;
    margin-right: 20%;
  }
</style>
  </jsp:attribute>
  <jsp:body>
<div id="title"><fmt:message key="div.ssf" /> ${agencyName}</div>

<c:if test="${not empty rmiError}">
  ${rmiError}
</c:if>
<c:forEach var="monitorResult" items="${monitorResults}">
  <c:if test="${not empty monitorResult.message}">
    <h3>${monitorResult.type}</h3>
    <div class="content">${monitorResult.message}</div>
  </c:if>
</c:forEach>
  </jsp:body>
</t:layout>
