<t:reportsParamsForm action="scheduleHorizStopsReport.jsp">
  <jsp:attribute name="title"><fmt:message key="div.SpecifyParameters"/></jsp:attribute>
  <jsp:attribute name="heading"><fmt:message key="div.spfsr"/></jsp:attribute>
  <jsp:body>
    <jsp:include page="params/routeSingle.jsp"/>
    <jsp:include page="params/submitReport.jsp"/>
  </jsp:body>
</t:reportsParamsForm>
