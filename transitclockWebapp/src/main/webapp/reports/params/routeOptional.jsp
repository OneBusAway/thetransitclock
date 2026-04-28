<%-- For creating a route selector parameter via a jsp include.
     User can select no routes (r param then set to " ") or a single
     route (but not an arbitrary multiple of routes). For when
     selecting a route is optional.
     Select is created by reading in routes via API for the agency
     specified by the "a" param. --%>

<script>

$.getJSON(apiUrlPrefix + "/command/routes",
 		function(routes) {
	 		var selectorData = [{id: ' ', text: 'Optionally Select Route'}];
	 		for (var i in routes.routes) {
	 			var route = routes.routes[i];
	 			selectorData.push({id: route.shortName, text: route.name})
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
    title="For when you want to optionally display information about a route."></select>
</div>
