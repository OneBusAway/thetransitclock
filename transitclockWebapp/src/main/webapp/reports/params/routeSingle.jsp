<%-- For creating a route selector parameter via a jsp include.
     User can select a single route (not all routes).
     Reads in routes via API for the agency specified by the "a" param. --%>

<script>

$.getJSON(apiUrlPrefix + "/command/routes",
 		function(routes) {
	        // Put in default value of Select Route but need to use
	        // an id of ' ' instead of '' since otherwise select2
	        // version 4.0.0 uses the text name as the id, which is wrong!
	 		var selectorData = [];
	 		for (var i in routes.routes) {
	 			var route = routes.routes[i];
	 			var name = route.shortName + " " + route.longName
	 			selectorData.push({id: route.shortName, text: name})
	 		}

 			$("#route").select2({
 				placeholder: "Select Route",
 				data: selectorData
 			});
 	});

</script>

<div id="routesDiv">
  <label for="route" class="block text-sm font-medium text-neutral-700 mb-1"><fmt:message key="div.route"/></label>
  <select id="route" name="r" style="width: 100%; max-width: 28rem;"
    title="Select which route you want data for."></select>
</div>
