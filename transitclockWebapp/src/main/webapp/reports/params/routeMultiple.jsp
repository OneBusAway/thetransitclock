<%-- For creating a route selector parameter via a jsp include.
     User can select all routes (r param then set to " ") or any
     number of routes.
     Reads in routes via API for the agency specified by the "a" param. --%>

<script>

$.getJSON(apiUrlPrefix + "/command/routes",
 		function(routes) {
	 		var selectorData = [{id: ' ', text: 'All Routes'}];
	 		for (var i in routes.routes) {
	 			var route = routes.routes[i];
	 			var name = route.shortName + " " + route.longName
	 			selectorData.push({id: route.shortName, text: name})
	 		}

 			$("#route").select2({
 				placeholder: "All Routes",
 				data : selectorData});
 	});

</script>

<div id="routesDiv">
  <label for="route" class="block text-sm font-medium text-neutral-700 mb-1"><fmt:message key="div.rou"/></label>
  <select id="route" name="r" multiple="multiple" style="width: 100%; max-width: 28rem;"
    title="Select which routes you want data for. You can use the Ctrl key along with the mouse to select multiple routes. Note: selecting all routes indeed reads in data for all routes which means it could be somewhat slow."></select>
</div>
