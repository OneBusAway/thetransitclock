<%
String agencyId = request.getParameter("a");
if (agencyId == null || agencyId.isEmpty()) {
    response.getWriter().write("You must specify agency in query string (e.g. ?a=mbta)");
    return;
}
%>
<t:reportsLayout>
  <jsp:attribute name="title"><fmt:message key="div.historical"/></jsp:attribute>
  <jsp:body>
    <t:emptyState title="Choose a report"
                  message="Pick a report from the list on the left to view or generate it."/>
  </jsp:body>
</t:reportsLayout>
