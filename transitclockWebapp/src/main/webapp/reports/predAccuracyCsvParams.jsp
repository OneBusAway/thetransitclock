<t:reportsParamsForm action="predAccuracyCsv.jsp">
  <jsp:attribute name="title"><fmt:message key="div.SpecifyParameters"/></jsp:attribute>
  <jsp:attribute name="heading"><fmt:message key="div.csvd"/></jsp:attribute>
  <jsp:body>
    <jsp:include page="params/routeAllOrSingle.jsp"/>
    <jsp:include page="params/fromDateNumDaysTime.jsp"/>
    <jsp:include page="params/submitReport.jsp"/>
  </jsp:body>
</t:reportsParamsForm>
