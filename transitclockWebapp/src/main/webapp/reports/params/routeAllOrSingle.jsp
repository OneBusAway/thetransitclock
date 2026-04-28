<%-- For creating a route selector parameter via a jsp include.
     User can select all routes (r param then set to " ") or a single
     route (but not an arbitrary multiple of routes).
     Select is created by reading in routes via API for the agency
     specified by the "a" param. --%>

<script>

$.getJSON(apiUrlPrefix + "/command/routes",
 		function(routes) {
	        // Generate list of routes for the selector.
	        // For selector2 version 4.0 now can't set id to empty
	        // string because then it returns the text 'All Routes'.
	        // So need to use a blank string that can be determined
	        // to be empty when trimmed.
	 		var selectorData = [{id: ' ', text: '<fmt:message key="div.AllRoutes" />'}];
	 		for (var i in routes.routes) {
	 			var route = routes.routes[i];
	 			var name = route.shortName + " " + route.longName
	 			selectorData.push({id: route.shortName, text: name})
	 		}

 			$("#route").select2({
 				data : selectorData})
 			.on("select2:select", function(e) {
 				var configuredTitle = $( "#route" ).attr("title");
 			 	$( "#select2-route-container" ).tooltip({ content: configuredTitle,
 			 			position: { my: "left+10 center", at: "right center" } });
 			});

	 		var configuredTitle = $( "#route" ).attr("title");
	 		$( "#select2-route-container" ).tooltip({ content: configuredTitle,
	 				position: { my: "left+10 center", at: "right center" } });
 	});

</script>

<div id="routesDiv">
  <label for="route" class="block text-sm font-medium text-gray-900 mb-1"><fmt:message key="div.route"/></label>
  <select id="route" name="r" style="width: 100%; max-width: 28rem;"
    title="Select which route you want data for. Note: selecting all routes indeed reads in data for all routes which means it could be somewhat slow."></select>
</div>
