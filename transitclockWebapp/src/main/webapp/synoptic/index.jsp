<%@ page pageEncoding="UTF-8" %>
<%
pageContext.setAttribute("ctx", request.getContextPath());
%>
<!--
 Query String parameters:
   a=AGENCY (required)
   r=ROUTE (optional, if not specified then a route selector is created)
   s=STOP_ID (optional, for specifying which stop interested in)
   tripPattern=TRIP_PATTERN (optional, for specifying which stop interested in).
   verbose=true (optional, for getting additional info in vehicle popup window)
   showUnassignedVehicles=true (optional, for showing unassigned vehicles)
   updateRate=MSEC (optional, for specifying update rate for vehicle locations.
-->
<t:layout bare="true">
  <jsp:attribute name="title">TheTransitClock Synoptic</jsp:attribute>
  <jsp:attribute name="head">
  <!-- So that get proper sized map on iOS mobile device -->
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
 <link rel="stylesheet" href="//cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.3/leaflet.css" />
  <script src="//cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.3/leaflet.js"></script>
  <script src="${pageContext.request.contextPath}/maps/javascript/leafletRotatedMarker.js"></script>
  <script src="${pageContext.request.contextPath}/maps/javascript/mapUiOptions.js"></script>

  <script src="${pageContext.request.contextPath}/javascript/jquery-dateFormat.min.js"></script>
<script src="${pageContext.request.contextPath}/synoptic/javascript/synoptic.js"></script>

  <link rel="stylesheet" href="${pageContext.request.contextPath}/maps/css/mapUi.css" />
  <link rel="stylesheet" href="${pageContext.request.contextPath}/synoptic/css/synoptic.css">

  <!-- Load in Select2 files so can create fancy selectors -->
  <link href="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0/css/select2.min.css" rel="stylesheet" />
  <script src="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0/js/select2.min.js"></script>
  </jsp:attribute>
  <jsp:body>
  <t:mapPage>
    <jsp:attribute name="title"><fmt:message key="div.synoptic"/></jsp:attribute>
    <jsp:attribute name="actions">
      <div id="routesDiv" class="ml-auto">
        <select id="routes" style="width:320px"></select>
      </div>
    </jsp:attribute>
    <jsp:body>
  <div id="synoptic"></div>
  <div id="synopticScrollbar" aria-hidden="true">
    <div id="synopticScrollThumb"></div>
  </div>
	<script type="text/javascript">

	var agencyTimezoneOffset;
	function dateFormat(time) {
		var localTimezoneOffset = (new Date()).getTimezoneOffset();
		var timezoneDiffMinutes = localTimezoneOffset - agencyTimezoneOffset;

		var offsetDate = new Date(parseInt(time)*1000 + timezoneDiffMinutes*60*1000);
		// Use jquery-dateFormat javascript library
		return $.format.date(offsetDate, 'HH:mm:ss');
	}


	$.getJSON(apiUrlPrefix + "/command/agencyGroup",
			function(agencies) {
				// If agency not defined, such as when just testing AVL feed,
				// then set map to United States
				if (agencies.agency.length == 0) {
					return;
				}

		        agencyTimezoneOffset = agencies.agency[0].timezoneOffsetMinutes;

		        // Fit the map initially to the agency, but only if route not
		        // specified in query string. If route specified in query string
		        // then the map will be fit to that route once it is loaded in.

		});

	var shortNameParam;
	function getShortNameParam() {
		return routeQueryStrParam;
	}

	function setShortNameParam(param) {
		shortNameParam = param;
	}


	var routeQueryStrParam;

	function getRouteQueryStrParam() {
		return routeQueryStrParam;
	}

	function setRouteQueryStrParam(param) {
		routeQueryStrParam = param;
	}

	var shapeMapLength;

	function getShapeLength(tripPatternId) {
		return shapeMapLength[tripPatternId];
	}

	function setShapeMapLenght(param) {
		shapeMapLength = param;
	}
	var imgVehicleOnSechedule=new Image();
	var imgVehicleNotOnSechedule=new Image();
	this.imgVehicleOnSechedule.src="mybus.png";
	this.imgVehicleOnSechedule.width=30;
	this.imgVehicleOnSechedule.height=30;
	this.imgVehicleNotOnSechedule.src="mybusRed.png";
	this.imgVehicleNotOnSechedule.width=30;
	this.imgVehicleNotOnSechedule.height=30;

	/**
	* Returns different image according to some conditions
	*
	**/
	function vehicleAlternColorCallBack(vehicle)
	{
		if(Math.abs(vehicle.schAdh)<120000)//ON SCHEDULE
			return imgVehicleOnSechedule;
		else
			return imgVehicleNotOnSechedule;
	}

function testFunc()
{}


function predictionCallback(preds, status) {
	// If predictions popup was closed then don't do anything


	// There will be predictions for just a single route/stop
	var routeStopPreds = preds.predictions[0];

	// Set timeout to update predictions again in few seconds
	//predictionsTimeout = setTimeout(getPredictionsJson, 20000, routeStopPreds.routeShortName, routeStopPreds.stopId);

	// Add route and stop info
	var stopName = routeStopPreds.stopName;
	if (routeStopPreds.stopCode)
		stopName += " (" + routeStopPreds.stopCode + ")";
	var content = '<div class="synopticPredictions">'
		+ '<div class="synopticPredictions__title">' + stopName + '</div>';

	for (var i in routeStopPreds.dest) {
		var dest = routeStopPreds.dest[i];
		if (dest.headsign)
			content += '<div class="synopticPredictions__headsign">' + dest.headsign + '</div>';

		if (dest.pred.length > 0) {
			content += '<table class="synopticPredictions__table">'
				+ '<thead><tr><th>Trip</th><th>Vehicle</th><th class="num">Min</th></tr></thead>'
				+ '<tbody>';
			for (var j in dest.pred) {
				var pred = dest.pred[j];
				var ident = synoptic.getVehicleIdentifier(pred.vehicle);
				if (ident == null)
					ident = pred.vehicle;
				content += '<tr><td>' + pred.trip + '</td>'
					+ '<td>' + ident + '</td>'
					+ '<td class="num">' + pred.min + '</td></tr>';
			}

			content += '</tbody></table>';

		} else {
			content += '<div class="synopticPredictions__empty">No predictions</div>';
		}
	}
	content += '</div>';

	synoptic.setPredictionsContent(content);
}

function getPredictionsJson( stopId) {

	// JSON request of predicton data
	var url = apiUrlPrefix + "/command/predictions?rs=" + getShortNameParam()
			+ encodeURIComponent("|") + stopId;
	$.getJSON(url, predictionCallback);
}

function vehicleUpdate(vehicleDetail, status)
{
	var buses=[];
	for(var i in  vehicleDetail.vehicles)
	{
		console.log("i: "+i);
		vehicle=vehicleDetail.vehicles[i];
		console.log(vehicle);

		//if(vehicle.direction==undefined)
		//	vehicle.direction=="0";

		var directionVehicle=(vehicle.direction=="0" || vehicle.direction==undefined)?0:1;
		var _identifier=(vehicle.licensePlate==undefined)?vehicle.id:vehicle.licensePlate;
		var gpsTimeStr = dateFormat(vehicle.loc.time);
		buses.push({id:vehicle.id, projection:vehicle.distanceAlongTrip/getShapeLength(vehicle.tripPattern),identifier:_identifier,direction:directionVehicle,gpsTimeStr:gpsTimeStr,nextStopName:vehicle.nextStopName,schAdhStr:vehicle.schAdhStr,trip:vehicle.trip,schAdh:vehicle.schAdh,headway:vehicle.headway,isScheduledService:vehicle.isScheduledService,freqStartTime:vehicle.freqStartTime});

	}
	synoptic.setBuses(buses);
	synoptic.steps=100;
	synoptic.counter=0;
	//window.requestAnimationFrame(test.animateBus);
	synoptic.animateBus(20);
	console.log(vehicleDetail);
}
function startTimerUpdate()
{
	console.log("startTimerUpdate");
	var url = apiUrlPrefix + "/command/vehiclesDetails?r=" + getRouteQueryStrParam();
		$.getJSON(url, vehicleUpdate);
	setTimeout(startTimerUpdate, 30000);
}

function routeConfigCallback(routeDetail, status)
{
	console.log(routeDetail);
	console.log(status);
	//FOR EACH DIRECTION
	var stops=[];
	var buses=[];
	var showReturn=false;
	var shapeMap = {};
	setShortNameParam(routeDetail.routes[0].shortName);
	for(var i  in routeDetail.routes[0].direction)
	{
		//console.log(routeDetail.routes[0].direction[i]);
		console.log("Direction "+routeDetail.routes[0].direction[i].id);
		console.log(routeDetail.routes[0].direction[i]);
		if(routeDetail.routes[0].direction[i]==undefined)
			routeDetail.routes[0].direction[i]="0";
		var distanceOverPath=0.0;

		var routeLenght=-1;
		console.log(routeDetail.routes[0].shape );
		for(	var k in routeDetail.routes[0].shape)
		{
			var shape=routeDetail.routes[0].shape[k];
			console.log("K "+k );
			if(routeDetail.routes[0].direction[i].id!=shape.directionId)
				continue;

			shapeMap[shape.tripPattern]=shape.length;
			if(shape.length>routeLenght)
				routeLenght=shape.length;
		}

		for(var j in routeDetail.routes[0].direction[i].stop)
		{

			var stop=routeDetail.routes[0].direction[i].stop[j];
			distanceOverPath+=stop.pathLength;
			var projectionStop=distanceOverPath/routeLenght;
			if(stop.direction==undefined)
				stop.direction="0";
			var directionStop=(stop.direction==routeDetail.routes[0].direction[i].id=="0")?0:1;
			stops.push({id: stop.id, identifier: stop.name,projection:projectionStop,direction:directionStop,distance:distanceOverPath});
		}
		if(i>0)
			showReturn=true;

		console.log(stops);

	}
	setShapeMapLenght(shapeMap);
	synoptic=null;

	var canvas = document.getElementById("synoptic");
	while (canvas.firstChild) {
		canvas.removeChild(canvas.firstChild);
	}
	var params={container:canvas,
			onVehiClick:testFunc,
			infoStop:function(data) {
				return '<table class="synopticInfo">'
					+ '<caption>'+data.identifier+'</caption>'
					+ '<tr><th scope="row">Distance</th><td>'+parseFloat(data.distance).toFixed(2)+' m</td></tr>'
					+ '</table>';
			},
			infoVehicle:function(data) {
				var startTimeRow = (data.isScheduledService==false)
					? '<tr><th scope="row">Start time</th><td>'+dateFormat(data.freqStartTime/1000)+'</td></tr>'
					: '';
				var headwayValue = (data.headway==-1)
					? '—'
					: (data.headway/60000).toFixed(2)+' min';
				return '<table class="synopticInfo">'
					+ '<caption>Bus '+data.identifier+'</caption>'
					+ '<tr><th scope="row">GPS time</th><td>'+data.gpsTimeStr+'</td></tr>'
					+ '<tr><th scope="row">Next stop</th><td>'+data.nextStopName+'</td></tr>'
					+ '<tr><th scope="row">Sched adh</th><td>'+data.schAdhStr+'</td></tr>'
					+ '<tr><th scope="row">Trip</th><td>'+data.trip+'</td></tr>'
					+ startTimeRow
					+ '<tr><th scope="row">Headway</th><td>'+headwayValue+'</td></tr>'
					+ '</table>';
			},
			patternType:routeDetail.routes[0].shape[0].patternType,
			drawReturnUpside:false,
			showReturn:showReturn,
			predictionFunction:getPredictionsJson,
			vehicleAlternColorCallBack:vehicleAlternColorCallBack
	}

	synoptic=new Sinoptico(params);
	synoptic.setStops(stops);
	synoptic.setBuses(buses);
	synoptic.init();
	synoptic.paintBus();
	console.log(routeDetail.routes[0].id);
	startTimerUpdate();
}

	console.log("INIT "+apiUrlPrefix);
	$.getJSON(apiUrlPrefix + "/command/routes",
	 		function(routes) {
				console.log("JSON "+routes);
				console.log(routes);
		        // Generate list of routes for the selector
		 		//var selectorData = [];
		 		var selectorData = [{id: '', text: 'Select Route'}];
		 		for (var i in routes.routes) {
		 			console.log("VAR i"+i);
		 			var route = routes.routes[i];
		 			selectorData.push({id: route.id, text: route.name})
		 		}
		 		console.log(selectorData);
		 		// Configure the selector to be a select2 one that has
		 		// search capability
		 		$("#routes").select2({
 				placeholder: "Select Route",
 				data : selectorData})
 				.on("select2:select", function(e) {

 					// First remove all old vehicles so that they don't
 					// get moved around when zooming to new route
 					//removeAllVehicles();
 					// Configure map for new route
 					console.log("SELECTED "+e.params.data.id);
 					var selectedRouteId = e.params.data.id;
 					var url = apiUrlPrefix + "/command/routesDetails?r=" + selectedRouteId;
 					$.getJSON(url, routeConfigCallback);
 					setRouteQueryStrParam(selectedRouteId)
 					// Reset the polling rate back down to minimum value since selecting new route
 					//avlPollingRate = MIN_AVL_POLLING_RATE;
 					//if (avlTimer)
 					//	clearTimeout(avlTimer);

 					// Read in vehicle locations now
 					//setRouteQueryStrParam("r=" + selectedRouteId);
 					//updateVehiclesUsingApiData();

				});



	 	});

	</script>
    </jsp:body>
  </t:mapPage>
  </jsp:body>
</t:layout>
