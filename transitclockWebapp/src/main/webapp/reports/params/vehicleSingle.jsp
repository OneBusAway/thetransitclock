<%-- For creating a vehicle selector parameter via a jsp include.
     User can select a single vehicle (not all vehicles).
     Reads in vehicles via API for the agency specified by the "a" param. --%>

<script>

$.getJSON(apiUrlPrefix + "/command/vehicleIds",
 		function(vehicles) {
	 		var selectorData = [];
	 		for (var i in vehicles.ids) {
	 			var id = vehicles.ids[i];
	 			selectorData.push({id: id, text: id})
	 		}

 			$("#vehicle").select2({
 				placeholder: "Select Vehicle",
 				data : selectorData});

 			// See vehicle.jsp for why the tooltip is wired onto the
 			// select2-generated container rather than the hidden <input>.
	 		var configuredTitle = $( "#vehicle" ).attr("title");
	 		$( "#select2-vehicle-container" ).tooltip({ content: configuredTitle });
 		 	$("#vehicle").on("change", function(e) { $("#select2-vehicle-container").tooltip("close") });
 	});

</script>

<div id="vehicleDiv">
  <label for="vehicle" class="block text-sm font-medium text-gray-900 mb-1"><fmt:message key="div.Vehicle"/>:</label>
  <input id="vehicle" name="v" style="width: 100%; max-width: 16rem;"
    title="Select which vehicle you want data for."/>
</div>
