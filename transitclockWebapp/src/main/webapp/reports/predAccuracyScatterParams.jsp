<t:layout>
  <jsp:attribute name="title"><fmt:message key="div.SpecifyParameters" /></jsp:attribute>
  <jsp:attribute name="head">
    <!-- Load in Select2 files so can create fancy selectors -->
    <link href="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0/css/select2.min.css" rel="stylesheet" />
    <script src="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0/js/select2.min.js"></script>

    <link href="params/reportParams.css" rel="stylesheet"/>
  </jsp:attribute>
  <jsp:body>
   <div id="title">
   <fmt:message key="div.sppas" />
   </div>

<div id="mainDiv">
<form action="predAccuracyScatterChart.jsp" method="POST">
   <%-- For passing agency param to the report --%>
   <input type="hidden" name="a" value="${param.a}">

   <jsp:include page="params/routeAllOrSingle.jsp" />

   <jsp:include page="params/fromDateNumDaysTime.jsp" />

   <jsp:include page="params/boolean.jsp">
    <jsp:param name="label" value="Provide tooltip info"/>
    <jsp:param name="name" value="tooltips"/>
    <jsp:param name="default" value="true"/>
    <jsp:param name="tooltip" value="If set to True then provides detailed
      information on data through tooltip. Can be useful but if processing
      large amounts of data can slow down the query."/>
   </jsp:include>

   <jsp:include page="params/predictionSource.jsp" />

   <div class="param">
     <label for="predictionType"><fmt:message key="div.ptype" /></label>
     <select id="predictionType" name="predictionType"
     	title="Specifies whether or not to show prediction accuracy for
     	predictions that were affected by a layover. Select 'All' to show
     	data for predictions, 'Affected by layover' to only see data where
     	predictions affected by when a driver is scheduled to leave a layover,
     	or 'Not affected by layover' if you only want data for predictions
     	that were not affected by layovers.">
       <option value=""><fmt:message key="div.pall" /></option>
       <option value="AffectedByWaitStop"><fmt:message key="div.paff" /></option>
       <option value="NotAffectedByWaitStop"><fmt:message key="div.pnaff" /></option>
     </select>
   </div>

    <jsp:include page="params/submitReport.jsp" />
  </form>
</div>
  </jsp:body>
</t:layout>
