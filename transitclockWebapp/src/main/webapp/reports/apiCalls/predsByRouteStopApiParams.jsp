<t:apiCallLayout>
  <jsp:attribute name="title"><fmt:message key="div.SpecifyParameters" /></jsp:attribute>
  <jsp:attribute name="head">
    <!-- Load in Select2 files so can create fancy route selector -->
    <link href="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0/css/select2.min.css" rel="stylesheet" />
    <script src="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0/js/select2.min.js"></script>

    <link href="../params/reportParams.css" rel="stylesheet"/>

    <script>
      function execute() {
        var selectedRouteId = $("#route").val();
        var stopId = $("#stopId").val();
        var numPreds = $("#numPreds").val();
        var format = $('input:radio[name=format]:checked').val();
    	  var url = apiUrlPrefix + "/command/predictions?rs=" + selectedRouteId + "|" + stopId
		  	  + (numPreds!=""?"&numPreds=" + numPreds:"")
    			  + "&format=" + format;

     	  // Actually do the API call
     	  location.href = url;
      }
    </script>
  </jsp:attribute>
  <jsp:body>
<div id="title">
   <fmt:message key="div.spfpbrsa" />
</div>

<div id="mainDiv">
   <%-- Create route selector --%>
   <jsp:include page="../params/routeSingle.jsp" />

   <div class="param">
    <label for="stop"><fmt:message key="div.stopid" /></label>
    <input type="text" id="stopId" size="10" />
   </div>

   <div class="param">
    <label for="numPreds"><fmt:message key="div.np" /></label>
    <input type="text" id="numPreds" size="10" /> <span class="note"><fmt:message key="div.difps" /></span>
   </div>

   <%-- Create json/xml format radio buttons --%>
   <jsp:include page="../params/jsonXmlFormat.jsp" />

   <%-- Create submit button --%>
   <jsp:include page="../params/submitApiCall.jsp" />
</div>
  </jsp:body>
</t:apiCallLayout>
