<t:reportsParamsForm action="vehicleEventReport.jsp">
  <jsp:attribute name="title"><fmt:message key="div.SpecifyParameters"/></jsp:attribute>
  <jsp:attribute name="heading">Select Parameters for Displaying Event Log by Vehicle</jsp:attribute>
  <jsp:body>
    <jsp:include page="params/vehicleSingle.jsp"/>
    <jsp:include page="params/fromToDateTime.jsp"/>
    <jsp:include page="params/submitReport.jsp"/>
  </jsp:body>
</t:reportsParamsForm>
