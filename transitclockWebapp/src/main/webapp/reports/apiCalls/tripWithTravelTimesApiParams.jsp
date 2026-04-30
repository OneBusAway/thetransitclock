<t:apiCallLayout>
  <jsp:attribute name="title"><fmt:message key="div.SpecifyParameters" /></jsp:attribute>
  <jsp:attribute name="head">
    <link href="../params/reportParams.css" rel="stylesheet"/>

    <script>
      function execute() {
        var tripId = $("#tripId").val();
        var format = $('input:radio[name=format]:checked').val();

    	  var url = apiUrlPrefix + "/command/tripWithTravelTimes"
    	          + "?tripId=" + tripId
    			  + "&format=" + format;

     	  // Actually do the API call
     	  location.href = url;
      }
    </script>
  </jsp:attribute>
  <jsp:body>
<div id="title">
   <fmt:message key="div.spfta" />
</div>

<div id="mainDiv">
  <div class="param">
    <label for="trip"><fmt:message key="div.dtrip" />:</label>
    <input type="text" id="tripId" size="35" />
  </div>

   <%-- Create json/xml format radio buttons --%>
   <jsp:include page="../params/jsonXmlFormat.jsp" />

   <%-- Create submit button --%>
   <jsp:include page="../params/submitApiCall.jsp" />
</div>
  </jsp:body>
</t:apiCallLayout>
