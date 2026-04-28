<t:layout>
  <jsp:attribute name="title"><fmt:message key="div.SpecifyParameters" /></jsp:attribute>
  <jsp:attribute name="head">
    <!-- Load in Select2 files so can create fancy route selector -->
    <link href="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0/css/select2.min.css" rel="stylesheet" />
    <script src="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0/js/select2.min.js"></script>

    <link href="../params/reportParams.css" rel="stylesheet"/>

    <script>
      function execute() {
        var selectedRouteIds = $("#route").val();
        var routesSpecifier = "";
        for (var i in selectedRouteIds) {
      	  var routeId = selectedRouteIds[i].trim();
      	  if (routeId != "")
      	  	routesSpecifier += "r=" + routeId + "&";
        }
        var format = $('input:radio[name=format]:checked').val();
    	  var url = apiUrlPrefix + "/command/routesDetails?" + routesSpecifier + "format=" + format;

     	  // Actually do the API call
     	  location.href = url;
      }
    </script>
  </jsp:attribute>
  <jsp:body>
<div id="title">
   <fmt:message key="div.spfrda" />
</div>

<div id="mainDiv">
   <%-- Create route selector --%>
   <jsp:include page="../params/routeMultiple.jsp" />

   <%-- Create json/xml format radio buttons --%>
   <jsp:include page="../params/jsonXmlFormat.jsp" />

   <%-- Create submit button --%>
   <jsp:include page="../params/submitApiCall.jsp" />
</div>
  </jsp:body>
</t:layout>
