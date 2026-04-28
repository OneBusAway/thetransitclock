<%@ page import="org.transitclock.utils.web.WebUtils" %>
<%@ page import="org.transitclock.web.WebConfigParams" %>
<%
pageContext.setAttribute("ajaxDataString", WebUtils.getAjaxDataString(request));
pageContext.setAttribute("mapTileUrl", WebConfigParams.getMapTileUrl());
pageContext.setAttribute("mapTileCopyright", WebConfigParams.getMapTileCopyright());
%>
<t:layout bare="true">
  <jsp:attribute name="title"><fmt:message key="div.avldata" /></jsp:attribute>
  <jsp:attribute name="head">
  <!-- So that get proper sized map on iOS mobile device -->
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />

  <link rel="stylesheet" href="${pageContext.request.contextPath}/maps/css/mapUi.css" />
  <link rel="stylesheet" href="${pageContext.request.contextPath}/map/css/avlMapUi.css" />

  <link rel="stylesheet" href="//cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.3/leaflet.css" />
  <script src="//cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.3/leaflet.js"></script>

  <script src="${pageContext.request.contextPath}/maps/javascript/leafletRotatedMarker.js"></script>
  <script src="${pageContext.request.contextPath}/maps/javascript/mapUiOptions.js"></script>
  <script src="${pageContext.request.contextPath}/map/javascript/animation.js"></script>

  <!-- Load in Select2 files so can create fancy route selector -->
  <link href="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0/css/select2.min.css" rel="stylesheet" />
  <script src="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0/js/select2.min.js"></script>
  </jsp:attribute>
  <jsp:body>
  <div id="map"></div>
  <div id="params">
  	<table id="paramsTable"></table>
  	<jsp:include page="params/vehicle.jsp" />
  	<jsp:include page="params/fromDateNumDaysTime.jsp" />
    <jsp:include page="params/routeSingle.jsp" /> <br>
    <input type="button" id="submit" value='<fmt:message key="div.Submit" />'>
    <a href="#" id="exportData"><fmt:message key="div.Export" /></a>
  </div>
  <div id="playbackContainer">
	  <div id="playback">
        <div>
	  	  <input type="image" src="${pageContext.request.contextPath}/reports/images/playback/media-seek-backward.svg" id="playbackPrev" />
	  	  <input type="image" src="${pageContext.request.contextPath}/reports/images/playback/media-skip-backward.svg" id="playbackRew" />
	  	  <input type="image" src="${pageContext.request.contextPath}/reports/images/playback/media-playback-start.svg" id="playbackPlay" />
	  	  <input type="image" src="${pageContext.request.contextPath}/reports/images/playback/media-skip-forward.svg" id="playbackFF" />
	  	  <input type="image" src="${pageContext.request.contextPath}/reports/images/playback/media-seek-forward.svg" id="playbackNext" />
	    </div>
	    <div><span id="playbackRate">1X</span></div>
	  	<div><span id="playbackTime">00:00:00</span></div>
	  </div>
  </div>

<script>
var request = {${ajaxDataString}},
	contextPath = "${pageContext.request.contextPath}";
window.transitclockMapTileUrl = '${mapTileUrl}';
window.transitclockMapTileCopyright = '${mapTileCopyright}';
</script>
<script src="${pageContext.request.contextPath}/map/javascript/avlMap.js?v=2"></script>
  </jsp:body>
</t:layout>
