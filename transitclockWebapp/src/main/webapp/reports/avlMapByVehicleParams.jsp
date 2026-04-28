<t:reportsParamsForm action="avlMap.jsp">
  <jsp:attribute name="title"><fmt:message key="div.SpecifyParameters"/></jsp:attribute>
  <jsp:attribute name="heading"><fmt:message key="div.spdavlbv"/></jsp:attribute>
  <jsp:body>
    <jsp:include page="params/vehicle.jsp"/>
    <jsp:include page="params/fromDateNumDaysTime.jsp"/>
    <jsp:include page="params/routeOptional.jsp"/>
    <%-- Override the shared "Route" label to clarify intent next to the vehicle picker. --%>
    <script>
      $("#routesDiv label").text("Route to Display:");
    </script>

    <jsp:include page="params/submitReport.jsp"/>
  </jsp:body>
</t:reportsParamsForm>
