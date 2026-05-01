<%
String agencyId = request.getParameter("a");
if (agencyId == null || agencyId.isEmpty()) {
    response.getWriter().write("You must specify agency in query string (e.g. ?a=mbta)");
    return;
}
%>
<t:splitLayout>
  <jsp:attribute name="title"><fmt:message key="div.apicalls" /></jsp:attribute>
  <jsp:attribute name="sidebar"><t:apiCallsSidebar/></jsp:attribute>
  <jsp:body>
    <t:emptyState title="Choose an API call"
                  message="Pick an API call from the list on the left to view its parameters."/>
  </jsp:body>
</t:splitLayout>
