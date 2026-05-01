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
    	  var url = apiUrlPrefix + "/command/gtfs-rt/tripUpdates?format=" + format;

     	  // Actually do the API call
     	  location.href = url;
      }
    </script>
  </jsp:attribute>
  <jsp:body>
<div id="title">
   <fmt:message key="div.spfgrtua" />
</div>

<div id="mainDiv">
   <div id="radioButtonsDiv">
     <input type="radio" name="format" value="binary" checked><fmt:message key="div.binary" />
     <input type="radio" name="format" value="human"><fmt:message key="div.hr" />
   </div>

   <%-- Create submit button --%>
   <jsp:include page="../params/submitApiCall.jsp" />

</div>
  </jsp:body>
</t:apiCallLayout>
