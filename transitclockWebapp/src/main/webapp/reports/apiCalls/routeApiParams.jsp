<t:apiCallLayout>
  <jsp:attribute name="title"><fmt:message key="div.SpecifyParameters" /></jsp:attribute>
  <jsp:attribute name="head">
    <!-- Load in Select2 files so can create fancy route selector -->
    <link href="../../select2/select2.css" rel="stylesheet"/>
    <script src="../../select2/select2.min.js"></script>

    <link href="../params/reportParams.css" rel="stylesheet"/>

    <script>
      function execute() {
        var selectedRouteId = $("#route").val();
        var format = $('input:radio[name=format]:checked').val();
    	  var url = apiUrlPrefix + "/command/routes?format=" + format;

     	  // Actually do the API call
     	  location.href = url;
      }
    </script>
  </jsp:attribute>
  <jsp:body>
<div id="title">
   <fmt:message key="div.spfra" />
</div>

<div id="mainDiv">
   <%-- Create json/xml format radio buttons --%>
   <jsp:include page="../params/jsonXmlFormat.jsp" />

   <%-- Create submit button --%>
   <jsp:include page="../params/submitApiCall.jsp" />
</div>
  </jsp:body>
</t:apiCallLayout>
