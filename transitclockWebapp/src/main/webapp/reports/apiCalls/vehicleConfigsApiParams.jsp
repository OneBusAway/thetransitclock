<t:layout>
  <jsp:attribute name="title"><fmt:message key="div.SpecifyParameters" /></jsp:attribute>
  <jsp:attribute name="head">
    <link href="../params/reportParams.css" rel="stylesheet"/>

    <script>
      function execute() {
        var format = $('input:radio[name=format]:checked').val();
    	  var url = apiUrlPrefix + "/command/vehicleConfigs?format=" + format;

     	  // Actually do the API call
     	  location.href = url;
      }
    </script>
  </jsp:attribute>
  <jsp:body>
<div id="title">
   <fmt:message key="div.spfvca" />
</div>

<div id="mainDiv">
   <%-- Create json/xml format radio buttons --%>
   <jsp:include page="../params/jsonXmlFormat.jsp" />

   <%-- Create submit button --%>
   <jsp:include page="../params/submitApiCall.jsp" />

</div>
  </jsp:body>
</t:layout>
