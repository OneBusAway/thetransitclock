<t:reportsParamsForm action="predAccuracyRangeChart.jsp">
  <jsp:attribute name="title"><fmt:message key="div.SpecifyParameters"/></jsp:attribute>
  <jsp:attribute name="heading"><fmt:message key="div.SpecifyParameters"/></jsp:attribute>
  <jsp:body>
    <jsp:include page="params/routeAllOrSingle.jsp"/>
    <jsp:include page="params/fromDateNumDaysTime.jsp"/>
    <jsp:include page="params/predictionSource.jsp"/>
    <jsp:include page="params/predictionType.jsp"/>
    <jsp:include page="params/allowableEarlyLate.jsp"/>
    <jsp:include page="params/submitReport.jsp"/>
  </jsp:body>
</t:reportsParamsForm>
