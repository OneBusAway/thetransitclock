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
 				data : selectorData});
 	});

</script>

<div id="blocksDiv">
  <label for="block" class="block text-sm font-medium text-neutral-700 mb-1"><fmt:message key="div.dblock"/>:</label>
  <select id="block" name="b" style="width: 100%; max-width: 22rem;"
    title="Select which block you want data for."></select>
</div>
