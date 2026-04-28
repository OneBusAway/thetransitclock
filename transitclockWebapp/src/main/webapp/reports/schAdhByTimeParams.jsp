<t:layout>
  <jsp:attribute name="title"><fmt:message key="div.SpecifyParameters" /></jsp:attribute>
  <jsp:attribute name="head">
    <!-- Load in Select2 files so can create fancy route selector -->
    <link href="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0/css/select2.min.css" rel="stylesheet" />
    <script src="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0/js/select2.min.js"></script>

    <link href="params/reportParams.css" rel="stylesheet"/>
  </jsp:attribute>
  <jsp:body>
<div id="title">
   <fmt:message key="div.spsabr" />
</div>

<div id="mainDiv">
<form action="schAdhByTimeChart.jsp" method="POST">
   <%-- For passing agency param to the report --%>
   <input type="hidden" name="a" value="${param.a}">

   <jsp:include page="params/routeSingle.jsp" />

   <jsp:include page="params/fromDateNumDaysTime.jsp" />

   <div class="param">
    <label for="allowableEarly"><fmt:message key="div.aear" /></label>
    <input id="allowableEarly" name="allowableEarly"
    	title="How early a vehicle can arrive compared to the prediction
    	and still be acceptable. Must be a negative number to indicate
    	early."
    	size="1"
        value="1.0" /> <span class="note"><fmt:message key="div.minutes" /></span>
  </div>

   <div class="param">
    <label for="allowableLate"><fmt:message key="div.alat" /></label>
    <input id="allowableLate" name="allowableLate"
    	title="How late a vehicle can arrive compared to the prediction
    	and still be acceptable. Must be a positive number to indicate
    	late."
    	size="1"
        value="4.0"/> <span class="note"><fmt:message key="div.minutes" /></span>
  </div>

    <jsp:include page="params/submitReport.jsp" />
  </form>
</div>
  </jsp:body>
</t:layout>
