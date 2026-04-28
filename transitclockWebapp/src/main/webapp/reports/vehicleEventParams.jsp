<t:layout>
  <jsp:attribute name="title">Specify Parameters</jsp:attribute>
  <jsp:attribute name="head">
    <!-- Load in Select2 files so can create fancy route selector -->
    <link href="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0/css/select2.min.css" rel="stylesheet" />
    <script src="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0/js/select2.min.js"></script>

    <link href="params/reportParams.css" rel="stylesheet"/>
  </jsp:attribute>
  <jsp:body>
<div id="title">
   Select Parameters for Displaying Event Log by Vehicle
</div>

<div id="mainDiv">
<form action="vehicleEventReport.jsp" method="POST">
   <%-- For passing agency param to the report --%>
   <input type="hidden" name="a" value="${param.a}">

   <jsp:include page="params/vehicleSingle.jsp" />
   <jsp:include page="params/fromToDateTime.jsp"/>


   <jsp:include page="params/submitReport.jsp" />
  </form>
</div>
  </jsp:body>
</t:layout>
