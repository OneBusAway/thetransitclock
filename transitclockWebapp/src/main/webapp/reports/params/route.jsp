<%-- For creating a route selector parameter via a jsp include.
     User can select all routes (r param then set to "") or a single
     route (but not an arbitrary multiple of routes).
     Reads in routes via API for the agency specified by the "a" param. --%>

<script>

$.getJSON(apiUrlPrefix + "/command/routes",
 		function(routes) {
	 		var selectorData = [{id: '', text: 'All Routes'}];
	 		for (var i in routes.route) {
	 			var route = routes.route[i];
	 			selectorData.push({id: route.id, text: route.name})
	 		}

 			$("#route").select2({
 				placeholder: "All Routes",
 				data : selectorData});

 			// select2 v4 wraps the original input in #select2-route-container;
 			// re-wire the tooltip there since the generated container loses
 			// the title attribute.
	 		var configuredTitle = $( "#route" ).attr("title");
	 		$( "#select2-route-container" ).tooltip({ content: configuredTitle });
 		 	$("#route").on("change", function(e) { $("#select2-route-container").tooltip("close") });
 	});

</script>

<div id="routesDiv">
  <label for="route" class="block text-sm font-medium text-gray-900 mb-1">Route:</label>
  <input id="route" name="r" style="width: 100%; max-width: 28rem;"
    title="Select which route you want data for. Note: selecting all routes indeed reads in data for all routes which means it could be somewhat slow."/>
</div>
