<%-- For creating a vehicle selector parameter via a jsp include.
     User can select all vehicles (v set to "") OR a single vehicle.
     Reads in vehicles via API for the agency specified by the "a" param. --%>

<script>

$.getJSON(apiUrlPrefix + "/command/vehicleIds",
 		function(vehicles) {
	 		var selectorData = [{id: '', text: 'All Vehicles'}];
	 		for (var i in vehicles.ids) {
	 			var id = vehicles.ids[i];
	 			selectorData.push({id: id, text: id})
	 		}

 			$("#vehicle").select2({
 				data : selectorData
 			});

	 		$("#vehicle option:first").attr("value", "");

 			// select2 v4's generated container doesn't carry the original
 			// title attribute, so wire jQuery UI tooltips onto the
 			// generated #select2-vehicle-container and dismiss on change.
	 		var configuredTitle = $( "#vehicle" ).attr("title");
	 		$( "#select2-vehicle-container" ).tooltip({ content: configuredTitle });
 		 	$("#vehicle").on("change", function(e) { $("#select2-vehicle-container").tooltip("close") });
 	});

</script>

<div id="vehicleDiv">
  <label for="vehicle" class="block text-sm font-medium text-gray-900 mb-1"><fmt:message key="div.Vehicle"/>:</label>
  <select id="vehicle" name="v" style="width: 100%; max-width: 16rem;"
    title="Select which vehicle you want data for."><!-- prevent jspx min --></select>
</div>
