<t:reportsParamsForm action="predAccuracyScatterChart.jsp">
  <jsp:attribute name="title"><fmt:message key="div.SpecifyParameters"/></jsp:attribute>
  <jsp:attribute name="heading"><fmt:message key="div.sppas"/></jsp:attribute>
  <jsp:body>
    <jsp:include page="params/routeAllOrSingle.jsp"/>
    <jsp:include page="params/fromDateNumDaysTime.jsp"/>

    <jsp:include page="params/boolean.jsp">
      <jsp:param name="label" value="Provide tooltip info"/>
      <jsp:param name="name" value="tooltips"/>
      <jsp:param name="default" value="true"/>
      <jsp:param name="tooltip" value="If set to True then provides detailed information on data through tooltip. Can be useful but if processing large amounts of data can slow down the query."/>
    </jsp:include>

    <jsp:include page="params/predictionSource.jsp"/>
    <jsp:include page="params/predictionType.jsp"/>
    <jsp:include page="params/submitReport.jsp"/>
  </jsp:body>
</t:reportsParamsForm>
