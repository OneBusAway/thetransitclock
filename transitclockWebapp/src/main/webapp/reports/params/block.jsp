<%-- For creating a block ID selector parameter via a jsp include.
     Reads in IDs via API for the agency specified by the "a" param. --%>

<script>

$.getJSON(apiUrlPrefix + "/command/blockIds",
 		function(blockIds) {
	 		var selectorData = [];
	 		for (var i in blockIds.ids) {
	 			var blockId = blockIds.ids[i];
	 			selectorData.push({id: blockId, text: blockId})
	 		}

 			$("#block").select2({
 				placeholder: "Select Block",
 				data : selectorData})
 			// select2's generated container doesn't carry the original
 			// title attribute, so the jQuery UI tooltip is wired up
 			// manually and re-applied after each selection.
 			.on("select2:select", function(e) {
 				var configuredTitle = $( "#block" ).attr("title");
 				$( "#select2-block-container" ).tooltip({ content: configuredTitle,
 						position: { my: "left+10 center", at: "right center" } });
 			});

	 		var configuredTitle = $( "#block" ).attr("title");
	 		$( "#select2-block-container" ).tooltip({ content: configuredTitle,
	 				position: { my: "left+10 center", at: "right center" } });
 	});

</script>

<div id="blocksDiv">
  <label for="block" class="block text-sm font-medium text-neutral-700 mb-1"><fmt:message key="div.dblock"/>:</label>
  <select id="block" name="b" style="width: 100%; max-width: 22rem;"
    title="Select which block you want data for."></select>
</div>
